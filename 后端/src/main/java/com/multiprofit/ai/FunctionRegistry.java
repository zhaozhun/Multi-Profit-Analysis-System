package com.multiprofit.ai;

import com.multiprofit.mcp.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;

/**
 * 函数注册中心
 * 注册所有MCP Server的工具方法，提供统一的函数调用接口
 */
@Component
public class FunctionRegistry {

    @Autowired
    private BizDataMcpServer bizDataMcp;

    @Autowired
    private AnalysisMcpServer analysisMcp;

    @Autowired
    private ReportMcpServer reportMcp;

    @Autowired
    private GovernanceMcpServer governanceMcp;

    @Autowired
    private DataIntegrationMcpServer integrationMcp;

    private final Map<String, Function<Map<String, Object>, Object>> functions = new HashMap<>();
    private final Map<String, ModelApiClient.ToolDefinition> toolDefinitions = new HashMap<>();

    @PostConstruct
    public void init() {
        // 业财数据MCP工具
        register("query_profit_metrics", "按维度组合查询盈利指标", params -> {
            List<String> dimensions = getList(params, "dimensions");
            List<String> metrics = getList(params, "metrics");
            String period = getString(params, "period");
            Map<String, String> filters = getMap(params, "filters");
            return bizDataMcp.queryProfitMetrics(dimensions, metrics, period, filters);
        });

        register("drill_down_detail", "维度下钻，从机构→部门→产品逐层穿透", params -> {
            String parentDim = getString(params, "parent_dim");
            String parentValue = getString(params, "parent_value");
            String childDim = getString(params, "child_dim");
            String period = getString(params, "period");
            return bizDataMcp.drillDown(parentDim, parentValue, childDim, period);
        });

        register("query_period_compare", "自动计算同比、环比、预算达成率", params -> {
            String dimType = getString(params, "dim_type");
            String dimValue = getString(params, "dim_value");
            String currentPeriod = getString(params, "current_period");
            String compareType = getString(params, "compare_type");
            return bizDataMcp.queryPeriodCompare(dimType, dimValue, currentPeriod, compareType);
        });

        register("query_master_data", "查询主数据（机构、部门、产品、渠道、客户经理）", params -> {
            String dimType = getString(params, "dim_type");
            String parentCode = getString(params, "parent_code");
            return bizDataMcp.queryMasterData(dimType, parentCode);
        });

        register("query_indicator_fact", "查询业务台账明细数据", params -> {
            String period = getString(params, "period");
            Map<String, String> dimensionFilters = getMap(params, "dimension_filters");
            int page = getInt(params, "page", 1);
            int size = getInt(params, "size", 10);
            return bizDataMcp.queryIndicatorFact(period, dimensionFilters, page, size);
        });

        // 分析算法MCP工具
        register("execute_allocation", "按规则执行费用分摊计算", params -> {
            String ruleCode = getString(params, "rule_code");
            String period = getString(params, "period");
            boolean dryRun = getBoolean(params, "dry_run", false);
            return analysisMcp.executeAllocation(ruleCode, period, dryRun);
        });

        register("calculate_contribution", "计算各维度对利润波动的贡献度", params -> {
            String targetMetric = getString(params, "target_metric");
            String dimension = getString(params, "dimension");
            String basePeriod = getString(params, "base_period");
            String currentPeriod = getString(params, "current_period");
            return analysisMcp.calculateContribution(targetMetric, dimension, basePeriod, currentPeriod);
        });

        register("analyze_sensitivity", "单因素/多因素盈利敏感性测算", params -> {
            List<Map<String, Object>> factors = getList(params, "factors");
            double changeRange = getDouble(params, "change_range", 0.1);
            return analysisMcp.analyzeSensitivity(factors, changeRange);
        });

        register("diagnose_allocation_rules", "诊断分摊规则配置，检查冲突和遗漏", params -> {
            String period = getString(params, "period");
            return analysisMcp.diagnoseRules(period);
        });

        register("get_factor_data", "获取分摊因子的数值数据", params -> {
            String factorCode = getString(params, "factor_code");
            String period = getString(params, "period");
            String dimType = getString(params, "dim_type");
            return analysisMcp.getFactorData(factorCode, period, dimType);
        });

        // 报表输出MCP工具
        register("generate_business_brief", "生成月度/季度经营分析简报", params -> {
            String period = getString(params, "period");
            String scope = getString(params, "scope", "全行");
            String format = getString(params, "format", "text");
            return reportMcp.generateBrief(period, scope, format);
        });

        register("generate_excel_report", "按模板生成Excel报表", params -> {
            String templateId = getString(params, "template_id");
            String period = getString(params, "period");
            Map<String, String> filters = getMap(params, "filters");
            return reportMcp.generateExcel(templateId, period, filters);
        });

        register("generate_chart", "生成ECharts图表配置", params -> {
            String chartType = getString(params, "chart_type");
            List<Map<String, Object>> data = getList(params, "data");
            Map<String, Object> options = getMap(params, "options");
            return reportMcp.generateChart(chartType, data, options);
        });

        register("trigger_alert", "触发指标异常预警推送", params -> {
            String metric = getString(params, "metric");
            double threshold = getDouble(params, "threshold", 0);
            String receiver = getString(params, "receiver");
            String message = getString(params, "message");
            return reportMcp.triggerAlert(metric, threshold, receiver, message);
        });

        // 数据治理MCP工具
        register("scan_data_quality", "扫描数据质量，返回完整性、一致性、准确性评分", params -> {
            String period = getString(params, "period");
            String dimType = getString(params, "dim_type");
            return governanceMcp.scanQuality(period, dimType);
        });

        register("detect_anomaly", "检测指标异常，返回异常清单", params -> {
            String period = getString(params, "period");
            List<String> metrics = getList(params, "metrics");
            double threshold = getDouble(params, "threshold", 10.0);
            return governanceMcp.detectAnomaly(period, metrics, threshold);
        });

        register("validate_data", "执行数据校验规则", params -> {
            String table = getString(params, "table");
            String period = getString(params, "period");
            List<String> rules = getList(params, "rules");
            return governanceMcp.validateData(table, period, rules);
        });

        register("get_governance_issues", "获取数据治理问题清单", params -> {
            String period = getString(params, "period");
            String level = getString(params, "level");
            return governanceMcp.getIssues(period, level);
        });

        // 数据集成MCP工具
        register("detect_file_format", "检测上传文件的格式、编码、字段", params -> {
            String filePath = getString(params, "file_path");
            return integrationMcp.detectFormat(filePath);
        });

        register("map_fields", "将源字段映射到目标表字段", params -> {
            List<String> sourceFields = getList(params, "source_fields");
            String targetTable = getString(params, "target_table");
            return integrationMcp.mapFields(sourceFields, targetTable);
        });

        register("clean_data", "执行数据清洗规则", params -> {
            List<Map<String, Object>> data = getList(params, "data");
            List<String> rules = getList(params, "rules");
            return integrationMcp.cleanData(data, rules);
        });

        register("link_dimensions", "将维度编码关联到dimension_master", params -> {
            List<Map<String, Object>> data = getList(params, "data");
            Map<String, String> dimMappings = getMap(params, "dim_mappings");
            return integrationMcp.linkDimensions(data, dimMappings);
        });

        register("import_data", "将清洗后的数据写入目标表", params -> {
            List<Map<String, Object>> data = getList(params, "data");
            String targetTable = getString(params, "target_table");
            String mode = getString(params, "mode", "insert");
            return integrationMcp.importData(data, targetTable, mode);
        });

        register("generate_quality_report", "生成数据质量报告", params -> {
            String importId = getString(params, "import_id");
            return integrationMcp.generateReport(importId);
        });
    }

