package com.multiprofit.allocation.ai;

/**
 * 分摊相关Function Call定义
 */
public class AllocationFunctionDefinitions {

    /**
     * 获取分摊规则列表
     */
    public static final String GET_ALLOCATION_RULES = """
    {
        "name": "get_allocation_rules",
        "description": "获取分摊规则列表，支持按成本类型、状态筛选",
        "parameters": {
            "type": "object",
            "properties": {
                "cost_type": {
                    "type": "string",
                    "description": "成本类型编码，如 RENT, SALARY, IT, MARKETING 等"
                },
                "status": {
                    "type": "string",
                    "enum": ["ACTIVE", "INACTIVE", "DRAFT"],
                    "description": "规则状态"
                }
            }
        }
    }
    """;

    /**
     * 预览分摊结果
     */
    public static final String PREVIEW_ALLOCATION = """
    {
        "name": "preview_allocation",
        "description": "预览指定规则的分摊结果，不实际执行分摊",
        "parameters": {
            "type": "object",
            "properties": {
                "rule_code": {
                    "type": "string",
                    "description": "分摊规则编码"
                },
                "period": {
                    "type": "string",
                    "description": "期间，格式 YYYY-MM"
                }
            },
            "required": ["rule_code", "period"]
        }
    }
    """;

    /**
     * 分析分摊结果
     */
    public static final String ANALYZE_ALLOCATION = """
    {
        "name": "analyze_allocation_result",
        "description": "分析分摊结果，识别异常和优化机会",
        "parameters": {
            "type": "object",
            "properties": {
                "period": {
                    "type": "string",
                    "description": "期间，格式 YYYY-MM"
                },
                "dim_type": {
                    "type": "string",
                    "enum": ["ORG", "DEPT", "PRODUCT", "CHANNEL", "MANAGER"],
                    "description": "分析维度"
                },
                "cost_type": {
                    "type": "string",
                    "description": "成本类型（可选）"
                }
            },
            "required": ["period"]
        }
    }
    """;

    /**
     * 推荐分摊规则
     */
    public static final String SUGGEST_RULE = """
    {
        "name": "suggest_allocation_rule",
        "description": "根据成本特性推荐分摊规则配置",
        "parameters": {
            "type": "object",
            "properties": {
                "cost_type": {
                    "type": "string",
                    "description": "成本类型"
                },
                "cost_amount": {
                    "type": "number",
                    "description": "成本金额"
                },
                "business_context": {
                    "type": "string",
                    "description": "业务背景描述"
                }
            },
            "required": ["cost_type"]
        }
    }
    """;

    /**
     * 获取分摊因子数据
     */
    public static final String GET_FACTOR_DATA = """
    {
        "name": "get_factor_data",
        "description": "获取指定因子的数值数据",
        "parameters": {
            "type": "object",
            "properties": {
                "factor_code": {
                    "type": "string",
                    "description": "因子编码，如 VOLUME, REVENUE, HEADCOUNT, AREA 等"
                },
                "period": {
                    "type": "string",
                    "description": "期间，格式 YYYY-MM"
                },
                "dim_type": {
                    "type": "string",
                    "description": "维度类型"
                }
            },
            "required": ["factor_code", "period"]
        }
    }
    """;

    /**
     * 诊断分摊规则
     */
    public static final String DIAGNOSE_RULES = """
    {
        "name": "diagnose_allocation_rules",
        "description": "诊断分摊规则配置的合理性，检查冲突和遗漏",
        "parameters": {
            "type": "object",
            "properties": {
                "period": {
                    "type": "string",
                    "description": "期间，格式 YYYY-MM"
                }
            },
            "required": ["period"]
        }
    }
    """;
}
