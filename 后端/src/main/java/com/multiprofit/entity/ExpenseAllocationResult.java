package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用分摊结果表
 */
@Data
@TableName("expense_allocation_result")
public class ExpenseAllocationResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 期间 */
    private String period;

    /** 业务编号 */
    private String bizId;

    /** 业务类型 */
    private String bizType;

    /** 费用类型 */
    private String expenseType;

    /** 费用名称 */
    private String expenseName;

    /** 分摊金额 */
    private BigDecimal allocatedAmount;

    /** 因子值 */
    private BigDecimal factorValue;

    /** 分摊比例 */
    private BigDecimal factorRatio;

    /** 规则编码 */
    private String ruleCode;

    /** 批次号 */
    private String batchNo;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
