package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 存款指标明细表
 */
@Data
@TableName("deposit_indicator_detail")
public class DepositIndicatorDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务编号 */
    private String bizId;

    /** 统计日期 */
    private LocalDate statDate;

    /** 账期月份 */
    private String accountPeriod;

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

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

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

    /** 存款余额 */
    private BigDecimal depositBalance;

    /** 存款利率 */
    private BigDecimal depositRate;

    /** 计息方式 */
    private String depositInterestCalcType;

    /** 当日利息支出 */
    private BigDecimal depositDailyInterest;

    /** 当月利息支出 */
    private BigDecimal depositMonthlyInterest;

    /** 累计利息支出 */
    private BigDecimal depositCumulativeInterest;

    /** FTP利率 */
    private BigDecimal ftpRate;

    /** FTP收入 */
    private BigDecimal ftpIncome;

    /** 运营成本 */
    private BigDecimal opCost;

    /** 费用类型 */
    private String expenseType;

    /** 口径 */
    private String caliberType;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
