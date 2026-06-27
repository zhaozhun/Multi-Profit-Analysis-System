package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工工时记录实体
 */
@Data
@TableName("employee_work_hours")
public class EmployeeWorkHours {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工编码
     */
    private String employeeCode;

    /**
     * 期间(YYYY-MM)
     */
    private String period;

    /**
     * 出勤天数
     */
    private BigDecimal workDays;

    /**
     * 实际工时
     */
    private BigDecimal workHours;

    /**
     * 加班工时
     */
    private BigDecimal overtimeHours;

    /**
     * 总工时(含加班)
     */
    private BigDecimal totalHours;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
