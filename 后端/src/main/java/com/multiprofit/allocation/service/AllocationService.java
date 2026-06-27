package com.multiprofit.allocation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiprofit.allocation.mapper.*;
import com.multiprofit.allocation.model.*;
import com.multiprofit.allocation.service.engine.AllocationAlgorithm;
import com.multiprofit.allocation.service.engine.AlgorithmRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分摊计算服务
 */
@Slf4j
@Service
public class AllocationService {

    @Autowired
    private AllocationRuleConfigMapper ruleMapper;

    @Autowired
    private AllocationFactorConfigMapper factorMapper;

    @Autowired
    private AllocationFactorWeightMapper weightMapper;

    @Autowired
    private AllocationBatchMapper batchMapper;

    @Autowired
    private AllocationDetailMapper detailMapper;

    @Autowired
    private AllocationFactorSnapshotMapper snapshotMapper;

    @Autowired
    private AlgorithmRegistry algorithmRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 执行分摊计算
     */
    @Transactional
    public AllocationBatch executeAllocation(AllocationRequest request) {
        log.info("开始执行分摊: 期间={}, 成本类型={}", request.getPeriod(), request.getCostType());

        // 1. 创建分摊批次
        AllocationBatch batch = createBatch(request);

        try {
            // 2. 更新批次状态为处理中
            batch.setStatus("PROCESSING");
            batch.setStartTime(LocalDateTime.now());
            batchMapper.updateById(batch);

            // 3. 获取待分摊的成本数据
            Map<String, BigDecimal> costData = getCostData(request.getPeriod(), request.getCostType());
            if (costData.isEmpty()) {
                log.warn("未找到待分摊的成本数据");
                batch.setStatus("COMPLETED");
                batch.setEndTime(LocalDateTime.now());
                batchMapper.updateById(batch);
                return batch;
            }

            // 4. 获取生效的分摊规则
            List<AllocationRuleConfig> rules = getEffectiveRules(request.getCostType());
            if (rules.isEmpty()) {
                log.warn("未找到生效的分摊规则");
                batch.setStatus("COMPLETED");
                batch.setEndTime(LocalDateTime.now());
                batchMapper.updateById(batch);
                return batch;
            }

            // 5. 执行分摊计算
            BigDecimal totalAllocated = BigDecimal.ZERO;
            int totalRecords = 0;

            for (AllocationRuleConfig rule : rules) {
                // 获取该规则对应的成本金额
                BigDecimal ruleCost = costData.getOrDefault(rule.getCostType(), BigDecimal.ZERO);
                if (ruleCost.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                // 执行单条规则的分摊
                List<AllocationDetail> details = executeRule(rule, ruleCost, request.getPeriod(), batch.getId());

                // 保存分摊明细
                for (AllocationDetail detail : details) {
                    detailMapper.insert(detail);
                    totalAllocated = totalAllocated.add(detail.getAllocatedAmount());
                }
                totalRecords += details.size();
            }

            // 6. 更新批次信息
            batch.setTotalAmount(costData.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            batch.setAllocatedAmount(totalAllocated);
            batch.setRecordCount(totalRecords);
            batch.setStatus("COMPLETED");
            batch.setEndTime(LocalDateTime.now());
            batchMapper.updateById(batch);

            log.info("分摊计算完成: 批次号={}, 总金额={}, 已分摊={}, 记录数={}",
                batch.getBatchNo(), batch.getTotalAmount(), batch.getAllocatedAmount(), batch.getRecordCount());

            return batch;

        } catch (Exception e) {
            log.error("分摊计算失败", e);
            batch.setStatus("FAILED");
            batch.setErrorMessage(e.getMessage());
            batch.setEndTime(LocalDateTime.now());
            batchMapper.updateById(batch);
            throw new RuntimeException("分摊计算失败: " + e.getMessage(), e);
        }
    }

    /**
     * 预览分摊结果（不实际执行）
     */
    public AllocationPreview previewAllocation(AllocationRequest request) {
        log.info("预览分摊结果: 期间={}, 成本类型={}", request.getPeriod(), request.getCostType());

        // 1. 获取待分摊的成本数据
        Map<String, BigDecimal> costData = getCostData(request.getPeriod(), request.getCostType());

        // 2. 获取生效的分摊规则
        List<AllocationRuleConfig> rules = getEffectiveRules(request.getCostType());

        // 3. 预览分摊结果
        List<AllocationPreviewItem> previewItems = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalAllocated = BigDecimal.ZERO;

        for (AllocationRuleConfig rule : rules) {
            BigDecimal ruleCost = costData.getOrDefault(rule.getCostType(), BigDecimal.ZERO);
            if (ruleCost.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            totalCost = totalCost.add(ruleCost);

            // 获取因子数据
            Map<String, BigDecimal> factorValues = getFactorValues(rule, request.getPeriod());

            // 计算分摊结果
            List<AllocationAlgorithm.AllocationResult> results = calculateAllocation(rule, ruleCost, factorValues);

            for (AllocationAlgorithm.AllocationResult result : results) {
                AllocationPreviewItem item = new AllocationPreviewItem();
                item.setRuleCode(rule.getRuleCode());
                item.setRuleName(rule.getRuleName());
                item.setCostType(rule.getCostType());
                item.setSourceDimType(rule.getSourceDimType());
                item.setSourceDimCode(rule.getSourceDimCode());
                item.setTargetDimType(result.getTargetDimType());
                item.setTargetDimCode(result.getTargetDimCode());
                item.setOriginalAmount(ruleCost);
                item.setAllocatedAmount(result.getAllocatedAmount());
                item.setAllocationRatio(result.getRatio());
                item.setFactorValue(result.getFactorValue());
                item.setAlgorithmCode(rule.getAlgorithmCode());

                previewItems.add(item);
                totalAllocated = totalAllocated.add(result.getAllocatedAmount());
            }
        }

        AllocationPreview preview = new AllocationPreview();
        preview.setPeriod(request.getPeriod());
        preview.setCostType(request.getCostType());
        preview.setTotalCost(totalCost);
        preview.setTotalAllocated(totalAllocated);
        preview.setRecordCount(previewItems.size());
        preview.setItems(previewItems);

        return preview;
    }

    /**
     * 查询分摊结果列表
     */
    public List<AllocationBatch> listResults(String period, String costType) {
        LambdaQueryWrapper<AllocationBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AllocationBatch::getPeriod, period);
        if (costType != null) {
            wrapper.eq(AllocationBatch::getCostType, costType);
        }
        wrapper.orderByDesc(AllocationBatch::getCreatedAt);
        return batchMapper.selectList(wrapper);
    }

    /**
     * 获取分摊结果详情
     */
    public AllocationBatchDetail getResultDetail(Long batchId) {
        // 获取批次信息
        AllocationBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("分摊批次不存在: " + batchId);
        }

        // 获取分摊明细
        List<AllocationDetail> details = detailMapper.selectByBatchId(batchId);

        // 获取因子快照
        List<AllocationFactorSnapshot> snapshots = snapshotMapper.selectByBatchId(batchId);

        AllocationBatchDetail detail = new AllocationBatchDetail();
        detail.setBatch(batch);
        detail.setDetails(details);
        detail.setSnapshots(snapshots);

        return detail;
    }

    /**
     * 查询目标维度的分摊汇总
     */
    public List<Map<String, Object>> getAllocationSummary(Long batchId, String targetDimType) {
        List<AllocationDetail> details = detailMapper.selectByBatchId(batchId);

        // 按目标维度分组汇总
        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        for (AllocationDetail detail : details) {
            if (targetDimType == null || targetDimType.equals(detail.getTargetDimType())) {
                summary.merge(detail.getTargetDimCode(), detail.getAllocatedAmount(), BigDecimal::add);
            }
        }

        // 转换为列表
        return summary.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("targetDimCode", entry.getKey());
                item.put("allocatedAmount", entry.getValue());
                return item;
            })
            .sorted((a, b) -> ((BigDecimal) b.get("allocatedAmount")).compareTo((BigDecimal) a.get("allocatedAmount")))
            .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 创建分摊批次
     */
    private AllocationBatch createBatch(AllocationRequest request) {
        AllocationBatch batch = new AllocationBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setPeriod(request.getPeriod());
        batch.setCostType(request.getCostType());
        batch.setStatus("PENDING");
        batch.setTriggerType(request.getTriggerType() != null ? request.getTriggerType() : "MANUAL");
        batch.setTriggeredBy(request.getTriggeredBy());
        batchMapper.insert(batch);
        return batch;
    }

