package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 产品分润规则实体
 */
@Data
@TableName("product_commission_rule")
public class ProductCommissionRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 产品类型
     */
    private String productType;

    /**
     * 产品编码(为空表示该类型全部产品)
     */
    private String productCode;

    /**
     * 分润类型(REVENUE_SHARE/PROFIT_SHARE/FIXED_RATE/TIERED)
     */
    private String commissionType;

    /**
     * 计算基数(REVENUE/PROFIT/BIZ_AMOUNT/BALANCE)
     */
    private String calcBase;

    /**
     * 分润费率
     */
    private BigDecimal rate;

    /**
     * 最低分润金额
     */
    private BigDecimal minAmount;

    /**
     * 最高分润金额
     */
    private BigDecimal maxAmount;

    /**
     * 阶梯配置(用于TIERED类型)
     */
    private String tierConfig;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    private LocalDate expireDate;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
