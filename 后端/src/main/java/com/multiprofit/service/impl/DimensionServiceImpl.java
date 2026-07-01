package com.multiprofit.service.impl;

import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.dto.DimensionAnalysisDTO;
import com.multiprofit.service.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 维度分析服务实现类
 * 数据源：dw_indicator_fact（预计算数据）
 */
@Service
public class DimensionServiceImpl implements DimensionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取数据库中最新的月份
     */
    private String getLatestPeriod() {
        try {
            String latest = jdbcTemplate.queryForObject(
                "SELECT MAX(period) FROM dw_indicator_fact WHERE period_type = 'MONTH'", String.class);
            return latest != null ? latest : "2026-06";
        } catch (Exception e) {
            return "2026-06";
        }
    }

    /**
     * 智能获取期间：如果指定期间没有数据，自动使用最新期间
     */
    private String getEffectivePeriod(String startDate) {
        String requestedPeriod = startDate.substring(0, 7);
        // 检查指定期间是否有数据
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ? AND period_type = 'MONTH'",
            Integer.class, requestedPeriod);
        if (count != null && count > 0) {
            return requestedPeriod;
        }
        // 没有数据，使用最新期间
        return getLatestPeriod();
    }

    @Override
    public DimensionAnalysisDTO getAnalysisData(String dimType, String startDate, String endDate,
                                                 String caliberType, Long parentId, Integer level) {
        String period = getEffectivePeriod(startDate);

        // 从 dw_indicator_fact 获取该维度的各项指标
        String sql = "SELECT dim_name, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = ? AND caliber_type = ? " +
            "AND indicator_code IN ('TOTAL_PROFIT', 'LOAN_PROFIT', 'DEPOSIT_PROFIT', 'INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST') " +
            "ORDER BY dim_name";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, dimType, caliberType);

        // 按维度名称分组
        Map<String, Map<String, BigDecimal>> dimMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String dimName = (String) row.get("dim_name");
            String indicatorCode = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");

            dimMap.computeIfAbsent(dimName, k -> new HashMap<>());
            dimMap.get(dimName).put(indicatorCode, value);
        }

        // 从 TOTAL 汇总数据读取 KPI 卡片（与驾驶舱一致）
        String totalSql = "SELECT indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND indicator_code IN ('TOTAL_PROFIT', 'INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST')";

        List<Map<String, Object>> totalRows = jdbcTemplate.queryForList(totalSql, period, caliberType);

        Map<String, BigDecimal> totalIndicatorMap = new HashMap<>();
        for (Map<String, Object> row : totalRows) {
            String code = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");
            totalIndicatorMap.put(code, value);
        }

        BigDecimal totalProfit = totalIndicatorMap.getOrDefault("TOTAL_PROFIT", BigDecimal.ZERO);
        BigDecimal totalIncome = totalIndicatorMap.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO);
        BigDecimal totalCost = totalIndicatorMap.getOrDefault("FTP_COST", BigDecimal.ZERO)
            .add(totalIndicatorMap.getOrDefault("RISK_COST", BigDecimal.ZERO))
            .add(totalIndicatorMap.getOrDefault("OP_COST", BigDecimal.ZERO));

        List<DashboardDTO.KpiCard> kpiCards = new ArrayList<>();
        kpiCards.add(createKpiCard("TOTAL_PROFIT", "总利润", totalProfit, "万元"));
        kpiCards.add(createKpiCard("INTEREST_INCOME", "利息收入", totalIncome, "万元"));
        kpiCards.add(createKpiCard("TOTAL_COST", "总成本", totalCost, "万元"));

        // 构建排名数据
        List<DimensionAnalysisDTO.RankItem> ranking = new ArrayList<>();
        int rankIndex = 1;
        for (Map.Entry<String, Map<String, BigDecimal>> entry : dimMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            DimensionAnalysisDTO.RankItem rankItem = new DimensionAnalysisDTO.RankItem();
            rankItem.setId((long) rankIndex);
            rankItem.setName(entry.getKey());
            rankItem.setNetProfit(values.getOrDefault("TOTAL_PROFIT", BigDecimal.ZERO));
            rankItem.setLoanProfit(values.getOrDefault("LOAN_PROFIT", BigDecimal.ZERO));
            rankItem.setDepositProfit(values.getOrDefault("DEPOSIT_PROFIT", BigDecimal.ZERO));
            rankItem.setRevenue(values.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO));
            rankItem.setYoyGrowth(BigDecimal.ZERO);
            rankItem.setRankIndex(rankIndex++);
            ranking.add(rankItem);
        }

        // 构建表格数据
        DimensionAnalysisDTO.TableData tableData = new DimensionAnalysisDTO.TableData();
        List<DimensionAnalysisDTO.TableRow> tableRows = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : dimMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            DimensionAnalysisDTO.TableRow row = new DimensionAnalysisDTO.TableRow();
            row.setName(entry.getKey());
            row.setLoanProfit(values.getOrDefault("LOAN_PROFIT", BigDecimal.ZERO));
            row.setDepositProfit(values.getOrDefault("DEPOSIT_PROFIT", BigDecimal.ZERO));
            row.setNetProfit(values.getOrDefault("TOTAL_PROFIT", BigDecimal.ZERO));
            row.setRevenue(values.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO));
            row.setFtpCost(values.getOrDefault("FTP_COST", BigDecimal.ZERO));
            row.setRiskCost(values.getOrDefault("RISK_COST", BigDecimal.ZERO));
            row.setOpCost(values.getOrDefault("OP_COST", BigDecimal.ZERO));
            tableRows.add(row);
        }
        tableData.setRows(tableRows);
        tableData.setTotal(tableRows.size());

        // 构建结果
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
        String period = getEffectivePeriod(startDate);

        // 从dimension_master获取维度数据
        String dimSql = "SELECT id, code, name, parent_id, level FROM dimension_master " +
            "WHERE dim_type = ? AND parent_id = ? ORDER BY sort_order";

        List<Map<String, Object>> dimRows = jdbcTemplate.queryForList(dimSql, dimType, parentId);

        // 从dw_indicator_fact获取指标数据（按dim_name关联）
        String indicatorSql = "SELECT dim_name, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = ? AND caliber_type = ? " +
            "AND indicator_code IN ('TOTAL_PROFIT', 'INTEREST_INCOME', 'LOAN_PROFIT', 'DEPOSIT_PROFIT')";

        List<Map<String, Object>> indicatorRows = jdbcTemplate.queryForList(indicatorSql, period, dimType, caliberType);

        // 按dim_name分组指标数据
        Map<String, Map<String, BigDecimal>> indicatorMap = new HashMap<>();
        for (Map<String, Object> row : indicatorRows) {
            String dimName = (String) row.get("dim_name");
            String code = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");
            indicatorMap.computeIfAbsent(dimName, k -> new HashMap<>()).put(code, value);
        }

        // 构建树节点
        List<DimensionAnalysisDTO.TreeNode> treeNodes = new ArrayList<>();
        for (Map<String, Object> dimRow : dimRows) {
            Long dimId = ((Number) dimRow.get("id")).longValue();
            String name = (String) dimRow.get("name");
            int level = (Integer) dimRow.get("level");

            Map<String, BigDecimal> indicators = indicatorMap.getOrDefault(name, new HashMap<>());

            DimensionAnalysisDTO.TreeNode node = new DimensionAnalysisDTO.TreeNode();
            node.setId(dimId);
            node.setKey(String.valueOf(dimId));
            node.setName(name);
            node.setLevel(level);
            node.setNetProfit(indicators.getOrDefault("TOTAL_PROFIT", BigDecimal.ZERO));
            node.setLoanProfit(indicators.getOrDefault("LOAN_PROFIT", BigDecimal.ZERO));
            node.setDepositProfit(indicators.getOrDefault("DEPOSIT_PROFIT", BigDecimal.ZERO));
            node.setRevenue(indicators.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO));

            // 检查是否有子节点
            String childCountSql = "SELECT COUNT(*) FROM dimension_master WHERE dim_type = ? AND parent_id = ?";
            Integer childCount = jdbcTemplate.queryForObject(childCountSql, Integer.class, dimType, dimId);
            node.setChildCount(childCount != null ? childCount : 0);

            treeNodes.add(node);
        }

        return treeNodes;
    }

    @Override
    public List<DimensionAnalysisDTO.RankItem> getRanking(String dimType, String startDate, String endDate,
                                                           String caliberType, String rankBy, int limit) {
        String period = startDate.substring(0, 7);

        // 根据排名字段选择指标
        String indicatorCode;
        switch (rankBy) {
            case "LOAN_PROFIT":
                indicatorCode = "LOAN_PROFIT";
                break;
            case "DEPOSIT_PROFIT":
                indicatorCode = "DEPOSIT_PROFIT";
                break;
            case "REVENUE":
                indicatorCode = "INTEREST_INCOME";
                break;
            default:
                indicatorCode = "TOTAL_PROFIT";
        }

        String sql = "SELECT dim_name, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code = ? AND period = ? AND period_type = 'MONTH' " +
            "AND dim_type = ? AND caliber_type = ? " +
            "ORDER BY calc_value DESC LIMIT ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, indicatorCode, period, dimType, caliberType, limit);

        List<DimensionAnalysisDTO.RankItem> ranking = new ArrayList<>();
        int rankIndex = 1;
        for (Map<String, Object> row : rows) {
            DimensionAnalysisDTO.RankItem rankItem = new DimensionAnalysisDTO.RankItem();
            rankItem.setId((long) rankIndex);
            rankItem.setName((String) row.get("dim_name"));
            rankItem.setNetProfit((BigDecimal) row.get("calc_value"));
            rankItem.setRankIndex(rankIndex++);
            ranking.add(rankItem);
        }

        return ranking;
    }

    @Override
    public DimensionAnalysisDTO.TableRow getDetail(Long dimId, String dimType, String startDate, String endDate) {
        // 简单实现：返回空行
        return new DimensionAnalysisDTO.TableRow();
    }

    @Override
    public List<DimensionAnalysisDTO.TableRow> crossDrill(String fromDimType, String fromDimName,
                                                           String toDimType, String startDate, String endDate,
                                                           String caliberType) {
        // 简单实现：返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> crossDrillTree(String fromDimType, Long fromDimId,
                                                     String toDimType, String startDate, String endDate,
                                                     String caliberType) {
        // 简单实现：返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getDrillPath(String dimType, Long currentId) {
        // 简单实现：返回空路径
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getDimHierarchy(String dimType) {
        // 简单实现：返回空层级
        return new ArrayList<>();
    }

    /**
     * 创建 KPI 卡片
     */
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

    /**
     * 获取维度中文名称
     */
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
}