    /**
     * 生成批次号
     */
    private String generateBatchNo() {
        return "ALLOC" + System.currentTimeMillis();
    }

    /**
     * 获取成本数据
     */
    private Map<String, BigDecimal> getCostData(String period, String costType) {
        // TODO: 从实际业务表获取成本数据
        // 这里使用模拟数据
        Map<String, BigDecimal> costData = new HashMap<>();
        costData.put("RENT", new BigDecimal("100000"));
        costData.put("SALARY", new BigDecimal("500000"));
        costData.put("IT", new BigDecimal("200000"));
        costData.put("MARKETING", new BigDecimal("150000"));

        if (costType != null) {
            BigDecimal amount = costData.get(costType);
            if (amount != null) {
                return Map.of(costType, amount);
            }
            return Collections.emptyMap();
        }
        return costData;
    }

    /**
     * 获取生效的分摊规则
     */
    private List<AllocationRuleConfig> getEffectiveRules(String costType) {
        if (costType != null) {
            return ruleMapper.selectByCostType(costType);
        }
        return ruleMapper.selectAllActive();
    }

    /**
     * 执行单条规则的分摊
     */
    private List<AllocationDetail> executeRule(AllocationRuleConfig rule, BigDecimal totalCost,
                                                String period, Long batchId) {
        // 1. 获取因子数据
        Map<String, BigDecimal> factorValues = getFactorValues(rule, period);

        // 2. 保存因子快照
        saveFactorSnapshots(rule, factorValues, period, batchId);

        // 3. 计算分摊结果
        List<AllocationAlgorithm.AllocationResult> results = calculateAllocation(rule, totalCost, factorValues);

        // 4. 转换为分摊明细
        List<AllocationDetail> details = new ArrayList<>();
        for (AllocationAlgorithm.AllocationResult result : results) {
            AllocationDetail detail = new AllocationDetail();
            detail.setBatchId(batchId);
            detail.setRuleId(rule.getId());
            detail.setPeriod(period);
            detail.setSourceDimType(rule.getSourceDimType());
            detail.setSourceDimCode(rule.getSourceDimCode() != null ? rule.getSourceDimCode() : "ALL");
            detail.setTargetDimType(result.getTargetDimType());
            detail.setTargetDimCode(result.getTargetDimCode());
            detail.setOriginalAmount(totalCost);
            detail.setAllocatedAmount(result.getAllocatedAmount());
            detail.setAllocationRatio(result.getRatio());
            detail.setAlgorithmCode(rule.getAlgorithmCode());

            // 序列化因子值和计算详情
            try {
                detail.setFactorValues(objectMapper.writeValueAsString(result.getFactorValues()));
                detail.setCalcDetails(objectMapper.writeValueAsString(result.getCalcDetails()));
            } catch (Exception e) {
                log.warn("序列化失败", e);
            }

            details.add(detail);
        }

        return details;
    }