    /**
     * 注册函数
     */
    private void register(String name, String description, Function<Map<String, Object>, Object> function) {
        functions.put(name, function);

        // 创建工具定义
        Map<String, Object> inputSchema = createInputSchema(name);
        toolDefinitions.put(name, new ModelApiClient.ToolDefinition(name, description, inputSchema));
    }

    /**
     * 执行函数
     */
    public Object execute(String functionName, Map<String, Object> params) {
        Function<Map<String, Object>, Object> function = functions.get(functionName);
        if (function == null) {
            throw new IllegalArgumentException("未知的函数: " + functionName);
        }
        return function.apply(params);
    }

    /**
     * 获取工具定义
     */
    public ModelApiClient.ToolDefinition getToolDefinition(String functionName) {
        return toolDefinitions.get(functionName);
    }

    /**
     * 获取所有工具定义
     */
    public List<ModelApiClient.ToolDefinition> getAllToolDefinitions() {
        return new ArrayList<>(toolDefinitions.values());
    }

    /**
     * 获取指定工具定义列表
     */
    public List<ModelApiClient.ToolDefinition> getToolDefinitions(List<String> functionNames) {
        List<ModelApiClient.ToolDefinition> definitions = new ArrayList<>();
        for (String name : functionNames) {
            ModelApiClient.ToolDefinition def = toolDefinitions.get(name);
            if (def != null) {
                definitions.add(def);
            }
        }
        return definitions;
    }

