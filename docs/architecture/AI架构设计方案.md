# 多维盈利分析系统 AI 架构设计方案

## 一、整体架构概览

### 1.1 五层架构图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ Layer 5: 交互入口层                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                    AI对话助手（底部统一入口）                             │   │
│  │                    卡片式对话 + 工作流展示                               │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ Layer 4: Agent智能执行层（5个）                                                  │
│  配置方式：MD文件定义prompt、触发词、工具                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 📥 数据接入  │  │ 🔍 专项分析  │  │ 💰 费用分摊  │  │ ⚠️ 风险预警  │       │
│  │  agent.md    │  │  agent.md    │  │  agent.md    │  │  agent.md    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘       │
│                              ┌──────────────┐                                  │
│                              │ 💬 智能助手  │                                  │
│                              │  agent.md    │                                  │
│                              └──────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ Layer 3: Skills业务技能层（8个）                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 利润波动归因 │  │ 全维度亏损   │  │ 经营报告生成 │  │ 盈利情景模拟 │       │
│  │ Skill        │  │ 扫描 Skill   │  │ Skill        │  │ Skill        │       │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 客户盈利分层 │  │ 分摊规则诊断 │  │ 异常检测诊断 │  │ 成本优化建议 │       │
│  │ Skill        │  │ Skill        │  │ Skill        │  │ Skill        │       │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ Layer 2: Function Call原子工具层（27个）                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 数据查询类   │  │ 计算分析类   │  │ 操作输出类   │  │ 数据集成类   │       │
│  │ 8个函数      │  │ 7个函数      │  │ 6个函数      │  │ 6个函数      │       │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ Layer 1: MCP Server能力总线层（5个）                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 业财数据     │  │ 分析算法     │  │ 报表输出     │  │ 数据治理     │       │
│  │ MCP Server   │  │ MCP Server   │  │ MCP Server   │  │ MCP Server   │       │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘       │
│                              ┌──────────────┐                                  │
│                              │ 数据集成     │                                  │
│                              │ MCP Server   │                                  │
│                              └──────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ Layer 0: 数据源层                                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ MySQL        │  │ 核心系统     │  │ HR系统       │  │ 财务系统     │       │
│  │ (biz_ledger) │  │ (业务数据)   │  │ (员工数据)   │  │ (费用数据)   │       │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 技术栈

| 技术领域 | 技术选型 | 版本 | 用途 |
|---------|---------|------|------|
| LLM大模型 | Claude API (Anthropic) | claude-sonnet-4 | 核心AI能力 |
| AI框架 | LangChain4j | 0.30.0 | Function Call、Agent编排 |
| 后端框架 | Spring Boot | 3.2.5 | 服务承载 |
| 数据库 | MySQL | 8.0 | 业务数据 |
| 缓存 | Redis | 7.x | 配置缓存 |
| 前端框架 | React | 18 | 用户界面 |
| 图表库 | ECharts | 5.x | 数据可视化 |

### 1.3 关键设计决策

| 决策项 | 方案 | 说明 |
|-------|------|------|
| **Agent配置** | MD文件 | 用Markdown定义prompt、触发词、工具，易维护 |
| **交互模式** | 卡片式对话 | 底部统一入口，Agent执行用工作流卡片展示 |
| **记忆系统** | 无长期记忆 | 企业系统多用户共用，不需要记忆历史 |
| **会话上下文** | 简单会话ID | 支持追问场景，30分钟过期 |
| **权限管控** | 用户上下文 | 每次请求带上用户角色和数据权限 |

---

## 二、MCP Server详细设计

### 2.1 MCP Server清单

| MCP Server | 职责 | 工具数量 |
|------------|------|----------|
| 业财数据MCP Server | 统一暴露所有业务数据查询能力 | 5个 |
| 分析算法MCP Server | 统一封装计算分析能力 | 5个 |
| 报表输出MCP Server | 统一封装报表生成能力 | 4个 |
| 数据治理MCP Server | 统一封装数据质量检测能力 | 4个 |
| 数据集成MCP Server | 统一封装数据接入能力 | 6个 |

### 2.2 业财数据 MCP Server

**职责**：统一暴露所有业务数据查询能力，AI无需感知底层表结构

```java
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
     */
    @McpTool(name = "query_profit_metrics",
             description = "按维度组合查询盈利指标（收入、成本、利润等）")
    public ProfitMetricsResult queryProfitMetrics(
            @Param("dimensions") List<String> dimensions,
            @Param("metrics") List<String> metrics,
            @Param("period") String period,
            @Param("filters") Map<String, String> filters) {
        String sql = buildMetricsQuery(dimensions, metrics, period, filters);
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
        return new ProfitMetricsResult(data, dimensions, metrics, period);
    }

    /**
     * MCP工具：维度下钻
     */
    @McpTool(name = "drill_down_detail",
             description = "维度下钻，从机构→部门→产品逐层穿透")
    public DrillDownResult drillDown(
            @Param("parent_dim") String parentDim,
            @Param("parent_value") String parentValue,
            @Param("child_dim") String childDim,
            @Param("period") String period) {
        return dimensionService.drillDown(parentDim, parentValue, childDim, period);
    }

    /**
     * MCP工具：同比环比分析
     */
    @McpTool(name = "query_period_compare",
             description = "自动计算同比、环比、预算达成率")
    public PeriodCompareResult queryPeriodCompare(
            @Param("dim_type") String dimType,
            @Param("dim_value") String dimValue,
            @Param("current_period") String currentPeriod,
            @Param("compare_type") String compareType) {
        return dashboardService.comparePeriod(dimType, dimValue, currentPeriod, compareType);
    }

    /**
     * MCP工具：查询主数据
     */
    @McpTool(name = "query_master_data",
             description = "查询主数据（机构、部门、产品、渠道、客户经理）")
    public List<DimensionMaster> queryMasterData(
            @Param("dim_type") String dimType,
            @Param("parent_code") String parentCode) {
        return dimensionService.getMasterData(dimType, parentCode);
    }

    /**
     * MCP工具：查询业务台账
     */
    @McpTool(name = "query_biz_ledger",
             description = "查询业务台账明细数据")
    public LedgerResult queryBizLedger(
            @Param("period") String period,
            @Param("dimensions") Map<String, String> dimensionFilters,
            @Param("page") int page,
            @Param("size") int size) {
        return dashboardService.queryLedger(period, dimensionFilters, page, size);
    }
}
```

