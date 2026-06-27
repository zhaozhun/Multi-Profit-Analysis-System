package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品收入数据实体
 */
@Data
@TableName("product_income_data")
public class ProductIncomeData {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 期间(YYYY-MM)
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
     * 产品类型
     */
    private String productType;

    /**
     * 利息收入
     */
    private BigDecimal interestIncome;

    /**
     * 手续费收入
     */
    private BigDecimal feeIncome;

    /**
     * 总收入
     */
    private BigDecimal totalRevenue;

    /**
     * 贷款余额
     */
    private BigDecimal loanBalance;

    /**
     * 业务量(笔数)
     */
    private BigDecimal bizAmount;

    /**
     * 净利润
     */
    private BigDecimal netProfit;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
