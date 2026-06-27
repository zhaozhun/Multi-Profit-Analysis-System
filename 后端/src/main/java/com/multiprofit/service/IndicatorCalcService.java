package com.multiprofit.service;

import java.util.List;
import java.util.Map;

/**
 * 指标计算服务接口
 */
public interface IndicatorCalcService {
    /**
     * 计算原子指标
     * @param indicatorCode 指标编码
     * @param calcPeriod 计算周期（MONTH/YEAR）
     * @param periodValue 周期值（2024-01/2024）
     * @return 计算结果
     */
    Map<String, Object> calcAtomicIndicator(String indicatorCode, String calcPeriod, String periodValue);

    /**
     * 计算派生指标
     * @param indicatorCode 指标编码
     * @param calcPeriod 计算周期
     * @param periodValue 周期值
     * @return 计算结果
     */
    Map<String, Object> calcDerivedIndicator(String indicatorCode, String calcPeriod, String periodValue);

    /**
     * 计算统计口径
     * @param indicatorCode 指标编码
     * @param statType 统计口径（MONTHLY_DAILY_AVG/YEARLY_DAILY_AVG）
     * @param calcPeriod 计算周期
     * @param periodValue 周期值
     * @return 计算结果
     */
    Map<String, Object> calcStatConfig(String indicatorCode, String statType, String calcPeriod, String periodValue);

    /**
     * 批量计算所有指标
     * @param calcPeriod 计算周期
     * @param periodValue 周期值
     * @return 计算结果列表
     */
    List<Map<String, Object>> calcAllIndicators(String calcPeriod, String periodValue);

    /**
     * 获取指标明细
     * @param indicatorCode 指标编码
     * @param calcPeriod 计算周期
     * @param periodValue 周期值
     * @param page 页码
     * @param size 每页数量
     * @return 明细数据
     */
    Map<String, Object> getIndicatorDetail(String indicatorCode, String calcPeriod, String periodValue, int page, int size);
}
