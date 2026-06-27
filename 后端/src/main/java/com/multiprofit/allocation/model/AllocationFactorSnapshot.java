package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 因子快照实体
 */
@Data
@TableName("allocation_factor_snapshot")
public class AllocationFactorSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 期间
     */
    private String period;

    /**
     * 因子编码
     */
    private String factorCode;

    /**
     * 维度类型
     */
    private String dimType;

    /**
     * 维度编码
     */
    private String dimCode;

    /**
     * 因子值
     */
    private BigDecimal factorValue;

    /**
     * 因子占比
     */
    private BigDecimal factorRatio;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 快照时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime snapshotTime;
}