### 2.3 分析算法 MCP Server

**职责**：统一封装所有计算分析能力，保证算法口径一致

```java
@Component
public class AnalysisMcpServer {

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private AllocationConfigService configService;

    /**
     * MCP工具：执行费用分摊
     */
    @McpTool(name = "execute_allocation",
             description = "按规则执行费用分摊计算")
    public AllocationResult executeAllocation(
            @Param("rule_code") String ruleCode,
            @Param("period") String period,
            @Param("dry_run") boolean dryRun) {
        if (dryRun) {
            return allocationService.previewAllocation(ruleCode, period);
        }
        return allocationService.executeAllocation(ruleCode, period);
    }

    /**
     * MCP工具：计算维度贡献度
     */
    @McpTool(name = "calculate_contribution",
             description = "计算各维度对利润波动的贡献度")
    public ContributionResult calculateContribution(
            @Param("target_metric") String targetMetric,
            @Param("dimension") String dimension,
            @Param("base_period") String basePeriod,
            @Param("current_period") String currentPeriod) {
        return analysisService.calculateContribution(
            targetMetric, dimension, basePeriod, currentPeriod);
    }

    /**
     * MCP工具：盈利敏感性分析
     */
    @McpTool(name = "analyze_sensitivity",
             description = "单因素/多因素盈利敏感性测算")
    public SensitivityResult analyzeSensitivity(
            @Param("factors") List<SensitivityFactor> factors,
            @Param("change_range") double changeRange) {
        return analysisService.analyzeSensitivity(factors, changeRange);
    }

    /**
     * MCP工具：分摊规则诊断
     */
    @McpTool(name = "diagnose_allocation_rules",
             description = "诊断分摊规则配置，检查冲突和遗漏")
    public DiagnosisResult diagnoseRules(
            @Param("period") String period) {
        List<AllocationRuleConfig> rules = configService.listRules(null, null);
        return diagnosisService.diagnose(rules, period);
    }

    /**
     * MCP工具：获取分摊因子数据
     */
    @McpTool(name = "get_factor_data",
             description = "获取分摊因子的数值数据")
    public FactorDataResult getFactorData(
            @Param("factor_code") String factorCode,
            @Param("period") String period,
            @Param("dim_type") String dimType) {
        return allocationService.getFactorData(factorCode, period, dimType);
    }
}
```

### 2.4 报表输出 MCP Server

**职责**：统一封装所有报表生成和输出能力

```java
@Component
public class ReportMcpServer {

    @Autowired
    private ReportGenerator reportGenerator;

    @Autowired
    private ExportService exportService;

    /**
     * MCP工具：生成经营简报
     */
    @McpTool(name = "generate_business_brief",
             description = "生成月度/季度经营分析简报")
    public BriefResult generateBrief(
            @Param("period") String period,
            @Param("scope") String scope,
            @Param("format") String format) {
        return reportService.generateBrief(period, scope, format);
    }

    /**
     * MCP工具：生成Excel报表
     */
    @McpTool(name = "generate_excel_report",
             description = "按模板生成Excel报表")
    public byte[] generateExcel(
            @Param("template_id") String templateId,
            @Param("period") String period,
            @Param("filters") Map<String, String> filters) {
        return exportService.generateExcel(templateId, period, filters);
    }

    /**
     * MCP工具：生成图表
     */
    @McpTool(name = "generate_chart",
             description = "生成ECharts图表配置")
    public ChartConfig generateChart(
            @Param("chart_type") String chartType,
            @Param("data") List<Map<String, Object>> data,
            @Param("options") Map<String, Object> options) {
        return chartService.generateChart(chartType, data, options);
    }

    /**
     * MCP工具：触发预警推送
     */
    @McpTool(name = "trigger_alert",
             description = "触发指标异常预警推送")
    public AlertResult triggerAlert(
            @Param("metric") String metric,
            @Param("threshold") double threshold,
            @Param("receiver") String receiver,
            @Param("message") String message) {
        return alertService.trigger(metric, threshold, receiver, message);
    }
}
```

### 2.5 数据治理 MCP Server

**职责**：统一封装数据质量检测和治理能力

```java
@Component
public class GovernanceMcpServer {

    @Autowired
    private DataGovernanceService governanceService;

    /**
     * MCP工具：数据质量扫描
     */
    @McpTool(name = "scan_data_quality",
             description = "扫描数据质量，返回完整性、一致性、准确性评分")
    public QualityReport scanQuality(
            @Param("period") String period,
            @Param("dim_type") String dimType) {
        return governanceService.scan(period, dimType);
    }

    /**
     * MCP工具：异常检测
     */
    @McpTool(name = "detect_anomaly",
             description = "检测指标异常，返回异常清单")
    public List<AnomalyResult> detectAnomaly(
            @Param("period") String period,
            @Param("metrics") List<String> metrics,
            @Param("threshold") double threshold) {
        return governanceService.detectAnomaly(period, metrics, threshold);
    }

    /**
     * MCP工具：数据校验
     */
    @McpTool(name = "validate_data",
             description = "执行数据校验规则")
    public ValidationResult validateData(
            @Param("table") String table,
            @Param("period") String period,
            @Param("rules") List<String> rules) {
        return validationService.validate(table, period, rules);
    }

    /**
     * MCP工具：获取治理问题清单
     */
    @McpTool(name = "get_governance_issues",
             description = "获取数据治理问题清单")
    public List<GovernanceIssue> getIssues(
            @Param("period") String period,
            @Param("level") String level) {
        return governanceService.getIssues(period, level);
    }
}
```

### 2.6 数据集成 MCP Server

**职责**：统一封装数据接入、清洗、转换能力

