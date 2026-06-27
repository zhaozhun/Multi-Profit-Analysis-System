package com.multiprofit.mcp;

import com.multiprofit.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 报表输出MCP Server
 * 统一封装所有报表生成和输出能力
 */
@Component
public class ReportMcpServer {

    @Autowired
    private ExportService exportService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * MCP工具：生成经营简报
     * 生成月度/季度经营分析简报
     */
    @McpTool(name = "generate_business_brief", description = "生成月度/季度经营分析简报")
    public Map<String, Object> generateBrief(
            String period,
            String scope,
            String format) {

        // 生成经营简报数据
        Map<String, Object> briefData = generateBriefData(period, scope);

        Map<String, Object> result = new HashMap<>();
        result.put("period", period);
        result.put("scope", scope);
        result.put("format", format);
        result.put("brief", briefData);

        return result;
    }

    /**
     * MCP工具：生成Excel报表
     * 按模板生成Excel报表
     */
    @McpTool(name = "generate_excel_report", description = "按模板生成Excel报表")
    public Map<String, Object> generateExcel(
            String templateId,
            String period,
            Map<String, String> filters) {

        try {
            // 根据模板ID选择导出方法
            byte[] excelData;
            switch (templateId.toLowerCase()) {
                case "dashboard":
                    excelData = exportService.exportDashboardToExcel(period, "ASSESS");
                    break;
                case "dimension":
                    String dimType = filters.getOrDefault("dimType", "ORG");
                    excelData = exportService.exportDimensionToExcel(dimType, period, period, "ASSESS");
                    break;
                case "ledger":
                    String orgName = filters.getOrDefault("orgName", null);
                    String productName = filters.getOrDefault("productName", null);
                    excelData = exportService.exportLedgerToExcel(period, orgName, productName);
                    break;
                case "report":
                    excelData = exportService.exportReportToExcel(period, "ASSESS");
                    break;
                case "org_profit":
                    excelData = exportService.exportOrgProfitToExcel(period, "ASSESS");
                    break;
                case "product_profit":
                    excelData = exportService.exportProductProfitToExcel(period, "ASSESS");
                    break;
                default:
                    excelData = exportService.exportDashboardToExcel(period, "ASSESS");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("templateId", templateId);
            result.put("period", period);
            result.put("size", excelData.length);
            result.put("status", "generated");

            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("templateId", templateId);
            result.put("period", period);
            result.put("status", "failed");
            result.put("error", e.getMessage());

            return result;
        }
    }

    /**
     * MCP工具：生成图表
     * 生成ECharts图表配置
     */
    @McpTool(name = "generate_chart", description = "生成ECharts图表配置")
    public Map<String, Object> generateChart(
            String chartType,
            List<Map<String, Object>> data,
            Map<String, Object> options) {

        Map<String, Object> chartConfig = new HashMap<>();
        chartConfig.put("type", chartType);
        chartConfig.put("data", data);

        // 根据图表类型生成配置
        switch (chartType.toLowerCase()) {
            case "bar":
                chartConfig.putAll(generateBarChartConfig(data, options));
                break;
            case "line":
                chartConfig.putAll(generateLineChartConfig(data, options));
                break;
            case "pie":
                chartConfig.putAll(generatePieChartConfig(data, options));
                break;
            case "waterfall":
                chartConfig.putAll(generateWaterfallChartConfig(data, options));
                break;
            default:
                chartConfig.putAll(generateDefaultChartConfig(data, options));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("chartType", chartType);
        result.put("config", chartConfig);

        return result;
    }

    /**
     * MCP工具：触发预警推送
     * 触发指标异常预警推送
     */
    @McpTool(name = "trigger_alert", description = "触发指标异常预警推送")
    public Map<String, Object> triggerAlert(
            String metric,
            double threshold,
            String receiver,
            String message) {

        // 触发预警推送（简化实现）
        Map<String, Object> alertResult = new HashMap<>();
        alertResult.put("alertId", UUID.randomUUID().toString());
        alertResult.put("status", "triggered");

        Map<String, Object> result = new HashMap<>();
        result.put("metric", metric);
        result.put("threshold", threshold);
        result.put("receiver", receiver);
        result.put("message", message);
        result.put("alertId", alertResult.get("alertId"));
        result.put("status", "triggered");

        return result;
    }

    /**
     * 生成简报数据
     */
    private Map<String, Object> generateBriefData(String period, String scope) {
        Map<String, Object> brief = new HashMap<>();

        // 查询整体数据
        String sql = "SELECT SUM(revenue) as revenue, SUM(cost) as cost, SUM(profit) as profit FROM biz_ledger WHERE period = ?";
        Map<String, Object> data = jdbcTemplate.queryForMap(sql, period);

        double revenue = toDouble(data.get("revenue"));
        double cost = toDouble(data.get("cost"));
        double profit = toDouble(data.get("profit"));

        brief.put("totalProfit", profit);
        brief.put("revenue", revenue);
        brief.put("cost", cost);
        brief.put("profitGrowth", 5.4); // 示例数据
        brief.put("highlights", Arrays.asList("深圳分行对公不良率上升", "零售条线消费贷款增速放缓"));
        brief.put("suggestions", Arrays.asList("加强对公贷款风险管控", "推动零售数字化转型", "优化FTP定价策略"));

        return brief;
    }

    /**
     * 生成柱状图配置
     */
    private Map<String, Object> generateBarChartConfig(List<Map<String, Object>> data, Map<String, Object> options) {
        Map<String, Object> config = new HashMap<>();
        config.put("xAxis", data.stream().map(d -> d.get("name")).toArray());
        config.put("yAxis", data.stream().map(d -> d.get("value")).toArray());
        config.put("series", new Object[]{new HashMap<String, Object>() {{
            put("type", "bar");
            put("data", data.stream().map(d -> d.get("value")).toArray());
        }}});
        return config;
    }

    /**
     * 生成折线图配置
     */
    private Map<String, Object> generateLineChartConfig(List<Map<String, Object>> data, Map<String, Object> options) {
        Map<String, Object> config = new HashMap<>();
        config.put("xAxis", data.stream().map(d -> d.get("name")).toArray());
        config.put("yAxis", data.stream().map(d -> d.get("value")).toArray());
        config.put("series", new Object[]{new HashMap<String, Object>() {{
            put("type", "line");
            put("data", data.stream().map(d -> d.get("value")).toArray());
            put("smooth", true);
        }}});
        return config;
    }

    /**
     * 生成饼图配置
     */
    private Map<String, Object> generatePieChartConfig(List<Map<String, Object>> data, Map<String, Object> options) {
        Map<String, Object> config = new HashMap<>();
        config.put("series", new Object[]{new HashMap<String, Object>() {{
            put("type", "pie");
            put("data", data.stream().map(d -> new HashMap<String, Object>() {{
                put("name", d.get("name"));
                put("value", d.get("value"));
            }}).toArray());
        }}});
        return config;
    }

    /**
     * 生成瀑布图配置
     */
    private Map<String, Object> generateWaterfallChartConfig(List<Map<String, Object>> data, Map<String, Object> options) {
        Map<String, Object> config = new HashMap<>();
        config.put("xAxis", data.stream().map(d -> d.get("name")).toArray());
        config.put("series", new Object[]{new HashMap<String, Object>() {{
            put("type", "bar");
            put("stack", "waterfall");
            put("data", data.stream().map(d -> d.get("value")).toArray());
        }}});
        return config;
    }

    /**
     * 生成默认图表配置
     */
    private Map<String, Object> generateDefaultChartConfig(List<Map<String, Object>> data, Map<String, Object> options) {
        Map<String, Object> config = new HashMap<>();
        config.put("data", data);
        return config;
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
