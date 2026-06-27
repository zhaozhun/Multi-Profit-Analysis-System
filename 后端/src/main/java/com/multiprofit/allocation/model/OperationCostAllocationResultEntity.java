package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运营费用分摊结果实体
 */
@Data
@TableName("operation_cost_allocation_result")
public class OperationCostAllocationResultEntity {

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
     * 费用类型
     */
    private String costType;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

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
     * 分摊金额
     */
    private BigDecimal allocatedAmount;

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
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
