package com.multiprofit.service;

import java.util.List;
import java.util.Map;

/**
 * 运营成本分摊服务接口
 */
public interface ExpenseAllocationService {

    /**
     * 获取运营成本汇总
     * @param period 期间
     * @param caliberType 口径
     * @param dimension 维度类型
     * @return 汇总数据
     */
    Map<String, Object> getExpenseSummary(String period, String caliberType, String dimension);

    /**
     * 获取业务费用组成
     * @param period 期间
     * @param bizId 业务编号
     * @return 费用组成
     */
    List<Map<String, Object>> getBizExpenseComposition(String period, String bizId);

    /**
     * 获取费用原始数据
     * @param period 期间
     * @param expenseType 费用类型
     * @return 原始数据列表
     */
    List<Map<String, Object>> getExpenseOriginalData(String period, String expenseType);

    /**
     * 获取分摊因子列表
     * @return 因子列表
     */
    List<Map<String, Object>> getAllocationFactors();

    /**
     * 获取分摊规则列表
     * @param expenseType 费用类型
     * @return 规则列表
     */
    List<Map<String, Object>> getAllocationRules(String expenseType);

    /**
     * 保存分摊规则
     * @param rule 规则配置
     * @return 保存结果
     */
    Map<String, Object> saveAllocationRule(Map<String, Object> rule);

    /**
     * 执行分摊
     * @param period 期间
     * @param expenseType 费用类型（可选）
     * @return 执行结果
     */
    Map<String, Object> executeAllocation(String period, String expenseType);
}