```java
@Component
public class DataIntegrationMcpServer {

    /**
     * MCP工具：检测文件格式
     */
    @McpTool(name = "detect_file_format",
             description = "检测上传文件的格式、编码、字段")
    public FileFormatResult detectFormat(
            @Param("file_path") String filePath) {
        return fileService.detectFormat(filePath);
    }

    /**
     * MCP工具：字段映射
     */
    @McpTool(name = "map_fields",
             description = "将源字段映射到目标表字段")
    public FieldMappingResult mapFields(
            @Param("source_fields") List<String> sourceFields,
            @Param("target_table") String targetTable) {
        return mappingService.mapFields(sourceFields, targetTable);
    }

    /**
     * MCP工具：数据清洗
     */
    @McpTool(name = "clean_data",
             description = "执行数据清洗规则")
    public CleanResult cleanData(
            @Param("data") List<Map<String, Object>> data,
            @Param("rules") List<String> rules) {
        return cleanService.clean(data, rules);
    }

    /**
     * MCP工具：维度关联
     */
    @McpTool(name = "link_dimensions",
             description = "将维度编码关联到dimension_master")
    public LinkResult linkDimensions(
            @Param("data") List<Map<String, Object>> data,
            @Param("dim_mappings") Map<String, String> dimMappings) {
        return dimensionService.link(data, dimMappings);
    }

    /**
     * MCP工具：数据入库
     */
    @McpTool(name = "import_data",
             description = "将清洗后的数据写入目标表")
    public ImportResult importData(
            @Param("data") List<Map<String, Object>> data,
            @Param("target_table") String targetTable,
            @Param("mode") String mode) {
        return importService.importData(data, targetTable, mode);
    }

    /**
     * MCP工具：生成质量报告
     */
    @McpTool(name = "generate_quality_report",
             description = "生成数据质量报告")
    public QualityReport generateReport(
            @Param("import_id") String importId) {
        return reportService.generateQualityReport(importId);
    }
}
```

---

## 三、Function Call详细设计

### 3.1 函数清单（27个）

#### 数据查询类（8个）

| 函数名 | 说明 | MCP来源 |
|--------|------|---------|
| `query_profit_metrics` | 按维度查询盈利指标 | 业财数据MCP |
| `drill_down_detail` | 维度下钻穿透 | 业财数据MCP |
| `query_period_compare` | 同比环比分析 | 业财数据MCP |
| `query_master_data` | 主数据查询 | 业财数据MCP |
| `query_biz_ledger` | 业务台账查询 | 业财数据MCP |
| `get_allocation_rules` | 分摊规则查询 | 分析算法MCP |
| `get_factor_data` | 因子数据查询 | 分析算法MCP |
| `get_governance_issues` | 治理问题查询 | 数据治理MCP |

#### 计算分析类（7个）

| 函数名 | 说明 | MCP来源 |
|--------|------|---------|
| `execute_allocation` | 执行费用分摊 | 分析算法MCP |
| `calculate_contribution` | 维度贡献度计算 | 分析算法MCP |
| `analyze_sensitivity` | 敏感性分析 | 分析算法MCP |
| `diagnose_allocation_rules` | 规则诊断 | 分析算法MCP |
| `suggest_allocation_rule` | 规则推荐 | 分析算法MCP |
| `scan_data_quality` | 数据质量扫描 | 数据治理MCP |
| `detect_anomaly` | 异常检测 | 数据治理MCP |

#### 操作输出类（6个）

| 函数名 | 说明 | MCP来源 |
|--------|------|---------|
| `generate_business_brief` | 生成经营简报 | 报表输出MCP |
| `generate_excel_report` | 生成Excel报表 | 报表输出MCP |
| `generate_chart` | 生成图表 | 报表输出MCP |
| `trigger_alert` | 触发预警推送 | 报表输出MCP |
| `validate_data` | 数据校验 | 数据治理MCP |
| `interpret_data` | 数据解读 | 业财数据MCP |

#### 数据集成类（6个）

| 函数名 | 说明 | MCP来源 |
|--------|------|---------|
| `detect_file_format` | 检测文件格式 | 数据集成MCP |
| `map_fields` | 字段映射 | 数据集成MCP |
| `clean_data` | 数据清洗 | 数据集成MCP |
| `link_dimensions` | 维度关联 | 数据集成MCP |
| `import_data` | 数据入库 | 数据集成MCP |
| `generate_quality_report` | 生成质量报告 | 数据集成MCP |

### 3.2 Function Call客户端实现

```java
@Component
public class ClaudeFunctionCallClient {

    @Value("${ai.anthropic.api-key:}")
    private String apiKey;

    @Value("${ai.anthropic.base-url:https://api.anthropic.com}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 支持Function Call的对话
     */
    public FunctionCallResult chatWithFunctions(String systemPrompt, String userMessage,
                                                 List<ToolDefinition> tools) {
        // 1. 构建带tools的请求
        String requestBody = buildRequestWithTools(systemPrompt, userMessage, tools);

        // 2. 调用Claude API
        HttpResponse<String> response = callApi(requestBody);

        // 3. 解析响应，判断是否需要调用函数
        ClaudeResponse claudeResponse = parseResponse(response.body());

        // 4. 如果是function_call，执行函数并递归调用
        if (claudeResponse.getStopReason().equals("tool_use")) {
            ToolUseBlock toolUse = claudeResponse.getToolUse();
            Object functionResult = executeFunction(toolUse.getName(), toolUse.getInput());

            // 5. 将函数结果返回给Claude继续对话
            return continueWithFunctionResult(systemPrompt, userMessage,
                toolUse, functionResult, tools);
        }

        return new FunctionCallResult(claudeResponse.getText(), null);
    }

    private String buildRequestWithTools(String systemPrompt, String userMessage,
                                          List<ToolDefinition> tools) {
        return """
        {
            "model": "claude-sonnet-4-20250514",
            "max_tokens": 4096,
            "system": "%s",
            "tools": %s,
            "messages": [{"role": "user", "content": "%s"}]
        }
        """.formatted(
            escapeJson(systemPrompt),
            objectMapper.writeValueAsString(tools),
            escapeJson(userMessage)
        );
    }
}
```

### 3.3 函数注册中心