    /**
     * 获取因子值
     */
    private Map<String, BigDecimal> getFactorValues(AllocationRuleConfig rule, String period) {
        // TODO: 从实际业务表获取因子数据
        // 这里使用模拟数据
        Map<String, BigDecimal> factorValues = new LinkedHashMap<>();
        factorValues.put("ORG001", new BigDecimal("3000"));
        factorValues.put("ORG002", new BigDecimal("5000"));
        factorValues.put("ORG003", new BigDecimal("2000"));
        return factorValues;
    }

    /**
     * 计算分摊结果
     */
    private List<AllocationAlgorithm.AllocationResult> calculateAllocation(
            AllocationRuleConfig rule, BigDecimal totalCost, Map<String, BigDecimal> factorValues) {

        // 1. 解析算法参数
        Map<String, Object> algorithmParams = parseAlgorithmParams(rule.getAlgorithmParams());

        // 2. 构建分摊上下文
        AllocationAlgorithm.AllocationContext context = AllocationAlgorithm.AllocationContext.builder()
            .totalCost(totalCost)
            .costType(rule.getCostType())
            .sourceDimType(rule.getSourceDimType())
            .sourceDimCode(rule.getSourceDimCode())
            .targetDimType(rule.getTargetDimType())
            .targetDimFilter(rule.getTargetDimFilter())
            .factorValues(factorValues)
            .algorithmParams(algorithmParams)
            .build();

        // 3. 获取权重配置（加权算法需要）
        if ("WEIGHTED".equals(rule.getAlgorithmCode())) {
            List<AllocationFactorWeight> weights = weightMapper.selectByRuleId(rule.getId());
            Map<String, BigDecimal> weightMap = new LinkedHashMap<>();
            for (AllocationFactorWeight weight : weights) {
                weightMap.put(weight.getFactorCode(), weight.getWeight());
            }
            context = AllocationAlgorithm.AllocationContext.builder()
                .totalCost(totalCost)
                .costType(rule.getCostType())
                .sourceDimType(rule.getSourceDimType())
                .sourceDimCode(rule.getSourceDimCode())
                .targetDimType(rule.getTargetDimType())
                .targetDimFilter(rule.getTargetDimFilter())
                .factorValues(factorValues)
                .factorWeights(weightMap)
                .algorithmParams(algorithmParams)
                .build();
        }

        // 4. 执行算法
        return algorithmRegistry.executeAlgorithm(rule.getAlgorithmCode(), context);
    }

