package com.multiprofit.model;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 业务明细台账（星型模型）
 *
 * 利润计算规则（按产品类型区分）：
 * - 贷款(LOAN)：净利润 = 对客利息收入 - FTP成本 - 风险成本 - 运营成本
 * - 存款(DEPOSIT)：净利润 = FTP收入 - 对客利息支出 - 运营成本
 *
 * 维度字段使用ID外键关联dimension_master表，通过JOIN查询获取维度名称
 */
@Data
public class BizLedger {

    private String bizId;
    private String statDate;
    private String accountPeriod;

    // 维度外键（关联 dimension_master.id）
    private Long orgId;          // 机构ID
    private Long productId;      // 产品ID
    private Long bizLineId;      // 条线ID
    private Long deptId;         // 部门ID
    private Long channelId;      // 渠道ID
    private Long managerId;      // 客户经理ID
    private Long customerId;     // 客户ID（关联 customer_master.id）

    // 产品类型（冗余字段，用于快速筛选）
    private String productType;  // LOAN-贷款 DEPOSIT-存款

    // 金额指标
    private BigDecimal bizAmount;           // 业务金额/余额
    private BigDecimal revenue;             // 业务总收入
    private BigDecimal interestIncome;      // 利息收入（贷款：对客利息收入；存款：FTP收入）
    private BigDecimal interestExpense;     // 利息支出（存款：对客利息支出）
    private BigDecimal feeIncome;           // 手续费收入
    private BigDecimal nonInterestIncome;   // 非息收入

    // 成本指标
    private BigDecimal ftpCost;             // FTP成本（贷款）
    private BigDecimal riskCost;            // 风险成本（仅贷款）
    private BigDecimal opCost;              // 运营成本

    // 利润
    private BigDecimal netProfit;           // 净利润

    // 贷款/存款拆分（冗余字段，用于快速聚合）
    private BigDecimal loanRevenue;
    private BigDecimal loanFtpCost;
    private BigDecimal loanRiskCost;
    private BigDecimal loanOpCost;
    private BigDecimal loanProfit;
    private BigDecimal depositRevenue;
    private BigDecimal depositInterest;
    private BigDecimal depositOpCost;
    private BigDecimal depositProfit;

    // 口径
    private String caliberType;             // BOOK-账面 ASSESS-考核
    private String currency;

    /**
     * 计算净利润（根据产品类型）
     */
    public void calculateNetProfit() {
        if ("DEPOSIT".equals(productType)) {
            // 存款：净利润 = FTP收入 - 对客利息支出 - 运营成本
            BigDecimal ftpIncome = interestIncome != null ? interestIncome : BigDecimal.ZERO;
            BigDecimal custInterest = interestExpense != null ? interestExpense : BigDecimal.ZERO;
            BigDecimal op = opCost != null ? opCost : BigDecimal.ZERO;
            this.netProfit = ftpIncome.subtract(custInterest).subtract(op);
        } else if ("LOAN".equals(productType)) {
            // 贷款：净利润 = 对客利息收入 - FTP成本 - 风险成本 - 运营成本
            BigDecimal income = interestIncome != null ? interestIncome : BigDecimal.ZERO;
            BigDecimal ftp = ftpCost != null ? ftpCost : BigDecimal.ZERO;
            BigDecimal risk = riskCost != null ? riskCost : BigDecimal.ZERO;
            BigDecimal op = opCost != null ? opCost : BigDecimal.ZERO;
            this.netProfit = income.subtract(ftp).subtract(risk).subtract(op);
        }
    }
}
