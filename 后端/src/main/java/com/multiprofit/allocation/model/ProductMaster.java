package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品主数据实体
 */
@Data
@TableName("product_master")
public class ProductMaster {

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
     * 产品类型(存款/贷款/理财/中间业务)
     */
    private String productType;

    /**
     * 父级产品编码
     */
    private String parentCode;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 产品经理
     */
    private String productManager;

    /**
     * 利润中心
     */
    private String profitCenter;

    /**
     * 产品等级(A/B/C/D)
     */
    private String productLevel;

    /**
     * 分润费率
     */
    private BigDecimal commissionRate;

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
