package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 成本类型配置实体
 */
@Data
@TableName("cost_type_config")
public class CostTypeConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 成本类型编码
     */
    private String costTypeCode;

    /**
     * 成本类型名称
     */
    private String costTypeName;

    /**
     * 父级编码(支持层级)
     */
    private String parentCode;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 描述
     */
    private String description;

    /**
     * 默认分摊算法
     */
    private String defaultAlgorithm;

    /**
     * 默认分摊因子
     */
    private String defaultFactor;

    /**
     * 会计科目编码
     */
    private String accountingCode;

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
