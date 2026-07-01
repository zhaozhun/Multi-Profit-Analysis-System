package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务利润汇总表
 */
@Data
@TableName("biz_profit_summary")
public class BizProfitSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 期间 */
    private String period;

    /** 业务编号 */
    private String bizId;

    /** 业务类型 */
    private String bizType;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 机构ID */
    private Long orgId;

    /** 机构名称 */
    private String orgName;

    /** 条线ID */
    private Long bizLineId;

    /** 条线名称 */
    private String bizLineName;

    /** 产品ID */
    private Long productId;

    /** 产品名称 */
    private String productName;

    /** 渠道ID */
    private Long channelId;

    /** 渠道名称 */
    private String channelName;

    /** 客户经理ID */
    private Long managerId;

    /** 客户经理名称 */
    private String managerName;

    /** 余额 */
    private BigDecimal balance;

    /** 利息收入 */
    private BigDecimal interestIncome;

    /** FTP成本 */
    private BigDecimal ftpCost;

    /** 风险成本 */
    private BigDecimal riskCost;

    /** 运营成本 */
    private BigDecimal opCost;

    /** 利润 */
    private BigDecimal profit;

    /** 口径 */
    private String caliberType;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
