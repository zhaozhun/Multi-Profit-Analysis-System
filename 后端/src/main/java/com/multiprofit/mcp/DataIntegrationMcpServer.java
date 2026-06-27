package com.multiprofit.mcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据集成MCP Server
 * 统一封装数据接入、清洗、转换能力
 */
@Component
public class DataIntegrationMcpServer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * MCP工具：检测文件格式
     * 检测上传文件的格式、编码、字段
     */
    @McpTool(name = "detect_file_format", description = "检测上传文件的格式、编码、字段")
    public Map<String, Object> detectFormat(String filePath) {

        // 检测文件格式（简化实现）
        Map<String, Object> formatInfo = new HashMap<>();
        formatInfo.put("format", "CSV");
        formatInfo.put("encoding", "UTF-8");
        formatInfo.put("fields", Arrays.asList("period", "org", "product", "revenue", "cost", "profit"));
        formatInfo.put("rowCount", 1000);

        Map<String, Object> result = new HashMap<>();
        result.put("filePath", filePath);
        result.put("format", formatInfo.get("format"));
        result.put("encoding", formatInfo.get("encoding"));
        result.put("fields", formatInfo.get("fields"));
        result.put("rowCount", formatInfo.get("rowCount"));

        return result;
    }

    /**
     * MCP工具：字段映射
     * 将源字段映射到目标表字段
     */
    @McpTool(name = "map_fields", description = "将源字段映射到目标表字段")
    public Map<String, Object> mapFields(
            List<String> sourceFields,
            String targetTable) {

        // 获取目标表字段
        List<String> targetFields = getTableFields(targetTable);

        // 智能映射
        List<Map<String, Object>> mappings = new ArrayList<>();
        for (String sourceField : sourceFields) {
            Map<String, Object> mapping = new HashMap<>();
            mapping.put("sourceField", sourceField);

            // 查找最佳匹配
            String bestMatch = findBestMatch(sourceField, targetFields);
            mapping.put("targetField", bestMatch);
            mapping.put("confidence", bestMatch != null ? calculateConfidence(sourceField, bestMatch) : 0);

            mappings.add(mapping);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sourceFields", sourceFields);
        result.put("targetTable", targetTable);
        result.put("targetFields", targetFields);
        result.put("mappings", mappings);

        return result;
    }

    /**
     * MCP工具：数据清洗
     * 执行数据清洗规则
     */
    @McpTool(name = "clean_data", description = "执行数据清洗规则")
    public Map<String, Object> cleanData(
            List<Map<String, Object>> data,
            List<String> rules) {

        List<Map<String, Object>> cleanedData = new ArrayList<>();
        int totalRecords = data.size();
        int cleanedRecords = 0;
        List<Map<String, Object>> issues = new ArrayList<>();

        for (Map<String, Object> record : data) {
            Map<String, Object> cleanedRecord = new HashMap<>(record);
            boolean hasIssue = false;

            for (String rule : rules) {
                Map<String, Object> ruleResult = applyCleaningRule(cleanedRecord, rule);
                if ((Boolean) ruleResult.get("applied")) {
                    cleanedRecord = (Map<String, Object>) ruleResult.get("record");
                    hasIssue = true;
                }
            }

            if (hasIssue) {
                cleanedRecords++;
                Map<String, Object> issue = new HashMap<>();
                issue.put("recordIndex", cleanedData.size());
                issue.put("original", record);
                issue.put("cleaned", cleanedRecord);
                issues.add(issue);
            }

            cleanedData.add(cleanedRecord);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalRecords", totalRecords);
        result.put("cleanedRecords", cleanedRecords);
        result.put("cleanRate", totalRecords > 0 ? Math.round((double) cleanedRecords / totalRecords * 100.0) / 100.0 : 0);
        result.put("issues", issues);
        result.put("cleanedData", cleanedData);

        return result;
    }

    /**
     * MCP工具：维度关联
     * 将维度编码关联到dimension_master
     */
    @McpTool(name = "link_dimensions", description = "将维度编码关联到dimension_master")
    public Map<String, Object> linkDimensions(
            List<Map<String, Object>> data,
            Map<String, String> dimMappings) {

        List<Map<String, Object>> linkedData = new ArrayList<>();
        int totalRecords = data.size();
        int linkedRecords = 0;
        List<Map<String, Object>> unmapped = new ArrayList<>();

        for (Map<String, Object> record : data) {
            Map<String, Object> linkedRecord = new HashMap<>(record);
            boolean allMapped = true;

            for (Map.Entry<String, String> mapping : dimMappings.entrySet()) {
                String sourceField = mapping.getKey();
                String targetDim = mapping.getValue();

                if (linkedRecord.containsKey(sourceField)) {
                    String sourceValue = linkedRecord.get(sourceField).toString();

                    // 查找维度编码
                    String dimCode = findDimensionCode(targetDim, sourceValue);
                    if (dimCode != null) {
                        linkedRecord.put(targetDim + "_code", dimCode);
                    } else {
                        allMapped = false;
                        Map<String, Object> unmappedItem = new HashMap<>();
                        unmappedItem.put("recordIndex", linkedData.size());
                        unmappedItem.put("field", sourceField);
                        unmappedItem.put("value", sourceValue);
                        unmapped.add(unmappedItem);
                    }
                }
            }

            if (allMapped) {
                linkedRecords++;
            }
            linkedData.add(linkedRecord);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalRecords", totalRecords);
        result.put("linkedRecords", linkedRecords);
        result.put("linkRate", totalRecords > 0 ? Math.round((double) linkedRecords / totalRecords * 100.0) / 100.0 : 0);
        result.put("unmapped", unmapped);
        result.put("linkedData", linkedData);

        return result;
    }

    /**
     * MCP工具：数据入库
     * 将清洗后的数据写入目标表
     */
    @McpTool(name = "import_data", description = "将清洗后的数据写入目标表")
    public Map<String, Object> importData(
            List<Map<String, Object>> data,
            String targetTable,
            String mode) {

        // 执行数据导入（简化实现）
        int importedRecords = 0;
        int failedRecords = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (Map<String, Object> record : data) {
            try {
                // 构建插入SQL
                StringBuilder sql = new StringBuilder("INSERT INTO " + targetTable + " (");
                StringBuilder values = new StringBuilder("VALUES (");

                int i = 0;
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    if (i > 0) {
                        sql.append(", ");
                        values.append(", ");
                    }
                    sql.append(entry.getKey());
                    values.append("?");
                    i++;
                }
                sql.append(") ").append(values).append(")");

                // 执行插入
                jdbcTemplate.update(sql.toString(), record.values().toArray());
                importedRecords++;

            } catch (Exception e) {
                failedRecords++;
                Map<String, Object> error = new HashMap<>();
                error.put("recordIndex", importedRecords + failedRecords);
                error.put("error", e.getMessage());
                errors.add(error);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("targetTable", targetTable);
        result.put("mode", mode);
        result.put("totalRecords", data.size());
        result.put("importedRecords", importedRecords);
        result.put("failedRecords", failedRecords);
        result.put("errors", errors);

        return result;
    }

    /**
     * MCP工具：生成质量报告
     * 生成数据质量报告
     */
    @McpTool(name = "generate_quality_report", description = "生成数据质量报告")
    public Map<String, Object> generateReport(String importId) {

        // 生成质量报告（简化实现）
        Map<String, Object> report = new HashMap<>();
        report.put("importId", importId);
        report.put("status", "completed");
        report.put("totalRecords", 1000);
        report.put("successRecords", 980);
        report.put("failedRecords", 20);
        report.put("successRate", 0.98);

        Map<String, Object> result = new HashMap<>();
        result.put("importId", importId);
        result.put("report", report);

        return result;
    }

    /**
     * 获取表字段
     */
    private List<String> getTableFields(String tableName) {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?";
        return jdbcTemplate.queryForList(sql, String.class, tableName);
    }

    /**
     * 查找最佳匹配字段
     */
    private String findBestMatch(String sourceField, List<String> targetFields) {
        String normalizedSource = normalizeFieldName(sourceField);

        for (String targetField : targetFields) {
            String normalizedTarget = normalizeFieldName(targetField);

            // 完全匹配
            if (normalizedSource.equals(normalizedTarget)) {
                return targetField;
            }

            // 包含匹配
            if (normalizedSource.contains(normalizedTarget) || normalizedTarget.contains(normalizedSource)) {
                return targetField;
            }
        }

        return null;
    }

    /**
     * 计算匹配置信度
     */
    private double calculateConfidence(String sourceField, String targetField) {
        String normalizedSource = normalizeFieldName(sourceField);
        String normalizedTarget = normalizeFieldName(targetField);

        if (normalizedSource.equals(normalizedTarget)) {
            return 1.0;
        }

        if (normalizedSource.contains(normalizedTarget) || normalizedTarget.contains(normalizedSource)) {
            return 0.8;
        }

        return 0.5;
    }

    /**
     * 标准化字段名
     */
    private String normalizeFieldName(String fieldName) {
        return fieldName.toLowerCase()
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "");
    }

    /**
     * 查找维度编码
     */
    private String findDimensionCode(String dimType, String value) {
        String sql = "SELECT dim_code FROM dimension_master WHERE dim_type = ? AND (dim_code = ? OR dim_name = ?)";
        List<String> codes = jdbcTemplate.queryForList(sql, String.class, dimType, value, value);
        return codes.isEmpty() ? null : codes.get(0);
    }

    /**
     * 应用清洗规则
     */
    private Map<String, Object> applyCleaningRule(Map<String, Object> record, String rule) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> cleanedRecord = new HashMap<>(record);
        boolean applied = false;

        switch (rule.toLowerCase()) {
            case "trim_whitespace":
                // 去除空格
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        String trimmed = ((String) entry.getValue()).trim();
                        if (!trimmed.equals(entry.getValue())) {
                            cleanedRecord.put(entry.getKey(), trimmed);
                            applied = true;
                        }
                    }
                }
                break;

            case "null_to_empty":
                // 空值替换为空字符串
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    if (entry.getValue() == null) {
                        cleanedRecord.put(entry.getKey(), "");
                        applied = true;
                    }
                }
                break;

            case "standardize_date":
                // 日期标准化
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    if (entry.getKey().toLowerCase().contains("date") && entry.getValue() instanceof String) {
                        String standardized = standardizeDate((String) entry.getValue());
                        if (!standardized.equals(entry.getValue())) {
                            cleanedRecord.put(entry.getKey(), standardized);
                            applied = true;
                        }
                    }
                }
                break;
        }

        result.put("record", cleanedRecord);
        result.put("applied", applied);

        return result;
    }

    /**
     * 标准化日期格式
     */
    private String standardizeDate(String date) {
        // 简单的日期标准化逻辑
        if (date == null) return "";

        // 移除多余空格
        date = date.trim();

        // 尝试解析常见格式
        if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return date;
        } else if (date.matches("\\d{4}/\\d{2}/\\d{2}")) {
            return date.replace("/", "-");
        } else if (date.matches("\\d{8}")) {
            return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
        }

        return date;
    }
}