```java
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

    @PostConstruct
    public void init() {
        // 业财数据MCP工具
        register("query_profit_metrics", bizDataMcp::queryProfitMetrics);
        register("drill_down_detail", bizDataMcp::drillDown);
        register("query_period_compare", bizDataMcp::queryPeriodCompare);
        register("query_master_data", bizDataMcp::queryMasterData);
        register("query_biz_ledger", bizDataMcp::queryBizLedger);

        // 分析算法MCP工具
        register("execute_allocation", analysisMcp::executeAllocation);
        register("calculate_contribution", analysisMcp::calculateContribution);
        register("analyze_sensitivity", analysisMcp::analyzeSensitivity);
        register("diagnose_allocation_rules", analysisMcp::diagnoseRules);
        register("get_factor_data", analysisMcp::getFactorData);

        // 报表输出MCP工具
        register("generate_business_brief", reportMcp::generateBrief);
        register("generate_excel_report", reportMcp::generateExcel);
        register("generate_chart", reportMcp::generateChart);
        register("trigger_alert", reportMcp::triggerAlert);

        // 数据治理MCP工具
        register("scan_data_quality", governanceMcp::scanQuality);
        register("detect_anomaly", governanceMcp::detectAnomaly);
        register("validate_data", governanceMcp::validateData);
        register("get_governance_issues", governanceMcp::getIssues);

        // 数据集成MCP工具
        register("detect_file_format", integrationMcp::detectFormat);
        register("map_fields", integrationMcp::mapFields);
        register("clean_data", integrationMcp::cleanData);
        register("link_dimensions", integrationMcp::linkDimensions);
        register("import_data", integrationMcp::importData);
        register("generate_quality_report", integrationMcp::generateReport);
    }

    public Object execute(String functionName, Map<String, Object> params) {
        return functions.get(functionName).apply(params);
    }
}
```

---

## 四、Skills详细设计

### 4.1 Skill清单（8个）

| 技能 | 触发词 | 调用的Function | 输出 |
|------|--------|----------------|------|
| **利润波动归因** | "利润变化"、"波动分析" | query_profit_metrics, calculate_contribution | 归因报告 |
| **全维度亏损扫描** | "亏损"、"低利" | query_profit_metrics, drill_down_detail | 亏损清单 |
| **经营报告生成** | "简报"、"报告" | query_profit_metrics, generate_business_brief | 完整报告 |
| **盈利情景模拟** | "如果"、"假设" | analyze_sensitivity | 模拟结果 |
| **客户盈利分层** | "客户分层"、"客户价值" | query_profit_metrics, drill_down_detail | 分层清单 |
| **分摊规则诊断** | "规则检查"、"分摊诊断" | diagnose_allocation_rules | 诊断报告 |
| **异常检测诊断** | "异常"、"预警" | detect_anomaly, interpret_data | 异常分析 |
| **成本优化建议** | "优化"、"降本" | query_profit_metrics, analyze_sensitivity | 优化建议 |

### 4.2 Skill接口定义

```java
public interface Skill {
    String getName();
    String getDescription();
    List<String> getTriggers();
    SkillResult execute(SkillContext context);
    boolean canHandle(String userMessage);
}

@Data
public class SkillContext {
    private String userMessage;
    private String period;
    private String dimType;
    private UserContext userContext;  // 用户权限上下文
    private Map<String, Object> params;
}

@Data
@AllArgsConstructor
public class SkillResult {
    private String answer;
    private Map<String, Object> data;
    private String chartType;
    private List<String> suggestions;
}
```

### 4.3 Skill实现示例：利润波动归因

```java
@Component
public class ProfitVarianceSkill implements Skill {

    @Autowired
    private McpFunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "profit-variance";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("利润变化", "波动分析", "利润归因", "为什么利润");
    }

    @Override
    public boolean canHandle(String userMessage) {
        return getTriggers().stream()
            .anyMatch(trigger -> userMessage.contains(trigger));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        String period = context.getPeriod();
        String basePeriod = context.getParam("base_period");

        // Step 1: 获取当期和基期数据
        ProfitMetricsResult currentData = (ProfitMetricsResult) functionRegistry
            .execute("query_profit_metrics", Map.of(
                "dimensions", Arrays.asList("ORG", "PRODUCT"),
                "metrics", Arrays.asList("REVENUE", "COST", "PROFIT"),
                "period", period
            ));

        ProfitMetricsResult baseData = (ProfitMetricsResult) functionRegistry
            .execute("query_profit_metrics", Map.of(
                "dimensions", Arrays.asList("ORG", "PRODUCT"),
                "metrics", Arrays.asList("REVENUE", "COST", "PROFIT"),
                "period", basePeriod
            ));

        // Step 2: 计算各维度贡献度
        ContributionResult contribution = (ContributionResult) functionRegistry
            .execute("calculate_contribution", Map.of(
                "target_metric", "NET_PROFIT",
                "dimension", "ORG",
                "base_period", basePeriod,
                "current_period", period
            ));

        // Step 3: AI生成归因分析
        String analysis = generateAnalysis(currentData, baseData, contribution);

        // Step 4: 生成图表
        ChartConfig chart = (ChartConfig) functionRegistry
            .execute("generate_chart", Map.of(
                "chart_type", "waterfall",
                "data", contribution.toChartData()
            ));

        return new SkillResult(analysis, Map.of(
            "current", currentData,
            "base", baseData,
            "contribution", contribution,
            "chart", chart
        ));
    }
}
```

### 4.4 Skill注册中心

```java
@Component
public class SkillRegistry {

    @Autowired
    private List<Skill> skills;

    /**
     * 根据用户消息匹配技能
     */
    public Skill matchSkill(String userMessage) {
        return skills.stream()
            .filter(skill -> skill.canHandle(userMessage))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有可用技能
     */
    public List<Skill> getAllSkills() {
        return skills;
    }
}
```

---

## 五、Agent详细设计（MD配置方式）

### 5.1 Agent配置文件位置

```
后端/src/main/resources/
└── agents/
    ├── data-ingestion.md      # 数据接入Agent
    ├── deep-analysis.md       # 专项分析Agent
    ├── allocation.md          # 费用分摊Agent
    ├── risk-alert.md          # 风险预警Agent
    └── smart-assistant.md     # 智能助手Agent
```

### 5.2 MD文件格式规范

```markdown
---
name: Agent名称
icon: 图标
description: 描述
triggers:
  - 触发词1
  - 触发词2
tools:
  - 工具1
  - 工具2
max_iterations: 10
---

# 系统提示词

Agent的系统提示词内容...
```

### 5.3 数据接入Agent（📥）

**配置文件**：`data-ingestion.md`

