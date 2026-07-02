package com.multiprofit.mcp;

import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.model.DimensionMaster;
import com.multiprofit.service.DashboardService;
import com.multiprofit.service.DimensionService;
import com.multiprofit.util.IndicatorFactSql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 业财数据MCP Server
 * 统一暴露所有业务数据查询能力，AI无需感知底层表结构
 */
@Component
public class BizDataMcpServer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private DashboardService dashboardService;

    /**
     * MCP工具：查询盈利指标
     * 按维度组合查询盈利指标（收入、成本、利润等）
     */
    @McpTool(name = "query_profit_metrics",
            description = "按维度查询盈利指标。dimensions为维度类型列表(仅支持 ORG/BIZ_LINE/DEPT/PRODUCT/CHANNEL/MANAGER/CUSTOMER 之一,或[]空数组表示全行汇总;不要传\"全行\"等中文);metrics为指标名,仅支持: net_profit(净利润)/loan_profit(贷款利润)/deposit_profit(存款利润)/loan_interest(贷款利息收入)/ftp_income(FTP收入)/interest_expense(利息支出)/ftp_cost(FTP成本)/risk_cost(风险成本)/op_cost(运营成本);period为YYYY-MM。注:本工具仅查利润/收入/成本类指标,不支持余额/规模/笔数类查询(如贷款余额/存款余额/业务量)")
    public Map<String, Object> queryProfitMetrics(
            List<String> dimensions,
            List<String> metrics,
            String period,
            Map<String, String> filters) {

        // 指标名 → dw_indicator_fact indicator_code 映射(EAV长表)
        Map<String, String> metricCode = new HashMap<>();
        metricCode.put("net_profit", "TOTAL_MONTHLY_PROFIT");
        metricCode.put("loan_profit", "LOAN_MONTHLY_PROFIT");
        metricCode.put("deposit_profit", "DEPOSIT_MONTHLY_PROFIT");
        metricCode.put("loan_interest", "LOAN_MONTHLY_INTEREST");
        metricCode.put("ftp_income", "FTP_MONTHLY_INCOME");
        metricCode.put("interest_expense", "INTEREST_MONTHLY_EXPENSE");
        metricCode.put("ftp_cost", "LOAN_FTP_COST");
        metricCode.put("risk_cost", "LOAN_RISK_COST");
        metricCode.put("op_cost", "LOAN_OP_COST");

        String caliber = (filters != null && filters.get("caliber_type") != null)
                ? filters.get("caliber_type") : "ASSESS";

        // 规范化period:AI可能传"本月"/null等非YYYY-MM值,此时取dw_indicator_fact最新MONTH期间
        if (period == null || !period.matches("\\d{4}-\\d{2}")) {
            period = jdbcTemplate.queryForObject(
                    "SELECT MAX(period) FROM dw_indicator_fact WHERE period_type='MONTH'", String.class);
        } else {
            // 合法YYYY-MM格式但该期间无数据时,回落到MAX(period)(防AI传未来月份)
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dw_indicator_fact WHERE period=? AND period_type='MONTH'", Integer.class, period);
            if (cnt == null || cnt == 0) {
                period = jdbcTemplate.queryForObject(
                        "SELECT MAX(period) FROM dw_indicator_fact WHERE period_type='MONTH'", String.class);
            }
        }

        if (metrics == null || metrics.isEmpty()) {
            metrics = List.of("net_profit");
        }

        // 指标校验:不在白名单的指标返回明确错误(防AI自造指标名如BIZ_AMOUNT静默返回0)
        for (String m : metrics) {
            if (!metricCode.containsKey(m.toLowerCase())) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "不支持的指标: " + m + "。仅支持: " + metricCode.keySet() +
                        "。本工具不支持余额/规模/笔数类查询。");
                return err;
            }
        }

        // 维度校验:非白名单维度值(如AI误传"全行")当作空维度处理
        boolean hasValidDim = false;
        String dimType = null;
        if (dimensions != null && !dimensions.isEmpty()) {
            String first = dimensions.get(0).toUpperCase();
            if (IndicatorFactSql.dimTable(first) != null) {
                hasValidDim = true;
                dimType = first;
            }
        }

        List<Map<String, Object>> data;

        if (!hasValidDim) {
            // 全行汇总:逐指标 SUM(calc_value) WHERE dim_type='TOTAL'(EAV聚合,无GROUP BY)
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dim_name", "全行");
            for (String m : metrics) {
                String code = metricCode.get(m.toLowerCase());
                String sql = "SELECT COALESCE(SUM(calc_value),0) AS v FROM dw_indicator_fact " +
                        "WHERE period=? AND period_type='MONTH' AND dim_type='TOTAL' AND caliber_type=? AND indicator_code=?";
                Object v = jdbcTemplate.queryForMap(sql, period, caliber, code).get("v");
                row.put(m.toLowerCase(), v);
            }
            data = new ArrayList<>();
            data.add(row);
        } else {
            // 按维度:用 IndicatorFactSql.pivot 透视该维度(自动level=3叶子过滤+ASSESS口径)
            String pivotSql = IndicatorFactSql.pivot(dimType, period);
            // pivot返回宽表含 dim_name/net_profit/loan_profit/deposit_profit/revenue/op_cost等列
            String sql = "SELECT dim_name, net_profit, loan_profit, deposit_profit, revenue, op_cost " +
                    "FROM (" + pivotSql + ") t ORDER BY net_profit DESC";
            data = jdbcTemplate.queryForList(sql);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("dimensions", dimensions);
        result.put("metrics", metrics);
        result.put("period", period);

        return result;
    }

    /**
     * MCP工具：维度下钻
     * 返回子维度各成员的盈利指标排名(net_profit/revenue/op_cost)。
     * 注:dw_indicator_fact 各 dim_type 独立预计算(ORG行不带dept_name),不支持"在父维度值范围内下钻",
     * 故退化为返回子维度全部成员;parentValue 仅作语义记录保留。
     */
    @McpTool(name = "drill_down_detail", description = "维度下钻，返回子维度各成员盈利排名(机构/部门/产品/渠道/客户经理等)。parentDim=父维度类型,parentValue=父维度值(语义保留),childDim=子维度类型,period=YYYY-MM")
    public Map<String, Object> drillDown(
            String parentDim,
            String parentValue,
            String childDim,
            String period) {

        // 子维度白名单校验(防注入:childDim 用于 dimTable 映射,不直接拼列名)
        String dimType = (childDim == null) ? "" : childDim.toUpperCase();
        if (IndicatorFactSql.dimTable(dimType) == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "不支持的维度类型: " + childDim + "，支持: ORG/BIZ_LINE/DEPT/PRODUCT/CHANNEL/MANAGER/CUSTOMER");
            return err;
        }

        // 规范化period:AI可能传"本月"/null,非YYYY-MM取最新MONTH期间
        if (period == null || !period.matches("\\d{4}-\\d{2}")) {
            period = jdbcTemplate.queryForObject(
                    "SELECT MAX(period) FROM dw_indicator_fact WHERE period_type='MONTH'", String.class);
        }

        // 用 IndicatorFactSql.pivot 透视子维度(自动level=3叶子+ASSESS口径),取各成员盈利排名
        String sql = "SELECT dim_name, net_profit, revenue, op_cost, loan_profit, deposit_profit " +
                     "FROM (" + IndicatorFactSql.pivot(dimType, period) + ") t " +
                     "ORDER BY net_profit DESC";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("parentDim", parentDim);
        result.put("parentValue", parentValue);
        result.put("childDim", dimType);
        result.put("period", period);
        result.put("note", "EAV各dim_type独立预计算,返回子维度全部成员排名(非父维度值范围内下钻)");

        return result;
    }

    /**
     * MCP工具：同比环比分析
     * 自动计算同比(YOY)/环比(MOM)变化率。
     * 无dimValue=全行汇总(用TOTAL维度);有dimValue=单维度成员。
     */
    @McpTool(name = "query_period_compare", description = "同比环比分析。dimType=维度类型,dimValue=维度值(空则全行),currentPeriod=YYYY-MM,compareType=YOY(同比)/MOM(环比)")
    public Map<String, Object> queryPeriodCompare(
            String dimType,
            String dimValue,
            String currentPeriod,
            String compareType) {

        // 规范化period:AI可能传"本月"/null,非YYYY-MM取最新MONTH期间
        if (currentPeriod == null || !currentPeriod.matches("\\d{4}-\\d{2}")) {
            currentPeriod = jdbcTemplate.queryForObject(
                    "SELECT MAX(period) FROM dw_indicator_fact WHERE period_type='MONTH'", String.class);
        }

        // 计算基期(同比=去年同月,环比=上月)
        String basePeriod = calculateBasePeriod(currentPeriod, compareType);

        // 取当期/基期汇总:无dimValue走全行TOTAL,有dimValue走单维度成员
        Map<String, Object> currentData = queryCompareRow(dimType, dimValue, currentPeriod);
        Map<String, Object> baseData = queryCompareRow(dimType, dimValue, basePeriod);

        // EAV按indicator_code聚合,基期若该期间不存在则返回0行需兜底
        if (currentData == null) currentData = new HashMap<>();
        if (baseData == null) baseData = new HashMap<>();

        Map<String, Object> result = new HashMap<>();
        result.put("current", currentData);
        result.put("base", baseData);
        result.put("currentPeriod", currentPeriod);
        result.put("basePeriod", basePeriod);
        result.put("compareType", compareType);
        if (dimValue != null && !dimValue.isEmpty()) {
            result.put("dimType", dimType);
            result.put("dimValue", dimValue);
        }

        // 计算盈利核心指标变化率(对齐pivot输出列名)
        for (String metric : Arrays.asList("revenue", "op_cost", "net_profit")) {
            double cur = toDouble(currentData.get(metric));
            double bse = toDouble(baseData.get(metric));
            if (bse != 0) {
                double changeRate = (cur - bse) / bse * 100;
                result.put(metric + "ChangeRate", Math.round(changeRate * 100.0) / 100.0);
            }
        }

        return result;
    }

    /**
     * 同比环比的当期/基期取数:无dimValue=全行TOTAL汇总,有dimValue=单维度成员
     */
    private Map<String, Object> queryCompareRow(String dimType, String dimValue, String period) {
        if (dimValue == null || dimValue.isEmpty()) {
            // 全行:用 pivotTotal 取 revenue/op_cost/net_profit
            String sql = "SELECT revenue, op_cost, net_profit FROM (" + IndicatorFactSql.pivotTotal(period) + ") t";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
            return list.isEmpty() ? null : list.get(0);
        } else {
            // 单成员:用 pivot 该维度,过滤 dim_name
            String dt = (dimType == null) ? "" : dimType.toUpperCase();
            if (IndicatorFactSql.dimTable(dt) == null) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "不支持的维度类型: " + dimType);
                return err;
            }
            String sql = "SELECT dim_name, revenue, op_cost, net_profit, loan_profit, deposit_profit " +
                         "FROM (" + IndicatorFactSql.pivot(dt, period) + ") t WHERE dim_name=?";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, dimValue);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /**
     * MCP工具：查询主数据
     * 查询主数据（机构、部门、产品、渠道、客户经理）
     */
    @McpTool(name = "query_master_data", description = "查询主数据（机构、部门、产品、渠道、客户经理）")
    public List<Map<String, Object>> queryMasterData(
            String dimType,
            String parentCode) {

        String sql = "SELECT * FROM dimension_master WHERE dim_type = ?";
        List<Object> params = new ArrayList<>();
        params.add(dimType);

        if (parentCode != null && !parentCode.isEmpty()) {
            sql += " AND parent_code = ?";
            params.add(parentCode);
        }

        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    /**
     * MCP工具：查询业务台账
     * 查询业务台账明细数据
     */
    @McpTool(name = "query_indicator_fact", description = "查询业务台账明细数据")
    public Map<String, Object> queryIndicatorFact(
            String period,
            Map<String, String> dimensionFilters,
            int page,
            int size) {

        StringBuilder sql = new StringBuilder("SELECT * FROM dw_indicator_fact WHERE period = ?");
        List<Object> params = new ArrayList<>();
        params.add(period);

        // 添加维度过滤条件
        if (dimensionFilters != null) {
            for (Map.Entry<String, String> entry : dimensionFilters.entrySet()) {
                sql.append(" AND ").append(entry.getKey().toLowerCase()).append(" = ?");
                params.add(entry.getValue());
            }
        }

        // 计算总数
        String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ") t";
        int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

        // 分页查询
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("period", period);

        return result;
    }

    /**
     * 计算基期
     */
    private String calculateBasePeriod(String currentPeriod, String compareType) {
        // currentPeriod格式: YYYY-MM
        String[] parts = currentPeriod.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        if ("YOY".equalsIgnoreCase(compareType)) {
            // 同比：去年同月
            return (year - 1) + "-" + String.format("%02d", month);
        } else {
            // 环比：上月
            if (month == 1) {
                return (year - 1) + "-12";
            } else {
                return year + "-" + String.format("%02d", month - 1);
            }
        }
    }

    /**
     * 转换为double
     */
    private double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
