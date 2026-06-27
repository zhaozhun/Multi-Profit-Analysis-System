package com.multiprofit.service;

import java.util.List;
import java.util.Map;

public interface IndicatorSummaryService {
    /**
     * 获取指标汇总数据
     * @param businessLine 业务条线：ASSET/LIABILITY
     * @param period 账期月份
     * @param statType 统计类型：MONTHLY_DAILY_AVG/YEARLY_DAILY_AVG
     * @return 指标汇总数据列表
     */
    List<Map<String, Object>> getIndicatorSummary(String businessLine, String period, String statType);

    /**
     * 获取费用类型列表
     * @param businessLine 业务条线：ASSET/LIABILITY
     * @return 费用类型列表
     */
    List<Map<String, Object>> getCostTypes(String businessLine);

    /**
     * 获取费用分摊结果
     * @param costType 费用类型
     * @param period 账期月份
     * @param page 页码
     * @param size 每页大小
     * @return 费用分摊结果
     */
    Map<String, Object> getCostAllocationResult(String costType, String period, int page, int size);

    /**
     * 获取费用原始数据
     * @param costType 费用类型
     * @param period 账期月份
     * @param dimType 维度类型
     * @return 费用原始数据
     */
    Map<String, Object> getCostOriginalData(String costType, String period, String dimType);

    /**
     * 触发指标预计算
     * @param period 账期月份
     * @param indicatorCode 指标编码（可选）
     * @return 计算结果
     */
    Map<String, Object> calculateIndicators(String period, String indicatorCode);
}