```markdown
---
name: 数据接入Agent
icon: 📥
description: 自动化数据导入、清洗、转换、校验全流程
triggers:
  - 导入
  - 上传
  - 同步
  - 接入
  - ETL
  - 批量导入
tools:
  - detect_file_format
  - map_fields
  - clean_data
  - validate_data
  - link_dimensions
  - import_data
  - generate_quality_report
max_iterations: 15
---

# 系统提示词

你是数据接入Agent，负责自动化数据导入全流程。

## 工作流程

1. **数据源识别**：识别数据格式（CSV/Excel/API/数据库）
2. **字段映射**：将源字段映射到目标表字段
3. **数据清洗**：处理空值、异常值、格式转换
4. **维度关联**：将维度编码关联到dimension_master
5. **数据校验**：执行格式校验、逻辑校验、平衡校验
6. **异常诊断**：对校验失败的数据进行AI诊断
7. **数据入库**：将清洗后的数据写入目标表
8. **质量报告**：生成数据质量报告

## 可用工具

- `detect_file_format`: 检测文件格式和字段
- `map_fields`: 字段映射配置
- `clean_data`: 数据清洗
- `validate_data`: 数据校验
- `link_dimensions`: 维度关联
- `import_data`: 数据入库
- `generate_quality_report`: 生成质量报告

## 输出格式

```
📊 数据接入质量报告

【导入概况】
• 源文件：xxx.csv
• 总记录数：100,000条
• 成功导入：98,500条（98.5%）
• 失败记录：1,500条（1.5%）

【数据清洗】
• 空值填充：2,340处
• 异常值标记：156条
• 格式转换：日期、金额标准化

【校验失败分析】
1. 维度编码不存在：800条
2. 金额不平衡：500条
3. 日期异常：200条

【建议操作】
1. 更新维度主数据后再重新导入失败记录
2. 与核心系统确认金额不平衡数据
```
```

### 5.4 专项分析Agent（🔍）

**配置文件**：`deep-analysis.md`

```markdown
---
name: 专项分析Agent
icon: 🔍
description: 处理复杂的开放式分析问题
triggers:
  - 为什么
  - 归因
  - 分析
  - 客户价值
  - 产品盈利
  - 渠道效能
  - 预算达成
  - 趋势分析
  - 对比分析
tools:
  - query_profit_metrics
  - drill_down_detail
  - query_period_compare
  - calculate_contribution
  - analyze_sensitivity
  - generate_chart
max_iterations: 10
---

# 系统提示词

你是专项分析Agent，负责处理复杂的开放式分析问题。

## 分析方法论

1. **目标拆解**：明确分析目标，确定分析框架（收入端/成本端/费用端）
2. **路径规划**：先宏观定位异常维度，再逐层下钻至细粒度
3. **交叉验证**：从业务动因和财务结果双向验证，排除偶发因素
4. **结论输出**：汇总根因，附上数据依据和可落地建议

## 分析场景

### 利润归因分析
- 查询整体利润数据
- 按维度拆解（机构/产品/条线）
- 定位异常维度
- 下钻分析根因
- 生成归因报告

### 客户价值分析
- 查询客户维度数据
- 计算客户贡献度
- 客户分层（高/中/低价值）
- 分析客户特征
- 生成客户画像

### 产品盈利分析
- 查询产品维度数据
- 产品盈利排名
- 产品趋势分析
- 产品结构分析
- 生成优化建议

### 渠道效能分析
- 查询渠道维度数据
- 渠道ROI计算
- 渠道效率排名
- 渠道优化建议

### 预算对比分析
- 查询实际数据
- 查询预算数据
- 计算达成率
- 分析偏差原因
- 生成纠偏建议

## 输出格式

```
📊 [分析主题]

【分析摘要】
一句话概括核心结论

【详细分析】
1. [维度1]
   • 指标值
   • 分析结论

2. [维度2]
   • 指标值
   • 分析结论

【根因定位】
• 根因1
• 根因2

【建议】
• 建议1
• 建议2
• 建议3

📈 [图表]
```
```

### 5.5 费用分摊Agent（💰）

**配置文件**：`allocation.md`

```markdown
---
name: 费用分摊Agent
icon: 💰
description: 银行运营费用的分摊管理
triggers:
  - 分摊
  - 费用分摊
  - 成本分摊
  - 分摊规则
  - 分摊因子
  - 运营费用
  - 房租
  - 水电
  - 催收
tools:
  - get_allocation_rules
  - get_factor_data
  - suggest_allocation_rule
  - diagnose_allocation_rules
  - execute_allocation
  - analyze_allocation_result
  - query_profit_metrics
max_iterations: 10
---

# 系统提示词

你是费用分摊Agent，专门负责银行运营费用的分摊管理。

## 业务背景

- 银行有30+种运营费用，每种费用的分摊规则不同
- 分摊目标：将公共费用合理分摊到机构/部门/产品/客户经理
- 分摊原则：谁受益谁承担、公平合理、口径统一

## 核心能力

### 1. 规则推荐
根据费用特性推荐最优分摊方案：
- 分析费用性质（固定/变动、直接/间接）
- 选择合适的分摊因子
- 推荐分摊算法
- 生成规则配置

### 2. 规则诊断
检查规则配置的完整性、冲突、合理性：
- 覆盖度检查：是否所有费用类型都有规则
- 冲突检查：是否存在重复或矛盾的规则
- 合理性检查：因子选择是否合适

### 3. 分摊执行
按规则执行费用分摊计算：
- 获取分摊规则
- 获取因子数据
- 执行分摊算法
- 生成分摊明细

### 4. 结果分析
分析分摊结果的合理性：
- 按维度汇总分析
- 识别异常分摊
- 分析偏差原因

### 5. 优化建议
基于历史数据推荐优化方案：
- 识别分摊不合理的费用
- 推荐更优的分摊因子
- 建议算法调整

## 费用分类

### 运营费用
| 费用类型 | 推荐因子 | 推荐算法 |
|---------|----------|----------|
| 房租物业 | 工位面积 | RATIO |
| 水电费 | 人数 | RATIO |
| 工位费 | 工位数 | RATIO |
| 报销费用 | 部门 | DIRECT |
| 催收费用 | 逾期业务量 | RATIO |
| 数据使用费 | 系统用户数 | RATIO |
| IT运维费 | 设备数 | RATIO |
| 营销费用 | 业务量 | WEIGHTED |
| 培训费用 | 参训人数 | RATIO |
| 行政办公 | 人数 | RATIO |

### 人力成本
| 费用类型 | 推荐因子 | 推荐算法 |
|---------|----------|----------|
| 工资 | 人数+薪资权重 | WEIGHTED |
| 社保 | 人数 | RATIO |
| 福利 | 人数 | RATIO |

### 管理费用
| 费用类型 | 推荐因子 | 推荐算法 |
|---------|----------|----------|
| 差旅 | 部门 | DIRECT |
| 招待 | 业务量 | RATIO |
| 物流 | 业务量 | RATIO |

## 分摊算法

- **RATIO**: 比例分摊（按单一因子占比）
- **WEIGHTED**: 加权分摊（按多因子加权）
- **STEP**: 阶梯分摊（按层级逐级）
- **DIRECT**: 直接归属（可直接归因）
- **FORMULA**: 公式分摊（自定义公式）

## 输出格式

### 规则推荐
```
📋 推荐方案

