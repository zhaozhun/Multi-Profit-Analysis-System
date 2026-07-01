package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分摊规则表
 */
@Data
@TableName("expense_allocation_rule")
public class ExpenseAllocationRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 费用表名 */
    private String expenseTable;

    /** 费用类型 */
    private String expenseType;

    /** 源维度类型 */
    private String sourceDimType;

    /** 目标类型 */
    private String targetType;

    /** 分摊因子编码 */
    private String factorCode;

    /** 描述 */
    private String description;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