    /**
     * 解析算法参数
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAlgorithmParams(String paramsJson) {
        if (paramsJson == null || paramsJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(paramsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析算法参数失败: {}", paramsJson, e);
            return new HashMap<>();
        }
    }

    /**
     * 保存因子快照
     */
    private void saveFactorSnapshots(AllocationRuleConfig rule, Map<String, BigDecimal> factorValues,
                                      String period, Long batchId) {
        String factorCode = parseAlgorithmParams(rule.getAlgorithmParams())
            .getOrDefault("factor_code", "UNKNOWN").toString();

        for (Map.Entry<String, BigDecimal> entry : factorValues.entrySet()) {
            AllocationFactorSnapshot snapshot = new AllocationFactorSnapshot();
            snapshot.setBatchId(batchId);
            snapshot.setPeriod(period);
            snapshot.setFactorCode(factorCode);
            snapshot.setDimType(rule.getTargetDimType());
            snapshot.setDimCode(entry.getKey());
            snapshot.setFactorValue(entry.getValue());

            // 计算占比
            BigDecimal total = factorValues.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                snapshot.setFactorRatio(entry.getValue().divide(total, 10, RoundingMode.HALF_UP));
            }

            snapshotMapper.insert(snapshot);
        }
    }

    // ========== 内部类 ==========

    /**
     * 分摊请求
     */
    public static class AllocationRequest {
        private String period;
        private String costType;
        private String triggerType;
        private String triggeredBy;

        // Getters and Setters
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public String getCostType() { return costType; }
        public void setCostType(String costType) { this.costType = costType; }
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public String getTriggeredBy() { return triggeredBy; }
        public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    }

    /**
     * 分摊预览
     */
    public static class AllocationPreview {
        private String period;
        private String costType;
        private BigDecimal totalCost;
        private BigDecimal totalAllocated;
        private int recordCount;
        private List<AllocationPreviewItem> items;

        // Getters and Setters
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public String getCostType() { return costType; }
        public void setCostType(String costType) { this.costType = costType; }
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        public BigDecimal getTotalAllocated() { return totalAllocated; }
        public void setTotalAllocated(BigDecimal totalAllocated) { this.totalAllocated = totalAllocated; }
        public int getRecordCount() { return recordCount; }
        public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
        public List<AllocationPreviewItem> getItems() { return items; }
        public void setItems(List<AllocationPreviewItem> items) { this.items = items; }
    }

    /**
     * 分摊预览项
     */
    public static class AllocationPreviewItem {
        private String ruleCode;
        private String ruleName;
        private String costType;
        private String sourceDimType;
        private String sourceDimCode;
        private String targetDimType;
        private String targetDimCode;
        private BigDecimal originalAmount;
        private BigDecimal allocatedAmount;
        private BigDecimal allocationRatio;
        private BigDecimal factorValue;
        private String algorithmCode;

        // Getters and Setters
        public String getRuleCode() { return ruleCode; }
        public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getCostType() { return costType; }
        public void setCostType(String costType) { this.costType = costType; }
        public String getSourceDimType() { return sourceDimType; }
        public void setSourceDimType(String sourceDimType) { this.sourceDimType = sourceDimType; }
        public String getSourceDimCode() { return sourceDimCode; }
        public void setSourceDimCode(String sourceDimCode) { this.sourceDimCode = sourceDimCode; }
        public String getTargetDimType() { return targetDimType; }
        public void setTargetDimType(String targetDimType) { this.targetDimType = targetDimType; }
        public String getTargetDimCode() { return targetDimCode; }
        public void setTargetDimCode(String targetDimCode) { this.targetDimCode = targetDimCode; }
        public BigDecimal getOriginalAmount() { return originalAmount; }
        public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
        public BigDecimal getAllocatedAmount() { return allocatedAmount; }
        public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }
        public BigDecimal getAllocationRatio() { return allocationRatio; }
        public void setAllocationRatio(BigDecimal allocationRatio) { this.allocationRatio = allocationRatio; }
        public BigDecimal getFactorValue() { return factorValue; }
        public void setFactorValue(BigDecimal factorValue) { this.factorValue = factorValue; }
        public String getAlgorithmCode() { return algorithmCode; }
        public void setAlgorithmCode(String algorithmCode) { this.algorithmCode = algorithmCode; }
    }

    /**
     * 分摊批次详情
     */
    public static class AllocationBatchDetail {
        private AllocationBatch batch;
        private List<AllocationDetail> details;
        private List<AllocationFactorSnapshot> snapshots;

        // Getters and Setters
        public AllocationBatch getBatch() { return batch; }
        public void setBatch(AllocationBatch batch) { this.batch = batch; }
        public List<AllocationDetail> getDetails() { return details; }
        public void setDetails(List<AllocationDetail> details) { this.details = details; }
        public List<AllocationFactorSnapshot> getSnapshots() { return snapshots; }
        public void setSnapshots(List<AllocationFactorSnapshot> snapshots) { this.snapshots = snapshots; }
    }
}
