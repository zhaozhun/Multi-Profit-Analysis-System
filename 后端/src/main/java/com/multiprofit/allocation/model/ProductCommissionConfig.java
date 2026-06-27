package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 产品分润配置实体
 */
@Data
@TableName("product_commission_config")
public class ProductCommissionConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品类型
     */
    private String productType;

    /**
     * 是否需要分润(0-否,1-是)
     */
    private Boolean needCommission;

    /**
     * 分润类型(REVENUE_SHARE/PROFIT_SHARE/BALANCE_SHARE)
     */
    private String commissionType;

    /**
     * 计算基数(INTEREST_INCOME/LOAN_BALANCE/FEE_INCOME/NET_PROFIT)
     */
    private String calcBase;

    /**
     * 分润费率
     */
    private BigDecimal commissionRate;

    /**
     * 最低分润金额
     */
    private BigDecimal minCommission;

    /**
     * 最高分润金额
     */
    private BigDecimal maxCommission;

    /**
     * 接收方类型(BRANCH/DEPT/PERSON)
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
     * 备注
     */
    private String remark;

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
