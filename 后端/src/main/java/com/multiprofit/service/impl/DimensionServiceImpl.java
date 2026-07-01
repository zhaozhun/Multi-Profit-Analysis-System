package com.multiprofit.service.impl;

import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.dto.DimensionAnalysisDTO;
import com.multiprofit.service.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 维度分析服务实现类(改造版)
 * 数据源：dw_indicator_fact(period_type+period), JOIN dim_*表
 */
@Service
public class DimensionServiceImpl implements DimensionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public DimensionAnalysisDTO getAnalysisData(String dimType, String startDate, String endDate,
                                                 String caliberType, Long parentId, Integer level) {
        String periodType = determinePeriodType(startDate, endDate);
        String period = determinePeriod(startDate, endDate, periodType);
        String dimTable = getDimTable(dimType);

        // 从DWS读取该维度各项指标(按dim_id关联dim_*表)
        String sql = "SELECT f.dim_id, dm.name as dim_name, f.indicator_code, f.calc_value " +
            "FROM dw_indicator_fact f " +
            "JOIN " + dimTable + " dm ON f.dim_id = dm.id " +
            "WHERE f.period = ? AND f.period_type = ? AND f.dim_type = ? AND f.caliber_type = ? " +
            "AND f.indicator_code IN ('TOTAL_MONTHLY_PROFIT','LOAN_MONTHLY_PROFIT','DEPOSIT_MONTHLY_PROFIT','LOAN_MONTHLY_INTEREST','FTP_MONTHLY_INCOME','LOAN_FTP_COST','LOAN_RISK_COST','LOAN_OP_COST','DEPOSIT_OP_COST','INTEREST_MONTHLY_EXPENSE') " +
            "ORDER BY dm.name";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, periodType, dimType, caliberType);

        // 按维度名称分组
        Map<String, Map<String, BigDecimal>> dimMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String dimName = (String) row.get("dim_name");
            String indicatorCode = (String) row.get("indicator_code");
            Object val = row.get("calc_value");
            BigDecimal value = val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO;
            dimMap.computeIfAbsent(dimName, k -> new HashMap<>()).put(indicatorCode, value);
        }

        // TOTAL汇总
        String totalSql = "SELECT indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = ? AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND indicator_code IN ('TOTAL_MONTHLY_PROFIT','LOAN_MONTHLY_INTEREST','FTP_MONTHLY_INCOME','LOAN_FTP_COST','LOAN_RISK_COST','LOAN_OP_COST','DEPOSIT_OP_COST','INTEREST_MONTHLY_EXPENSE')";

        List<Map<String, Object>> totalRows = jdbcTemplate.queryForList(totalSql, period, periodType, caliberType);

        Map<String, BigDecimal> totalIndicatorMap = new HashMap<>();
        for (Map<String, Object> row : totalRows) {
            Object val = row.get("calc_value");
            totalIndicatorMap.put((String) row.get("indicator_code"),
                val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO);
        }

        BigDecimal totalProfit = totalIndicatorMap.getOrDefault("TOTAL_MONTHLY_PROFIT", BigDecimal.ZERO);
        BigDecimal totalIncome = totalIndicatorMap.getOrDefault("LOAN_MONTHLY_INTEREST", BigDecimal.ZERO)
            .add(totalIndicatorMap.getOrDefault("FTP_MONTHLY_INCOME", BigDecimal.ZERO));
        BigDecimal totalCost = totalIndicatorMap.getOrDefault("LOAN_FTP_COST", BigDecimal.ZERO)
            .add(totalIndicatorMap.getOrDefault("LOAN_RISK_COST", BigDecimal.ZERO))
            .add(totalIndicatorMap.getOrDefault("LOAN_OP_COST", BigDecimal.ZERO))
            .add(totalIndicatorMap.getOrDefault("DEPOSIT_OP_COST", BigDecimal.ZERO))
            .add(totalIndicatorMap.getOrDefault("INTEREST_MONTHLY_EXPENSE", BigDecimal.ZERO));

        List<DashboardDTO.KpiCard> kpiCards = new ArrayList<>();
        kpiCards.add(createKpiCard("TOTAL_MONTHLY_PROFIT", "总利润", totalProfit, "万元"));
        kpiCards.add(createKpiCard("LOAN_MONTHLY_INTEREST", "收入合计", totalIncome, "万元"));
        kpiCards.add(createKpiCard("TOTAL_COST", "成本合计", totalCost, "万元"));

        // 排名数据
        List<DimensionAnalysisDTO.RankItem> ranking = new ArrayList<>();
        int rankIndex = 1;
        for (Map.Entry<String, Map<String, BigDecimal>> entry : dimMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            DimensionAnalysisDTO.RankItem rankItem = new DimensionAnalysisDTO.RankItem();
            rankItem.setId((long) rankIndex);
            rankItem.setName(entry.getKey());
            rankItem.setNetProfit(values.getOrDefault("TOTAL_MONTHLY_PROFIT", BigDecimal.ZERO));
            rankItem.setLoanProfit(values.getOrDefault("LOAN_MONTHLY_PROFIT", BigDecimal.ZERO));
            rankItem.setDepositProfit(values.getOrDefault("DEPOSIT_MONTHLY_PROFIT", BigDecimal.ZERO));
            rankItem.setRevenue(values.getOrDefault("LOAN_MONTHLY_INTEREST", BigDecimal.ZERO)
                .add(values.getOrDefault("FTP_MONTHLY_INCOME", BigDecimal.ZERO)));
            rankItem.setYoyGrowth(BigDecimal.ZERO);
            rankItem.setRankIndex(rankIndex++);
            ranking.add(rankItem);
        }

        // 表格数据
        DimensionAnalysisDTO.TableData tableData = new DimensionAnalysisDTO.TableData();
        List<DimensionAnalysisDTO.TableRow> tableRows = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : dimMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            DimensionAnalysisDTO.TableRow row = new DimensionAnalysisDTO.TableRow();
            row.setName(entry.getKey());
            row.setLoanProfit(values.getOrDefault("LOAN_MONTHLY_PROFIT", BigDecimal.ZERO));
            row.setDepositProfit(values.getOrDefault("DEPOSIT_MONTHLY_PROFIT", BigDecimal.ZERO));
            row.setNetProfit(values.getOrDefault("TOTAL_MONTHLY_PROFIT", BigDecimal.ZERO));
            row.setRevenue(values.getOrDefault("LOAN_MONTHLY_INTEREST", BigDecimal.ZERO)
                .add(values.getOrDefault("FTP_MONTHLY_INCOME", BigDecimal.ZERO)));
            row.setFtpCost(values.getOrDefault("LOAN_FTP_COST", BigDecimal.ZERO));
            row.setRiskCost(values.getOrDefault("LOAN_RISK_COST", BigDecimal.ZERO));
            row.setOpCost(values.getOrDefault("LOAN_OP_COST", BigDecimal.ZERO)
                .add(values.getOrDefault("DEPOSIT_OP_COST", BigDecimal.ZERO)));
            tableRows.add(row);
        }
        tableData.setRows(tableRows);
        tableData.setTotal(tableRows.size());

        DimensionAnalysisDTO result = new DimensionAnalysisDTO();
        result.setDimType(dimType);
        result.setDimLabel(getDimName(dimType));
        result.setCurrentLevel(level != null ? level : 1);
        result.setKpiCards(kpiCards);
        result.setRanking(ranking);
        result.setTableData(tableData);
        result.setTreeData(new ArrayList<>());
        result.setCostStructure(new ArrayList<>());
        result.setDrillPath(new ArrayList<>());

        return result;
    }

    @Override
    public List<DimensionAnalysisDTO.TreeNode> getTreeData(String dimType, String startDate, String endDate,
                                                            String caliberType, Long parentId) {
        String periodType = determinePeriodType(startDate, endDate);
        String period = determinePeriod(startDate, endDate, periodType);
        String dimTable = getDimTable(dimType);

        // 从dim_*表获取维度层级
        String dimSql = "SELECT id, code, name, parent_id, level FROM " + dimTable +
            " WHERE parent_id = ? ORDER BY sort_order";
        List<Map<String, Object>> dimRows = jdbcTemplate.queryForList(dimSql, parentId);

        // 从DWS获取指标数据(按dim_id关联)
        String indicatorSql = "SELECT f.dim_id, f.indicator_code, f.calc_value FROM dw_indicator_fact f " +
            "WHERE f.period = ? AND f.period_type = ? AND f.dim_type = ? AND f.caliber_type = ? " +
            "AND f.indicator_code IN ('TOTAL_MONTHLY_PROFIT','LOAN_MONTHLY_INTEREST','FTP_MONTHLY_INCOME','LOAN_MONTHLY_PROFIT','DEPOSIT_MONTHLY_PROFIT')";

        List<Map<String, Object>> indicatorRows = jdbcTemplate.queryForList(indicatorSql, period, periodType, dimType, caliberType);

        // 按dim_id分组指标数据
        Map<Long, Map<String, BigDecimal>> indicatorMap = new HashMap<>();
        for (Map<String, Object> row : indicatorRows) {
            Long dimId = ((Number) row.get("dim_id")).longValue();
            String code = (String) row.get("indicator_code");
            Object val = row.get("calc_value");
            BigDecimal value = val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO;
            indicatorMap.computeIfAbsent(dimId, k -> new HashMap<>()).put(code, value);
        }

        // 构建树节点
        List<DimensionAnalysisDTO.TreeNode> treeNodes = new ArrayList<>();
        for (Map<String, Object> dimRow : dimRows) {
            Long dimId = ((Number) dimRow.get("id")).longValue();
            String name = (String) dimRow.get("name");
            int level = (Integer) dimRow.get("level");

            Map<String, BigDecimal> indicators = indicatorMap.getOrDefault(dimId, new HashMap<>());
            // TOTAL_MONTHLY_PROFIT = LOAN_MONTHLY_PROFIT + DEPOSIT_MONTHLY_PROFIT (维度级别)
            BigDecimal totalProfit = indicators.getOrDefault("TOTAL_MONTHLY_PROFIT", BigDecimal.ZERO);
            if (totalProfit.compareTo(BigDecimal.ZERO) == 0) {
                totalProfit = indicators.getOrDefault("LOAN_MONTHLY_PROFIT", BigDecimal.ZERO)
                    .add(indicators.getOrDefault("DEPOSIT_MONTHLY_PROFIT", BigDecimal.ZERO));
            }

            DimensionAnalysisDTO.TreeNode node = new DimensionAnalysisDTO.TreeNode();
            node.setId(dimId);
            node.setKey(String.valueOf(dimId));
            node.setName(name);
            node.setLevel(level);
            node.setNetProfit(totalProfit);
            node.setLoanProfit(indicators.getOrDefault("LOAN_MONTHLY_PROFIT", BigDecimal.ZERO));
            node.setDepositProfit(indicators.getOrDefault("DEPOSIT_MONTHLY_PROFIT", BigDecimal.ZERO));
            node.setRevenue(indicators.getOrDefault("LOAN_MONTHLY_INTEREST", BigDecimal.ZERO)
                .add(indicators.getOrDefault("FTP_MONTHLY_INCOME", BigDecimal.ZERO)));

            // 检查子节点数
            String childCountSql = "SELECT COUNT(*) FROM " + dimTable + " WHERE parent_id = ?";
            Integer childCount = jdbcTemplate.queryForObject(childCountSql, Integer.class, dimId);
            node.setChildCount(childCount != null ? childCount : 0);

            treeNodes.add(node);
        }

        return treeNodes;
    }

    @Override
    public List<DimensionAnalysisDTO.RankItem> getRanking(String dimType, String startDate, String endDate,
                                                           String caliberType, String rankBy, int limit) {
        String periodType = determinePeriodType(startDate, endDate);
        String period = determinePeriod(startDate, endDate, periodType);
        String dimTable = getDimTable(dimType);

        String indicatorCode;
        switch (rankBy) {
            case "LOAN_PROFIT": indicatorCode = "LOAN_MONTHLY_PROFIT"; break;
            case "DEPOSIT_PROFIT": indicatorCode = "DEPOSIT_MONTHLY_PROFIT"; break;
            case "REVENUE": indicatorCode = "LOAN_MONTHLY_INTEREST"; break;
            default: indicatorCode = "TOTAL_MONTHLY_PROFIT";
        }

        String sql = "SELECT dm.name as dim_name, f.calc_value FROM dw_indicator_fact f " +
            "JOIN " + dimTable + " dm ON f.dim_id = dm.id " +
            "WHERE f.indicator_code = ? AND f.period = ? AND f.period_type = ? " +
            "AND f.dim_type = ? AND f.caliber_type = ? ORDER BY f.calc_value DESC LIMIT ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, indicatorCode, period, periodType, dimType, caliberType, limit);

        List<DimensionAnalysisDTO.RankItem> ranking = new ArrayList<>();
        int rankIndex = 1;
        for (Map<String, Object> row : rows) {
            Object val = row.get("calc_value");
            BigDecimal value = val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO;
            DimensionAnalysisDTO.RankItem rankItem = new DimensionAnalysisDTO.RankItem();
            rankItem.setId((long) rankIndex);
            rankItem.setName((String) row.get("dim_name"));
            rankItem.setNetProfit(value);
            rankItem.setRankIndex(rankIndex++);
            ranking.add(rankItem);
        }

        return ranking;
    }

    @Override
    public DimensionAnalysisDTO.TableRow getDetail(Long dimId, String dimType, String startDate, String endDate) {
        return new DimensionAnalysisDTO.TableRow();
    }

    @Override
    public List<DimensionAnalysisDTO.TableRow> crossDrill(String fromDimType, String fromDimName,
                                                           String toDimType, String startDate, String endDate,
                                                           String caliberType) {
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> crossDrillTree(String fromDimType, Long fromDimId,
                                                     String toDimType, String startDate, String endDate,
                                                     String caliberType) {
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getDrillPath(String dimType, Long currentId) {
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getDimHierarchy(String dimType) {
        return new ArrayList<>();
    }

    // ========== 辅助方法 ==========

    private String determinePeriodType(String startDate, String endDate) {
        if (startDate == null) return "MONTH";
        if (startDate.endsWith("-01-01") && endDate != null && endDate.endsWith("-12-31")) return "YEAR";
        if (startDate.equals(endDate)) return "DAY";
        return "MONTH";
    }

    private String determinePeriod(String startDate, String endDate, String periodType) {
        if (startDate == null) return "2026-06";
        switch (periodType) {
            case "YEAR": return startDate.substring(0, 4);
            case "MONTH": return startDate.substring(0, 7);
            case "DAY": return startDate;
            default: return startDate.substring(0, 7);
        }
    }

    private String getDimTable(String dimType) {
        switch (dimType) {
            case "ORG": return "dim_organization";
            case "BIZ_LINE": return "dim_biz_line";
            case "DEPT": return "dim_dept";
            case "PRODUCT": return "dim_product";
            case "CHANNEL": return "dim_channel";
            case "MANAGER": return "dim_manager";
            case "CUSTOMER": return "dim_customer_type";
            default: return "dim_organization";
        }
    }

    private String getDimName(String dimType) {
        switch (dimType) {
            case "ORG": return "机构";
            case "BIZ_LINE": return "业务线";
            case "DEPT": return "部门";
            case "PRODUCT": return "产品";
            case "CHANNEL": return "渠道";
            case "MANAGER": return "客户经理";
            case "CUSTOMER": return "客户";
            default: return dimType;
        }
    }

    private DashboardDTO.KpiCard createKpiCard(String code, String name, BigDecimal value, String unit) {
        DashboardDTO.KpiCard card = new DashboardDTO.KpiCard();
        card.setMetricCode(code);
        card.setMetricName(name);
        card.setValue(value);
        card.setUnit(unit);
        card.setYoyGrowth(BigDecimal.ZERO);
        card.setMomGrowth(BigDecimal.ZERO);
        card.setBudgetRate(BigDecimal.ZERO);
        card.setColor("#1890ff");
        return card;
    }
}
