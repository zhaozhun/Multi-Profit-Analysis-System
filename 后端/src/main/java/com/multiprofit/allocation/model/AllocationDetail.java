package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分摊明细实体
 */
@Data
@TableName("allocation_detail")
public class AllocationDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 期间
     */
    private String period;

    /**
     * 来源维度类型
     */
    private String sourceDimType;

    /**
     * 来源维度编码
     */
    private String sourceDimCode;

    /**
     * 目标维度类型
     */
    private String targetDimType;

    /**
     * 目标维度编码
     */
    private String targetDimCode;

    /**
     * 原始金额
     */
    private BigDecimal originalAmount;

    /**
     * 分摊金额
     */
    private BigDecimal allocatedAmount;

    /**
     * 分摊比例
     */
    private BigDecimal allocationRatio;

    /**
     * 因子值(多因子时为JSON)
     */
    private String factorValues;

    /**
     * 使用的算法编码
     */
    private String algorithmCode;

    /**
     * 计算详情(用于审计)
     */
    private String calcDetails;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