【费用类型】房租物业 (RENT)

【推荐规则】
• 分摊因子：工位面积 (AREA)
• 分摊算法：RATIO (比例分摊)
• 分摊方向：总行 → 各分行
• 执行周期：月度

【推荐理由】
• 房租与办公面积直接相关，按面积分摊最公平
• RATIO算法简单清晰，易于理解和复核
• 月度执行符合财务核算周期

【配置建议】
• 确保各分行工位面积数据准确
• 新增分行时及时更新面积数据
• 定期复核面积数据与实际是否一致
```

### 规则诊断
```
📋 诊断报告

【整体评分】75分

【问题1】规则覆盖度不足 (严重)
• 14种费用类型未配置分摊规则
• 催收费用、数据使用费、IT运维费等

【问题2】规则冲突 (警告)
• 营销费用同时配置了RATIO和WEIGHTED规则

【问题3】因子数据缺失 (警告)
• 工位面积数据：深圳分行、杭州分行缺失

【优化建议】
1. 优先补充14种未配置规则
2. 删除冲突的重复规则
3. 限期补充缺失的因子数据
```
```

### 5.6 风险预警Agent（⚠️）

**配置文件**：`risk-alert.md`

```markdown
---
name: 风险预警Agent
icon: ⚠️
description: 风险指标的持续监控
triggers:
  - 风险
  - 预警
  - 异常检测
  - 巡检
  - 监控
tools:
  - query_profit_metrics
  - detect_anomaly
  - drill_down_detail
  - trigger_alert
  - generate_report
max_iterations: 8
---

# 系统提示词

你是风险预警Agent，负责风险指标的持续监控。

## 监控指标

### 核心盈利指标
1. **净利润异常波动**：环比变化 > 10%
2. **毛利率异常**：环比下降 > 5%
3. **成本收入比异常**：> 行业均值20%

### 风险指标
4. **不良率上升**：环比上升 > 0.5%
5. **拨备覆盖率下降**：环比下降 > 10%

### 集中度指标
6. **单一客户集中度**：> 30%
7. **单一产品集中度**：> 40%

## 处理流程

1. **定时巡检**：每天9点自动执行
2. **异常检测**：对比历史数据，识别异常
3. **根因分析**：发现异常后自动下钻分析
4. **预警推送**：生成预警单推送给责任人
5. **闭环跟踪**：跟踪异常改善情况

## 预警等级

| 等级 | 条件 | 处理方式 |
|------|------|----------|
| 🔴 严重 | 偏离>20% | 立即推送，24小时内响应 |
| 🟡 警告 | 偏离10-20% | 当日推送，3个工作日内响应 |
| 🟢 关注 | 偏离5-10% | 周报汇总，持续观察 |

## 输出格式

```
⚠️ 风险巡检报告

【巡检时间】2026-06-27 09:00
【巡检周期】2026年6月

【异常发现】
🔴 严重：深圳分行净利润环比-15%
   • 根因：A产品利润下降40%
   • 建议：立即排查A产品成本上涨原因

🟡 警告：杭州分行成本收入比上升至65%
   • 根因：营销费用同比增长30%
   • 建议：优化营销费用结构

🟢 关注：广州分行毛利率环比下降3%
   • 根因：存款利率上升导致FTP成本增加
   • 建议：关注利率走势，适时调整定价

【推送状态】
• 深圳分行预警已推送至分行行长
• 杭州分行预警已推送至财务总监

【历史跟踪】
• 上月深圳分行A产品利润异常：已改善
• 上月北京分行成本收入比异常：持续关注中
```
```

### 5.7 智能助手Agent（💬）

**配置文件**：`smart-assistant.md`

```markdown
---
name: 智能助手Agent
icon: 💬
description: 快速响应简单查询和报告生成
triggers:
  - 默认
tools:
  - query_profit_metrics
  - drill_down_detail
  - generate_business_brief
  - generate_excel_report
  - generate_chart
max_iterations: 5
---

# 系统提示词

你是智能助手Agent，负责快速响应用户的简单查询和报告生成。

## 核心能力

### 1. 快速数据查询
- 回答简单的指标查询
- 支持多维度数据查询
- 提供同比环比对比

### 2. NL2SQL转换
- 将自然语言转为SQL查询
- 智能理解用户意图
- 自动生成查询语句

### 3. 图表智能推荐
- 根据数据类型推荐图表
- 柱状图：排名对比
- 折线图：趋势分析
- 饼图：结构占比

### 4. 报告自动生成
- 生成月度经营简报
- 生成Excel报表
- 生成分析报告

## 响应要求

- 简单问题直接回答，不触发复杂流程
- 附带相关图表辅助理解
- 给出进一步分析的建议

## 输出格式

### 简单查询
```
📊 查询结果

本月收入：35亿元
环比增长：5.2%
同比增长：8.5%

💡 如需深入分析，可以问我：
• "各机构收入排名"
• "收入趋势分析"
• "收入结构分析"
```

### 报告生成
```
📋 2026年6月经营简报

一、整体经营情况
本月全行实现净利润12.84亿元，环比增长5.4%...

二、利润构成
• 业务总收入：35.6亿元
• FTP成本：12.8亿元
• 风险成本：5.2亿元
• 运营成本：4.8亿元

三、重点关注
1. 深圳分行对公不良率上升
2. 零售条线消费贷款增速放缓

四、经营建议
1. 加强对公贷款风险管控
2. 推动零售数字化转型
3. 优化FTP定价策略
```
```

### 5.8 Agent配置加载器

