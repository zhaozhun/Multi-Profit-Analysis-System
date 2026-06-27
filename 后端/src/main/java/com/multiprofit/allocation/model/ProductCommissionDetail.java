package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品分润明细实体
 */
@Data
@TableName("product_commission_detail")
public class ProductCommissionDetail {

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
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 计算基数类型
     */
    private String calcBase;

    /**
     * 基数金额
     */
    private BigDecimal baseAmount;

    /**
     * 分润费率
     */
    private BigDecimal commissionRate;

    /**
     * 分润金额
     */
    private BigDecimal commissionAmount;

    /**
     * 接收方类型
     */
    private String receiverType;

    /**
     * 接收方编码
     */
    private String receiverCode;

    /**
     * 接收方名称
     */
    private String receiverName;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
