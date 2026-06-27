package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工主数据实体
 */
@Data
@TableName("employee_master")
public class EmployeeMaster {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工编码
     */
    private String employeeCode;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 所属机构编码
     */
    private String orgCode;

    /**
     * 所属机构名称
     */
    private String orgName;

    /**
     * 所属部门编码
     */
    private String deptCode;

    /**
     * 所属部门名称
     */
    private String deptName;

    /**
     * 职位
     */
    private String position;

    /**
     * 职级
     */
    private String jobLevel;

    /**
     * 入职日期
     */
    private LocalDate entryDate;

    /**
     * 月薪
     */
    private BigDecimal salary;

    /**
     * 月标准工时
     */
    private BigDecimal workHoursPerMonth;

    /**
     * 工位面积(平方米)
     */
    private BigDecimal workstationArea;

    /**
     * 状态(ACTIVE/INACTIVE)
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
