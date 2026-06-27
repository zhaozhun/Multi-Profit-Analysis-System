package com.multiprofit.skill.impl;

import com.multiprofit.ai.FunctionRegistry;
import com.multiprofit.skill.SkillContext;
import com.multiprofit.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 异常检测技能
 * 检测指标异常波动
 */
@Slf4j
@Component
public class AnomalyDetectionSkill extends SimpleSkill {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "anomaly-detection";
    }

    @Override
    public String getDescription() {
        return "检测指标异常波动";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("异常", "预警", "风险", "监控", "检测异常");
    }

    @Override
    public SkillResult execute(SkillContext context) {
        log.info("执行异常检测技能");

        String period = context.getPeriod();
        double threshold = context.getDoubleParam("threshold");
        if (threshold == 0) {
            threshold = 10.0; // 默认10%阈值
        }

        try {
            // 检测异常
            List<String> metrics = Arrays.asList("REVENUE", "COST", "PROFIT");
            List<Map<String, Object>> anomalies = detectAnomalies(period, metrics, threshold);

            // 生成报告
            String report = generateReport(anomalies, period, threshold);

            return SkillResult.success(report, Map.of("anomalies", anomalies));

        } catch (Exception e) {
            log.error("异常检测失败", e);
            return SkillResult.failed("异常检测失败: " + e.getMessage());
        }
    }

    /**
     * 检测异常
     */
    private List<Map<String, Object>> detectAnomalies(String period, List<String> metrics, double threshold) {
        Map<String, Object> params = new HashMap<>();
        params.put("period", period);
        params.put("metrics", metrics);
        params.put("threshold", threshold);

        List<Map<String, Object>> anomalies = (List<Map<String, Object>>) functionRegistry.execute(
            "detect_anomaly", params);

        return anomalies != null ? anomalies : Collections.emptyList();
    }

    /**
     * 生成报告
     */
    private String generateReport(List<Map<String, Object>> anomalies, String period, double threshold) {
        StringBuilder report = new StringBuilder();

        report.append("⚠️ 异常检测报告\n\n");
        report.append("【检测期间】").append(period).append("\n");
        report.append("【检测阈值】").append(String.format("%.1f", threshold)).append("%\n\n");

        if (anomalies.isEmpty()) {
            report.append("【检测结果】\n");
            report.append("• 未发现异常波动\n");
        } else {
            report.append("【检测结果】\n");
            report.append("• 发现 ").append(anomalies.size()).append(" 个异常指标\n\n");

            for (int i = 0; i < anomalies.size(); i++) {
                Map<String, Object> anomaly = anomalies.get(i);
                String metric = extractString(anomaly, "metric");
                double changeRate = extractDouble(anomaly, "changeRate");
                String level = extractString(anomaly, "level");

                report.append(i + 1).append(". ").append(getMetricName(metric));
                report.append("（").append(level).append("）\n");
                report.append("   • 变化率：").append(String.format("%.2f", changeRate)).append("%\n");
                report.append("   • 当期值：").append(formatAmount(extractDouble(anomaly, "currentValue"))).append("\n");
                report.append("   • 基期值：").append(formatAmount(extractDouble(anomaly, "baseValue"))).append("\n\n");
            }

            report.append("【建议】\n");
            report.append("• 重点关注严重异常指标，分析原因\n");
            report.append("• 及时采取措施，防止风险扩大\n");
        }

        return report.toString();
    }

    /**
     * 获取指标名称
     */
    private String getMetricName(String metric) {
        switch (metric) {
            case "REVENUE": return "收入";
            case "COST": return "成本";
            case "PROFIT": return "利润";
            default: return metric;
        }
    }
}
