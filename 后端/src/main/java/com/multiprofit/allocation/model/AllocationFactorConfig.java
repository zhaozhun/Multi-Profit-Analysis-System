package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分摊因子配置实体
 */
@Data
@TableName("allocation_factor_config")
public class AllocationFactorConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 因子编码
     */
    private String factorCode;

    /**
     * 因子名称
     */
    private String factorName;

    /**
     * 因子类型(VOLUME/REVENUE/HEADCOUNT/AREA/ASSET/CUSTOM)
     */
    private String factorType;

    /**
     * 数据来源(表名.字段名或SQL)
     */
    private String dataSource;

    /**
     * 计算公式
     */
    private String calcFormula;

    /**
     * 因子描述
     */
    private String description;

    /**
     * 适用的成本类型列表(JSON格式)
     */
    private String applicableCostTypes;

    /**
     * 单位
     */
    private String unit;

    /**
     * 精度(小数位数)
     */
    private Integer precisionVal;

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
