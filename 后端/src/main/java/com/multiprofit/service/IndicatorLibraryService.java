package com.multiprofit.service;

import java.util.List;
import java.util.Map;

/**
 * 指标库服务接口
 * 从indicator_library表读取指标定义,驱动前端卡片和维度分析
 */
public interface IndicatorLibraryService {
    /** 按类别获取指标code列表 */
    List<String> getCodesByCategory(String category);
    /** 获取所有活跃指标code列表 */
    List<String> getAllActiveCodes();
    /** 按code获取指标详情 */
    Map<String, Object> getIndicatorByCode(String code);
}