```java
@Component
public class AgentConfigLoader {

    @Value("${agent.config.path:classpath:agents/}")
    private String configPath;

    private Map<String, AgentConfig> configs = new HashMap<>();

    @PostConstruct
    public void loadConfigs() {
        // 加载所有MD配置文件
        File[] files = new File(configPath).listFiles((dir, name) -> name.endsWith(".md"));
        for (File file : files) {
            AgentConfig config = parseMarkdown(file);
            configs.put(config.getName(), config);
        }
    }

    private AgentConfig parseMarkdown(File file) {
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        // 解析YAML frontmatter
        String yaml = extractFrontmatter(content);
        AgentConfig config = parseYaml(yaml);

        // 提取系统提示词
        String systemPrompt = extractSystemPrompt(content);
        config.setSystemPrompt(systemPrompt);

        return config;
    }

    private String extractFrontmatter(String content) {
        // 提取 --- 之间的YAML内容
        int start = content.indexOf("---") + 3;
        int end = content.indexOf("---", start);
        return content.substring(start, end).trim();
    }

    private String extractSystemPrompt(String content) {
        // 提取 # 系统提示词 之后的内容
        int start = content.indexOf("# 系统提示词");
        return content.substring(start).trim();
    }

    public AgentConfig getConfig(String agentName) {
        return configs.get(agentName);
    }

    public List<AgentConfig> getAllConfigs() {
        return new ArrayList<>(configs.values());
    }
}
```

### 5.9 Agent配置模型

```java
@Data
public class AgentConfig {
    private String name;
    private String icon;
    private String description;
    private List<String> triggers;
    private List<String> tools;
    private int maxIterations;
    private String systemPrompt;
}
```

### 5.10 Agent执行引擎

```java
@Component
public class AgentExecutor {

    @Autowired
    private AgentConfigLoader configLoader;

    @Autowired
    private ClaudeFunctionCallClient claudeClient;

    @Autowired
    private FunctionRegistry functionRegistry;

    @Autowired
    private SessionContextCache sessionCache;

    /**
     * 执行Agent
     */
    public AgentResult execute(String agentName, String userMessage,
                                String sessionId, UserContext userContext) {
        // 1. 加载Agent配置
        AgentConfig config = configLoader.getConfig(agentName);

        // 2. 构建带权限的系统提示词
        String systemPrompt = buildPromptWithUserContext(
            config.getSystemPrompt(), userContext);

        // 3. 获取可用工具
        List<ToolDefinition> tools = getToolsForAgent(config.getTools());

        // 4. 获取会话上下文（支持追问）
        List<ChatMessage> history = sessionCache.getMessages(sessionId);

        // 5. 调用Claude执行
        FunctionCallResult result = claudeClient.chatWithFunctions(
            systemPrompt, userMessage, tools, history);

        // 6. 保存会话上下文
        sessionCache.addMessage(sessionId, new UserMessage(userMessage));
        sessionCache.addMessage(sessionId, new AssistantMessage(result.getText()));

        return new AgentResult(result.getText(), result.getFunctionResult());
    }

    private String buildPromptWithUserContext(String basePrompt, UserContext userContext) {
        return basePrompt + """

        ## 用户上下文

        - 用户ID：{userId}
        - 用户角色：{role}
        - 数据权限：
          - 可见机构：{visibleOrgs}
          - 可见产品：{visibleProducts}
          - 可见期间：{visiblePeriods}

        请注意：
        - 只展示用户权限范围内的数据
        - 如果用户查询超出权限范围，提示无权限
        """.formatted(
            userContext.getUserId(),
            userContext.getRole(),
            userContext.getVisibleOrgs(),
            userContext.getVisibleProducts(),
            userContext.getVisiblePeriods()
        );
    }
}
```

### 5.11 Agent路由（基于MD配置的triggers）

```java
@Component
public class AgentRouter {

    @Autowired
    private AgentConfigLoader configLoader;

    public String route(String userMessage) {
        String message = userMessage.toLowerCase();

        // 遍历所有Agent配置，匹配triggers
        for (AgentConfig config : configLoader.getAllConfigs()) {
            // 跳过默认Agent
            if (config.getTriggers().contains("默认")) {
                continue;
            }

            for (String trigger : config.getTriggers()) {
                if (message.contains(trigger)) {
                    return config.getName();
                }
            }
        }

        // 默认返回智能助手
        return "智能助手Agent";
    }
}
```

### 5.12 用户上下文模型

```java
@Data
public class UserContext {
    private String userId;
    private String userName;
    private String role;           // ADMIN, BRANCH_MANAGER, CUSTOMER_MANAGER, ANALYST
    private List<String> visibleOrgs;      // 可见机构列表
    private List<String> visibleProducts;  // 可见产品列表
    private List<String> visiblePeriods;   // 可见期间列表
}
```

### 5.13 会话上下文缓存

```java
@Component
public class SessionContextCache {

    private final Cache<String, List<ChatMessage>> sessionCache = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)  // 30分钟过期
        .maximumSize(1000)  // 最多1000个会话
        .build();

    public void addMessage(String sessionId, ChatMessage message) {
        List<ChatMessage> messages = sessionCache.getIfPresent(sessionId);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        sessionCache.put(sessionId, messages);
    }

    public List<ChatMessage> getMessages(String sessionId) {
        return sessionCache.getIfPresent(sessionId);
    }
}
```

---

## 六、交互模式设计

### 6.1 卡片式对话 + 工作流展示

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  主页面                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  内容区（图表/报表/分析结果）                                         │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  AI助手（底部统一入口）                                               │   │
│  │  ┌───────────────────────────────────────────────────────────────┐  │   │
│  │  │  💬 用户: 分析一下客户价值分布                                 │  │   │
│  │  │                                                               │  │   │
│  │  │  🤖 AI: 我来为您进行客户价值分析...                           │  │   │
│  │  │                                                               │  │   │
│  │  │  ┌─────────────────────────────────────────────────────────┐ │  │   │
│  │  │  │  🔍 专项分析Agent 执行流程                              │ │  │   │
│  │  │  │  ─────────────────────────────────────────────────────  │ │  │   │
│  │  │  │  ✅ 步骤1: 查询客户维度数据                    [完成]   │ │  │   │
│  │  │  │  ✅ 步骤2: 计算客户贡献度                      [完成]   │ │  │   │
│  │  │  │  ✅ 步骤3: 客户分层分析                        [完成]   │ │  │   │
│  │  │  │  ✅ 步骤4: 生成分析报告                        [完成]   │ │  │   │
│  │  │  │  ─────────────────────────────────────────────────────  │ │  │   │
│  │  │  │  📈 分析结果                                            │ │  │   │
│  │  │  │  ┌─────────────────────────────────────────────────┐   │ │  │   │
│  │  │  │  │  [客户价值金字塔图]                              │   │ │  │   │
│  │  │  │  │  TOP 10%客户贡献80%利润                         │   │ │  │   │
│  │  │  │  └─────────────────────────────────────────────────┘   │ │  │   │
│  │  │  │  💡 建议: 聚焦高价值客户维护，挖掘中价值客户潜力      │ │  │   │
│  │  │  └─────────────────────────────────────────────────────────┘ │  │   │
│  │  └───────────────────────────────────────────────────────────────┘  │   │
│  │  ┌───────────────────────────────────────────────────────────────┐  │   │
│  │  │  输入框: 请输入问题...                    [发送] [快捷技能▼]  │  │   │
│  │  └───────────────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 工作流卡片组件

