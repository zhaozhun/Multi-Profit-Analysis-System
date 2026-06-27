package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工费用分摊结果实体
 */
@Data
@TableName("employee_cost_allocation")
public class EmployeeCostAllocation {

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
     * 员工编码
     */
    private String employeeCode;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 所属机构
     */
    private String orgCode;

    /**
     * 所属部门
     */
    private String deptCode;

    /**
     * 成本类型
     */
    private String costType;

    /**
     * 成本类型名称
     */
    private String costTypeName;

    /**
     * 原始金额
     */
    private BigDecimal originalAmount;

    /**
     * 分摊金额
     */
    private BigDecimal allocatedAmount;

    /**
     * 分摊因子
     */
    private String allocationFactor;

    /**
     * 因子值
     */
    private BigDecimal factorValue;

    /**
     * 因子占比
     */
    private BigDecimal factorRatio;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
