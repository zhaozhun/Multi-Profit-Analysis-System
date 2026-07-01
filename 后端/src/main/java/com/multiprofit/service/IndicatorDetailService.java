package com.multiprofit.service;

import java.util.List;
import java.util.Map;

/**
 * 指标明细服务接口
 */
public interface IndicatorDetailService {

    /**
     * 查询贷款指标汇总数据
     * @param period 账期月份
     * @param caliberType 口径类型
     * @return 汇总数据
     */
    Map<String, Object> getLoanIndicatorSummary(String period, String caliberType);

    /**
     * 查询存款指标汇总数据
     * @param period 账期月份
     * @param caliberType 口径类型
     * @return 汇总数据
     */
    Map<String, Object> getDepositIndicatorSummary(String period, String caliberType);

    /**
     * 查询贷款指标明细列表
     * @param period 账期月份
     * @param caliberType 口径类型
     * @param dimension 维度类型
     * @param dimensionValue 维度值
     * @param page 页码
     * @param pageSize 每页大小
     * @return 明细列表
     */
    Map<String, Object> getLoanIndicatorDetailList(String period, String caliberType,
                                                    String dimension, String dimensionValue,
                                                    int page, int pageSize);

    /**
     * 查询存款指标明细列表
     * @param period 账期月份
     * @param caliberType 口径类型
     * @param dimension 维度类型
     * @param dimensionValue 维度值
     * @param page 页码
     * @param pageSize 每页大小
     * @return 明细列表
     */
    Map<String, Object> getDepositIndicatorDetailList(String period, String caliberType,
                                                       String dimension, String dimensionValue,
                                                       int page, int pageSize);

    /**
     * 按维度汇总贷款指标
     * @param period 账期月份
     * @param caliberType 口径类型
     * @param dimension 维度类型
     * @return 维度汇总数据
     */
    List<Map<String, Object>> getLoanIndicatorByDimension(String period, String caliberType, String dimension);

    /**
     * 按维度汇总存款指标
     * @param period 账期月份
     * @param caliberType 口径类型
     * @param dimension 维度类型
     * @return 维度汇总数据
     */
    List<Map<String, Object>> getDepositIndicatorByDimension(String period, String caliberType, String dimension);

    /**
     * 查询贷款指标趋势
     * @param months 月份数
     * @param caliberType 口径类型
     * @return 趋势数据
     */
    List<Map<String, Object>> getLoanIndicatorTrend(int months, String caliberType);

    /**
     * 查询存款指标趋势
     * @param months 月份数
     * @param caliberType 口径类型
     * @return 趋势数据
     */
    List<Map<String, Object>> getDepositIndicatorTrend(int months, String caliberType);
}