    /**
     * 检查函数是否存在
     */
    public boolean hasFunction(String functionName) {
        return functions.containsKey(functionName);
    }

    /**
     * 获取所有函数名
     */
    public Set<String> getAllFunctionNames() {
        return functions.keySet();
    }

    // 辅助方法：安全获取参数
    @SuppressWarnings("unchecked")
    private <T> T get(Map<String, Object> params, String key, T defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object value = params.get(key);
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    private String getString(Map<String, Object> params, String key) {
        return getString(params, key, null);
    }

    private String getString(Map<String, Object> params, String key, String defaultValue) {
        Object value = get(params, key, defaultValue);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = get(params, key, null);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double getDouble(Map<String, Object> params, String key, double defaultValue) {
        Object value = get(params, key, null);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = get(params, key, null);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getList(Map<String, Object> params, String key) {
        Object value = get(params, key, null);
        if (value instanceof List) {
            return (List<T>) value;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> getMap(Map<String, Object> params, String key) {
        Object value = get(params, key, null);
        if (value instanceof Map) {
            return (Map<K, V>) value;
        }
        return Collections.emptyMap();
    }

    /**
     * 创建输入模式
     */
    private Map<String, Object> createInputSchema(String functionName) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        switch (functionName) {
            case "query_profit_metrics":
                properties.put("dimensions", createArrayProperty("维度列表"));
                properties.put("metrics", createArrayProperty("指标列表"));
                properties.put("period", createStringProperty("期间"));
                properties.put("filters", createObjectProperty("过滤条件"));
                required.add("dimensions");
                required.add("metrics");
                required.add("period");
                break;

            case "drill_down_detail":
                properties.put("parent_dim", createStringProperty("父维度"));
                properties.put("parent_value", createStringProperty("父维度值"));
                properties.put("child_dim", createStringProperty("子维度"));
                properties.put("period", createStringProperty("期间"));
                required.add("parent_dim");
                required.add("parent_value");
                required.add("child_dim");
                required.add("period");
                break;

            case "query_period_compare":
                properties.put("dim_type", createStringProperty("维度类型"));
                properties.put("dim_value", createStringProperty("维度值"));
                properties.put("current_period", createStringProperty("当期"));
                properties.put("compare_type", createStringProperty("对比类型"));
                required.add("dim_type");
                required.add("dim_value");
                required.add("current_period");
                required.add("compare_type");
                break;

            case "query_master_data":
                properties.put("dim_type", createStringProperty("维度类型"));
                properties.put("parent_code", createStringProperty("父编码"));
                required.add("dim_type");
                break;

            case "query_indicator_fact":
                properties.put("period", createStringProperty("期间"));
                properties.put("dimension_filters", createObjectProperty("维度过滤"));
                properties.put("page", createIntegerProperty("页码"));
                properties.put("size", createIntegerProperty("每页大小"));
                required.add("period");
                break;

            case "execute_allocation":
                properties.put("rule_code", createStringProperty("规则编码"));
                properties.put("period", createStringProperty("期间"));
                properties.put("dry_run", createBooleanProperty("是否预览"));
                required.add("rule_code");
                required.add("period");
                break;

            case "calculate_contribution":
                properties.put("target_metric", createStringProperty("目标指标"));
                properties.put("dimension", createStringProperty("维度"));
                properties.put("base_period", createStringProperty("基期"));
                properties.put("current_period", createStringProperty("当期"));
                required.add("target_metric");
                required.add("dimension");
                required.add("base_period");
                required.add("current_period");
                break;

            case "analyze_sensitivity":
                properties.put("factors", createArrayProperty("因素列表"));
                properties.put("change_range", createNumberProperty("变化范围"));
                required.add("factors");
                required.add("change_range");
                break;

            case "diagnose_allocation_rules":
                properties.put("period", createStringProperty("期间"));
                required.add("period");
                break;

            case "get_factor_data":
                properties.put("factor_code", createStringProperty("因子编码"));
                properties.put("period", createStringProperty("期间"));
                properties.put("dim_type", createStringProperty("维度类型"));
                required.add("factor_code");
                required.add("period");
                required.add("dim_type");
                break;

            case "generate_business_brief":
                properties.put("period", createStringProperty("期间"));
                properties.put("scope", createStringProperty("范围"));
                properties.put("format", createStringProperty("格式"));
                required.add("period");
                break;

            case "generate_excel_report":
                properties.put("template_id", createStringProperty("模板ID"));
                properties.put("period", createStringProperty("期间"));
                properties.put("filters", createObjectProperty("过滤条件"));
                required.add("template_id");
                required.add("period");
                break;

            case "generate_chart":
                properties.put("chart_type", createStringProperty("图表类型"));
                properties.put("data", createArrayProperty("数据"));
                properties.put("options", createObjectProperty("选项"));
                required.add("chart_type");
                required.add("data");
                break;

            case "trigger_alert":
                properties.put("metric", createStringProperty("指标"));
                properties.put("threshold", createNumberProperty("阈值"));
                properties.put("receiver", createStringProperty("接收人"));
                properties.put("message", createStringProperty("消息"));
                required.add("metric");
                required.add("threshold");
                required.add("receiver");
                required.add("message");
                break;

            case "scan_data_quality":
                properties.put("period", createStringProperty("期间"));
                properties.put("dim_type", createStringProperty("维度类型"));
                required.add("period");
                break;

            case "detect_anomaly":
                properties.put("period", createStringProperty("期间"));
                properties.put("metrics", createArrayProperty("指标列表"));
                properties.put("threshold", createNumberProperty("阈值"));
                required.add("period");
                required.add("metrics");
                required.add("threshold");
                break;

            case "validate_data":
                properties.put("table", createStringProperty("表名"));
                properties.put("period", createStringProperty("期间"));
                properties.put("rules", createArrayProperty("规则列表"));
                required.add("table");
                required.add("period");
                required.add("rules");
                break;

            case "get_governance_issues":
                properties.put("period", createStringProperty("期间"));
                properties.put("level", createStringProperty("级别"));
                required.add("period");
                break;

            case "detect_file_format":
                properties.put("file_path", createStringProperty("文件路径"));
                required.add("file_path");
                break;

            case "map_fields":
                properties.put("source_fields", createArrayProperty("源字段列表"));
                properties.put("target_table", createStringProperty("目标表"));
                required.add("source_fields");
                required.add("target_table");
                break;

            case "clean_data":
                properties.put("data", createArrayProperty("数据"));
                properties.put("rules", createArrayProperty("规则列表"));
                required.add("data");
                required.add("rules");
                break;

            case "link_dimensions":
                properties.put("data", createArrayProperty("数据"));
                properties.put("dim_mappings", createObjectProperty("维度映射"));
                required.add("data");
                required.add("dim_mappings");
                break;

            case "import_data":
                properties.put("data", createArrayProperty("数据"));
                properties.put("target_table", createStringProperty("目标表"));
                properties.put("mode", createStringProperty("模式"));
                required.add("data");
                required.add("target_table");
                required.add("mode");
                break;

            case "generate_quality_report":
                properties.put("import_id", createStringProperty("导入ID"));
                required.add("import_id");
                break;
        }

        schema.put("properties", properties);
        schema.put("required", required);

        return schema;
    }

    private Map<String, Object> createStringProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "string");
        prop.put("description", description);
        return prop;
    }

    private Map<String, Object> createIntegerProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "integer");
        prop.put("description", description);
        return prop;
    }

    private Map<String, Object> createNumberProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "number");
        prop.put("description", description);
        return prop;
    }

    private Map<String, Object> createBooleanProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "boolean");
        prop.put("description", description);
        return prop;
    }

    private Map<String, Object> createArrayProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "array");
        prop.put("description", description);
        return prop;
    }

    private Map<String, Object> createObjectProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "object");
        prop.put("description", description);
        return prop;
    }
}
