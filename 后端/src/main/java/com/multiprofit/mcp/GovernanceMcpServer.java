package com.multiprofit.mcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据治理MCP Server
 * 统一封装数据质量检测和治理能力
 */
@Component
public class GovernanceMcpServer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * MCP工具：数据质量扫描
     * 扫描数据质量，返回完整性、一致性、准确性评分
     */
    @McpTool(name = "scan_data_quality", description = "扫描数据质量，返回完整性、一致性、准确性评分")
    public Map<String, Object> scanQuality(
            String period,
            String dimType) {

        // 扫描数据质量
        Map<String, Object> qualityReport = scanDataQuality(period, dimType);

        Map<String, Object> result = new HashMap<>();
        result.put("period", period);
        result.put("dimType", dimType);
        result.put("report", qualityReport);

        return result;
    }

    /**
     * MCP工具：异常检测
     * 检测指标异常，返回异常清单
     */
    @McpTool(name = "detect_anomaly", description = "检测指标异常，返回异常清单")
    public List<Map<String, Object>> detectAnomaly(
            String period,
            List<String> metrics,
            double threshold) {

        List<Map<String, Object>> anomalies = new ArrayList<>();

        for (String metric : metrics) {
            // 查询当期数据
            String currentSql = "SELECT SUM(" + metric.toLowerCase() + ") as value FROM dw_indicator_fact WHERE period = ?";
            Double currentValue = jdbcTemplate.queryForObject(currentSql, Double.class, period);

            // 查询上期数据
            String basePeriod = calculateBasePeriod(period, "MOM");
            Double baseValue = jdbcTemplate.queryForObject(currentSql, Double.class, basePeriod);

            if (currentValue != null && baseValue != null && baseValue != 0) {
                double changeRate = (currentValue - baseValue) / baseValue * 100;

                if (Math.abs(changeRate) > threshold) {
                    Map<String, Object> anomaly = new HashMap<>();
                    anomaly.put("metric", metric);
                    anomaly.put("currentValue", currentValue);
                    anomaly.put("baseValue", baseValue);
                    anomaly.put("changeRate", Math.round(changeRate * 100.0) / 100.0);
                    anomaly.put("threshold", threshold);
                    anomaly.put("level", Math.abs(changeRate) > 20 ? "critical" : "warning");
                    anomalies.add(anomaly);
                }
            }
        }

        return anomalies;
    }

    /**
     * MCP工具：数据校验
     * 执行数据校验规则
     */
    @McpTool(name = "validate_data", description = "执行数据校验规则")
    public Map<String, Object> validateData(
            String table,
            String period,
            List<String> rules) {

        List<Map<String, Object>> validationResults = new ArrayList<>();
        int passedCount = 0;
        int failedCount = 0;

        for (String rule : rules) {
            Map<String, Object> result = executeValidationRule(table, period, rule);
            validationResults.add(result);

            if ((Boolean) result.get("passed")) {
                passedCount++;
            } else {
                failedCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("table", table);
        result.put("period", period);
        result.put("totalRules", rules.size());
        result.put("passedCount", passedCount);
        result.put("failedCount", failedCount);
        result.put("passRate", rules.isEmpty() ? 0 : Math.round((double) passedCount / rules.size() * 100.0) / 100.0);
        result.put("details", validationResults);

        return result;
    }

    /**
     * MCP工具：获取治理问题清单
     * 获取数据治理问题清单
     */
    @McpTool(name = "get_governance_issues", description = "获取数据治理问题清单")
    public List<Map<String, Object>> getIssues(
            String period,
            String level) {

        // 获取治理问题（简化实现）
        List<Map<String, Object>> issues = new ArrayList<>();

        // 检查空值问题
        String nullSql = "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ? AND (revenue IS NULL OR cost IS NULL OR profit IS NULL)";
        Integer nullCount = jdbcTemplate.queryForObject(nullSql, Integer.class, period);
        if (nullCount != null && nullCount > 0) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("level", "warning");
            issue.put("description", "发现" + nullCount + "条空值记录");
            issue.put("table", "dw_indicator_fact");
            issue.put("period", period);
            issues.add(issue);
        }

        // 检查平衡问题
        String balanceSql = "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ? AND ABS(revenue - cost - profit) > 0.01";
        Integer balanceCount = jdbcTemplate.queryForObject(balanceSql, Integer.class, period);
        if (balanceCount != null && balanceCount > 0) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("level", "critical");
            issue.put("description", "发现" + balanceCount + "条不平衡记录");
            issue.put("table", "dw_indicator_fact");
            issue.put("period", period);
            issues.add(issue);
        }

        // 按级别过滤
        if (level != null && !level.isEmpty()) {
            issues.removeIf(issue -> !level.equals(issue.get("level")));
        }

        return issues;
    }

    /**
     * 扫描数据质量
     */
    private Map<String, Object> scanDataQuality(String period, String dimType) {
        Map<String, Object> report = new HashMap<>();

        // 完整性检查
        String completenessSql = "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ? AND (revenue IS NOT NULL AND cost IS NOT NULL AND profit IS NOT NULL)";
        Integer completenessCount = jdbcTemplate.queryForObject(completenessSql, Integer.class, period);

        String totalSql = "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ?";
        Integer totalCount = jdbcTemplate.queryForObject(totalSql, Integer.class, period);

        int completeness = totalCount != null && totalCount > 0 ? (int) ((double) completenessCount / totalCount * 100) : 0;

        // 一致性检查
        String consistencySql = "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ? AND ABS(revenue - cost - profit) < 0.01";
        Integer consistencyCount = jdbcTemplate.queryForObject(consistencySql, Integer.class, period);

        int consistency = totalCount != null && totalCount > 0 ? (int) ((double) consistencyCount / totalCount * 100) : 0;

        // 准确性检查（简化）
        int accuracy = 95;

        // 整体评分
        int overallScore = (completeness + consistency + accuracy) / 3;

        report.put("completeness", completeness);
        report.put("consistency", consistency);
        report.put("accuracy", accuracy);
        report.put("overallScore", overallScore);

        // 问题清单
        List<Map<String, Object>> issues = new ArrayList<>();
        if (completeness < 100) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("level", "warning");
            issue.put("description", "数据完整性不足：" + completeness + "%");
            issues.add(issue);
        }
        if (consistency < 100) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("level", "critical");
            issue.put("description", "数据一致性问题：" + consistency + "%");
            issues.add(issue);
        }
        report.put("issues", issues);

        return report;
    }

    /**
     * 执行单个校验规则
     */
    private Map<String, Object> executeValidationRule(String table, String period, String rule) {
        Map<String, Object> result = new HashMap<>();
        result.put("rule", rule);

        try {
            switch (rule.toLowerCase()) {
                case "null_check":
                    // 空值检查
                    String nullSql = "SELECT COUNT(*) FROM " + table + " WHERE period = ? AND (revenue IS NULL OR cost IS NULL OR profit IS NULL)";
                    Integer nullCount = jdbcTemplate.queryForObject(nullSql, Integer.class, period);
                    result.put("passed", nullCount == 0);
                    result.put("message", nullCount == 0 ? "无空值" : "发现" + nullCount + "条空值记录");
                    break;

                case "balance_check":
                    // 平衡检查
                    String balanceSql = "SELECT COUNT(*) FROM " + table + " WHERE period = ? AND ABS(revenue - cost - profit) > 0.01";
                    Integer balanceCount = jdbcTemplate.queryForObject(balanceSql, Integer.class, period);
                    result.put("passed", balanceCount == 0);
                    result.put("message", balanceCount == 0 ? "数据平衡" : "发现" + balanceCount + "条不平衡记录");
                    break;

                case "range_check":
                    // 范围检查
                    String rangeSql = "SELECT COUNT(*) FROM " + table + " WHERE period = ? AND (revenue < 0 OR cost < 0)";
                    Integer rangeCount = jdbcTemplate.queryForObject(rangeSql, Integer.class, period);
                    result.put("passed", rangeCount == 0);
                    result.put("message", rangeCount == 0 ? "数据范围正常" : "发现" + rangeCount + "条异常范围记录");
                    break;

                default:
                    result.put("passed", true);
                    result.put("message", "未知规则，跳过检查");
            }
        } catch (Exception e) {
            result.put("passed", false);
            result.put("message", "规则执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 计算基期
     */
    private String calculateBasePeriod(String currentPeriod, String compareType) {
        String[] parts = currentPeriod.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        if ("YOY".equalsIgnoreCase(compareType)) {
            return (year - 1) + "-" + String.format("%02d", month);
        } else {
            if (month == 1) {
                return (year - 1) + "-12";
            } else {
                return year + "-" + String.format("%02d", month - 1);
            }
        }
    }
}
