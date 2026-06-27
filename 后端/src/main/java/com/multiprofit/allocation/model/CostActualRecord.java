package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用实际发生记录实体
 */
@Data
@TableName("cost_actual_record")
public class CostActualRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 期间(YYYY-MM)
     */
    private String period;

    /**
     * 费用编码
     */
    private String costCode;

    /**
     * 费用名称
     */
    private String costName;

    /**
     * 费用类型(大类)
     */
    private String costType;

    /**
     * 实际金额
     */
    private BigDecimal amount;

    /**
     * 归属部门(直接归属时)
     */
    private String deptCode;

    /**
     * 归属机构
     */
    private String orgCode;

    /**
     * 供应商
     */
    private String vendor;

    /**
     * 发票号
     */
    private String invoiceNo;

    /**
     * 发生日期
     */
    private LocalDate occurrenceDate;

    /**
     * 费用说明
     */
    private String description;

    /**
     * 附件数量
     */
    private Integer attachmentCount;

    /**
     * 状态(PENDING-待分摊/ALLOCATED-已分摊/CONFIRMED-已确认)
     */
    private String status;

    /**
     * 录入人
     */
    private String createdBy;

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
