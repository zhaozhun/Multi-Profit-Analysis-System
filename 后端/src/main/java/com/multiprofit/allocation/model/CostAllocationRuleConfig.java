package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用分摊规则配置实体
 */
@Data
@TableName("cost_allocation_rule_config")
public class CostAllocationRuleConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 费用编码
     */
    private String costCode;

    /**
     * 费用名称
     */
    private String costName;

    /**
     * 分摊方法
     */
    private String allocationMethod;

    /**
     * 分摊因子
     */
    private String allocationFactor;

    /**
     * 因子数据来源
     */
    private String factorSource;

    /**
     * 因子计算公式
     */
    private String factorFormula;

    /**
     * 分摊目标类型(EMPLOYEE-员工/DEPT-部门/ORG-机构)
     */
    private String targetType;

    /**
     * 分摊范围(ALL-全部/DEPT-指定部门/TEAM-指定团队)
     */
    private String targetScope;

    /**
     * 目标部门编码列表(JSON)
     */
    private String targetDeptCodes;

    /**
     * 权重配置(多因子时)(JSON)
     */
    private String weightConfig;

    /**
     * 最低分摊金额
     */
    private BigDecimal minAmount;

    /**
     * 最高分摊金额
     */
    private BigDecimal maxAmount;

    /**
     * 精度(小数位数)
     */
    private Integer precisionVal;

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
