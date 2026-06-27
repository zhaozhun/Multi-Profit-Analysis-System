package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分摊因子权重配置实体
 */
@Data
@TableName("allocation_factor_weight")
public class AllocationFactorWeight {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 因子编码
     */
    private String factorCode;

    /**
     * 权重(0-1)
     */
    private BigDecimal weight;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
