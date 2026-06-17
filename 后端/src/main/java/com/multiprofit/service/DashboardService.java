package com.multiprofit.service;

import com.multiprofit.dto.DashboardDTO;

/**
 * 驾驶舱服务接口（支持日期范围）
 */
public interface DashboardService {

    /**
     * 获取驾驶舱全量数据
     */
    DashboardDTO getDashboardData(String startDate, String endDate, String caliberType, Long orgScope);

    /**
     * 获取瀑布图数据
     */
    DashboardDTO.WaterfallData getWaterfallData(String startDate, String endDate, String caliberType);

    /**
     * 获取趋势数据（近12个月）
     */
    DashboardDTO.TrendData getTrendData(String endDate, String caliberType);

    /**
     * 获取维度盈利概览
     */
    DashboardDTO.DimOverview getDimOverview(String dimType, String startDate, String endDate, String caliberType);
}
