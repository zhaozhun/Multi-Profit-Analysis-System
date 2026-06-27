package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分摊批次实体
 */
@Data
@TableName("allocation_batch")
public class AllocationBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 期间(YYYY-MM)
     */
    private String period;

    /**
     * 成本类型(为空表示全部)
     */
    private String costType;

    /**
     * 待分摊总金额
     */
    private BigDecimal totalAmount;

    /**
     * 已分摊金额
     */
    private BigDecimal allocatedAmount;

    /**
     * 分摊记录数
     */
    private Integer recordCount;

    /**
     * 状态(PENDING/PROCESSING/COMPLETED/FAILED)
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 触发类型(MANUAL/AUTO/SCHEDULED)
     */
    private String triggerType;

    /**
     * 触发人
     */
    private String triggeredBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
