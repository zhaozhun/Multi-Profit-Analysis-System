package com.multiprofit.mcp;

import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.model.DimensionMaster;
import com.multiprofit.service.DashboardService;
import com.multiprofit.service.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
    @McpTool(name = "query_profit_metrics", description = "按维度组合查询盈利指标（收入、成本、利润等）")
    public Map<String, Object> queryProfitMetrics(
            List<String> dimensions,
            List<String> metrics,
            String period,
            Map<String, String> filters) {

        StringBuilder sql = new StringBuilder("SELECT ");

        // 添加维度字段
        for (int i = 0; i < dimensions.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(dimensions.get(i).toLowerCase());
        }

        // 添加指标字段
        for (String metric : metrics) {
            sql.append(", SUM(").append(metric.toLowerCase()).append(") as ").append(metric.toLowerCase());
        }

        sql.append(" FROM dw_indicator_fact WHERE period = ?");

        List<Object> params = new ArrayList<>();
        params.add(period);

        // 添加过滤条件
        if (filters != null) {
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                sql.append(" AND ").append(entry.getKey().toLowerCase()).append(" = ?");
                params.add(entry.getValue());
            }
        }

        sql.append(" GROUP BY ");
        for (int i = 0; i < dimensions.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(dimensions.get(i).toLowerCase());
        }

        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("dimensions", dimensions);
        result.put("metrics", metrics);
        result.put("period", period);

        return result;
    }

    /**
     * MCP工具：维度下钻
     * 从机构→部门→产品逐层穿透
     */
    @McpTool(name = "drill_down_detail", description = "维度下钻，从机构→部门→产品逐层穿透")
    public Map<String, Object> drillDown(
            String parentDim,
            String parentValue,
            String childDim,
            String period) {

        // 查询子维度数据
        String sql = "SELECT " + childDim.toLowerCase() + ", " +
                    "SUM(revenue) as revenue, SUM(cost) as cost, SUM(profit) as profit " +
                    "FROM dw_indicator_fact " +
                    "WHERE period = ? AND " + parentDim.toLowerCase() + " = ? " +
                    "GROUP BY " + childDim.toLowerCase();

        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, period, parentValue);

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("parentDim", parentDim);
        result.put("parentValue", parentValue);
        result.put("childDim", childDim);
        result.put("period", period);

        return result;
    }

    /**
     * MCP工具：同比环比分析
     * 自动计算同比、环比、预算达成率
     */
    @McpTool(name = "query_period_compare", description = "自动计算同比、环比、预算达成率")
    public Map<String, Object> queryPeriodCompare(
            String dimType,
            String dimValue,
            String currentPeriod,
            String compareType) {

        // 计算上期/去年同期
        String basePeriod = calculateBasePeriod(currentPeriod, compareType);

        // 查询当期数据
        String currentSql = "SELECT SUM(revenue) as revenue, SUM(cost) as cost, SUM(profit) as profit " +
                           "FROM dw_indicator_fact WHERE period = ? AND " + dimType.toLowerCase() + " = ?";
        Map<String, Object> currentData = jdbcTemplate.queryForMap(currentSql, currentPeriod, dimValue);

        // 查询基期数据
        Map<String, Object> baseData = jdbcTemplate.queryForMap(currentSql, basePeriod, dimValue);

        // 计算变化率
        Map<String, Object> result = new HashMap<>();
        result.put("current", currentData);
        result.put("base", baseData);
        result.put("currentPeriod", currentPeriod);
        result.put("basePeriod", basePeriod);
        result.put("compareType", compareType);

        // 计算各项指标的变化率
        for (String metric : Arrays.asList("revenue", "cost", "profit")) {
            double current = toDouble(currentData.get(metric));
            double base = toDouble(baseData.get(metric));
            if (base != 0) {
                double changeRate = (current - base) / base * 100;
                result.put(metric + "ChangeRate", Math.round(changeRate * 100.0) / 100.0);
            }
        }

        return result;
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
