package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 运营费用分摊规则实体
 */
@Data
@TableName("operation_cost_allocation_rule")
public class OperationCostAllocationRule {

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
     * 费用类别(FIXED/VARIABLE/DIRECT)
     */
    private String costCategory;

    /**
     * 费用类型(RENT/UTILITIES/WORKSTATION/REIMBURSE/COLLECTION/DATA_FEE/IT_OPS/MARKETING/TRAINING/ADMIN)
     */
    private String costType;

    /**
     * 分摊方法(EMPLOYEE_COUNT/WORK_HOURS/SALARY/AREA/BIZ_VOLUME/DEPT_DIRECT/CUSTOM)
     */
    private String allocationMethod;

    /**
     * 分摊因子配置(JSON)
     */
    private String allocationFactor;

    /**
     * 分摊范围(ALL/DEPT/TEAM)
     */
    private String targetScope;

    /**
     * 目标部门编码列表(JSON)
     */
    private String targetDeptCodes;

    /**
     * 默认金额(用于预算)
     */
    private String defaultAmount;

    /**
     * 自定义计算公式
     */
    private String calcFormula;

    /**
     * 费用描述
     */
    private String description;

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
