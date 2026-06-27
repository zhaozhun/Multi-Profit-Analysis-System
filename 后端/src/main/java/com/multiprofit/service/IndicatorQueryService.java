package com.multiprofit.service;

import java.util.List;
import java.util.Map;

/**
 * 指标查询服务接口
 */
public interface IndicatorQueryService {
    /**
     * 获取指标值
     * @param indicatorCode 指标编码
     * @param calcPeriod 计算周期
     * @param periodValue 周期值
     * @return 指标值
     */
    Map<String, Object> getIndicatorValue(String indicatorCode, String calcPeriod, String periodValue);

    /**
     * 获取指标趋势
     * @param indicatorCode 指标编码
     * @param months 月份数量
     * @return 趋势数据
     */
    List<Map<String, Object>> getIndicatorTrend(String indicatorCode, int months);

    /**
     * 获取指标明细（分页）
     * @param indicatorCode 指标编码
     * @param calcPeriod 计算周期
     * @param periodValue 周期值
     * @param page 页码
     * @param size 每页数量
     * @return 明细数据
     */
    Map<String, Object> getIndicatorDetail(String indicatorCode, String calcPeriod, String periodValue, int page, int size);

    /**
     * 按分组获取明细
     * @param indicatorCode 指标编码
     * @param calcPeriod 计算周期
     * @param periodValue 周期值
     * @param groupValue 分组值
     * @return 明细数据
     */
    Map<String, Object> getIndicatorDetailByGroup(String indicatorCode, String calcPeriod, String periodValue, String groupValue);

    /**
     * 对比多个指标
     * @param indicatorCodes 指标编码列表
     * @param calcPeriod 计算周期
     * @param periodValue 周期值
     * @return 对比结果
     */
    Map<String, Object> compareIndicators(List<String> indicatorCodes, String calcPeriod, String periodValue);
}
