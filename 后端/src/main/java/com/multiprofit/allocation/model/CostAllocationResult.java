package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用分摊结果实体
 */
@Data
@TableName("cost_allocation_result")
public class CostAllocationResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 期间
     */
    private String period;

    /**
     * 费用编码
     */
    private String costCode;

    /**
     * 费用名称
     */
    private String costName;

    /**
     * 费用性质
     */
    private String costCategory;

    /**
     * 原始金额
     */
    private BigDecimal originalAmount;

    /**
     * 目标类型(EMPLOYEE/DEPT/ORG)
     */
    private String targetType;

    /**
     * 目标编码
     */
    private String targetCode;

    /**
     * 目标名称
     */
    private String targetName;

    /**
     * 所属部门
     */
    private String deptCode;

    /**
     * 所属机构
     */
    private String orgCode;

    /**
     * 分摊金额
     */
    private BigDecimal allocatedAmount;

    /**
     * 分摊方法
     */
    private String allocationMethod;

    /**
     * 分摊因子
     */
    private String allocationFactor;

    /**
     * 因子值
     */
    private BigDecimal factorValue;

    /**
     * 因子占比
     */
    private BigDecimal factorRatio;

    /**
     * 计算详情(JSON)
     */
    private String calcDetails;

    /**
     * 状态(CALCULATED-已计算/CONFIRMED-已确认)
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
