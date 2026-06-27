package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分摊算法配置实体
 */
@Data
@TableName("allocation_algorithm_config")
public class AllocationAlgorithmConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 算法编码
     */
    private String algorithmCode;

    /**
     * 算法名称
     */
    private String algorithmName;

    /**
     * 算法类型(RATIO/WEIGHTED/STEP/DIRECT/FORMULA)
     */
    private String algorithmType;

    /**
     * 算法描述
     */
    private String description;

    /**
     * 实现类名(用于插件化)
     */
    private String implementationClass;

    /**
     * 参数定义(JSON Schema格式)
     */
    private String paramDefinition;

    /**
     * 公式模板
     */
    private String formulaTemplate;

    /**
     * 是否内置算法
     */
    private Boolean isBuiltin;

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