```typescript
// components/AgentWorkflowCard.tsx
interface AgentWorkflowCardProps {
  agentName: string;
  agentIcon: string;
  status: 'running' | 'completed' | 'failed';
  steps: WorkflowStep[];
  result?: WorkflowResult;
}

interface WorkflowStep {
  id: string;
  name: string;
  status: 'pending' | 'running' | 'completed' | 'failed';
  tool?: string;
  duration?: number;
  output?: string;
}

interface WorkflowResult {
  summary: string;
  charts?: ChartConfig[];
  suggestions?: string[];
  confidence?: number;
}
```

### 6.3 交互场景分类

| 场景 | 交互方式 | 展示形式 |
|------|----------|----------|
| 简单查询（"本月收入？"） | 直接对话 | 文字回答 |
| 中等复杂（"各机构利润排名"） | 触发Skill | 文字+图表 |
| 复杂分析（"客户价值分析"） | 触发Agent | 工作流卡片 |
| 定时任务（"每日风险巡检"） | 后台执行 | 消息通知 |

---

## 七、API接口设计

### 7.1 Agent API

```java
@RestController
@RequestMapping("/api/agent")
public class AiAgentController {

    @Autowired
    private AgentExecutor agentExecutor;

    @Autowired
    private AgentRouter agentRouter;

    /**
     * Agent对话接口
     */
    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@RequestBody AgentChatRequest request,
                                                @RequestHeader("X-User-Id") String userId) {
        // 1. 获取用户上下文
        UserContext userContext = userService.getUserContext(userId);

        // 2. 路由到对应Agent
        String agentName = agentRouter.route(request.getMessage());

        // 3. 执行Agent
        AgentResult result = agentExecutor.execute(
            agentName, request.getMessage(), request.getSessionId(), userContext);

        // 4. 返回结果
        return ApiResponse.ok(new AgentChatResponse(
            agentName,
            result.getAnswer(),
            result.getData(),
            request.getSessionId()
        ));
    }

    /**
     * 获取Agent列表
     */
    @GetMapping("/list")
    public ApiResponse<List<AgentConfig>> listAgents() {
        return ApiResponse.ok(agentConfigLoader.getAllConfigs());
    }
}
```

### 7.2 请求/响应模型

```java
@Data
public class AgentChatRequest {
    private String message;
    private String sessionId;
    private Map<String, Object> context;
}

@Data
@AllArgsConstructor
public class AgentChatResponse {
    private String agentName;
    private String answer;
    private Map<String, Object> data;
    private String sessionId;
}
```

---

## 八、实施路线图

### 整体实施计划

| 阶段 | 任务 | 产出 | 时间 |
|------|------|------|------|
| Phase 1 | MCP Server搭建 | 5个MCP Server，24个工具 | 3周 |
| Phase 2 | Function Call集成 | Claude FC客户端，27个函数注册 | 1周 |
| Phase 3 | Skill系统开发 | 8个业务技能 | 2周 |
| Phase 4 | Agent引擎开发 | 5个Agent配置 + 执行引擎 | 3周 |
| Phase 5 | 前端集成 | 卡片式对话 + 工作流展示 | 2周 |

### 文件清单

| 类型 | 文件 | 说明 |
|------|------|------|
| **MCP Server** | 5个Java文件 | BizDataMcpServer, AnalysisMcpServer等 |
| **Function Call** | 4个Java文件 | ClaudeFunctionCallClient, FunctionRegistry等 |
| **Skill** | 12个Java文件 | 8个Skill实现 + 接口 + 注册中心 |
| **Agent配置** | 5个MD文件 | data-ingestion.md, deep-analysis.md等 |
| **Agent引擎** | 8个Java文件 | AgentExecutor, AgentRouter, AgentConfigLoader等 |
| **API** | 2个Java文件 | AiAgentController, 请求/响应模型 |
| **前端** | 5个TSX文件 | AgentWorkflowCard, AiAssistant页面等 |

---

## 九、总结

### 技术组件统计

| 组件类型 | 数量 | 说明 |
|---------|------|------|
| MCP Server | 5个 | 业财数据、分析算法、报表输出、数据治理、数据集成 |
| Function Call | 27个 | 数据查询8个、计算分析7个、操作输出6个、数据集成6个 |
| Skill | 8个 | 利润归因、亏损扫描、报告生成、情景模拟、客户分层、分摊诊断、异常检测、成本优化 |
| Agent | 5个 | 数据接入、专项分析、费用分摊、风险预警、智能助手 |

### 架构优势

1. **分层解耦**：MCP层统一收口，AI层无需感知底层实现
2. **能力复用**：同一套MCP工具可供给多个Agent使用
3. **灵活扩展**：新增数据源或能力仅需新增MCP Server
4. **口径统一**：所有计算逻辑在MCP层统一，保证一致性
5. **安全可控**：所有数据访问通过MCP层，统一权限管控
6. **配置灵活**：Agent用MD文件定义，无需改代码即可调整prompt

### 预期效果

| 场景 | 完善前 | 完善后 |
|------|--------|--------|
| 数据查询 | 手动写SQL | 自然语言查询 |
| 分析报告 | 人工编写 | AI自动生成 |
| 异常检测 | 人工巡检 | 自动监控预警 |
| 费用分摊 | 手动配置 | AI智能推荐 |
| 数据接入 | 手动导入 | 自动化全流程 |
