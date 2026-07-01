package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 贷款指标明细表
 */
@Data
@TableName("loan_indicator_detail")
public class LoanIndicatorDetail {

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

    /** 在贷余额 */
    private BigDecimal loanBalance;

    /** 贷款利率 */
    private BigDecimal loanRate;

    /** 计息方式 */
    private String loanInterestCalcType;

    /** 当日利息收入 */
    private BigDecimal loanDailyInterest;

    /** 当月利息收入 */
    private BigDecimal loanMonthlyInterest;

    /** 累计利息收入 */
    private BigDecimal loanCumulativeInterest;

    /** FTP利率 */
    private BigDecimal ftpRate;

    /** FTP成本 */
    private BigDecimal ftpCost;

    /** 风险成本 */
    private BigDecimal riskCost;

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
