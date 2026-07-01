package com.multiprofit.service;

import java.util.List;
import java.util.Map;

public interface DataWarehouseService {
    /**
     * 获取指标汇总
     */
    Map<String, Object> getIndicatorSummary(String indicatorCode, String period, String caliberType);

    /**
     * 获取指标维度数据
     */
    List<Map<String, Object>> getIndicatorDimension(String indicatorCode, String period, String dimType, String caliberType);

    /**
     * 获取指标明细数据
     */
    Map<String, Object> getIndicatorDetail(String indicatorCode, String period, String dimType, Long dimId, String caliberType);

    /**
     * 获取指标趋势
     */
    List<Map<String, Object>> getIndicatorTrend(String indicatorCode, int months, String caliberType);

    /**
     * 获取指标列表
     */
    List<Map<String, Object>> getIndicatorList();
}
