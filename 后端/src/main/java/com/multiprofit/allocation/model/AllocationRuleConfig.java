package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分摊规则配置实体
 */
@Data
@TableName("allocation_rule_config")
public class AllocationRuleConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 成本类型
     */
    private String costType;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 优先级(数值越小优先级越高)
     */
    private Integer priority;

    /**
     * 来源维度类型
     */
    private String sourceDimType;

    /**
     * 来源维度编码(为空表示全部)
     */
    private String sourceDimCode;

    /**
     * 目标维度类型
     */
    private String targetDimType;

    /**
     * 目标维度过滤条件
     */
    private String targetDimFilter;

    /**
     * 算法编码
     */
    private String algorithmCode;

    /**
     * 算法参数(JSON格式)
     */
    private String algorithmParams;

    /**
     * 分摊周期类型(MONTHLY/QUARTERLY/YEARLY/ON_DEMAND)
     */
    private String periodType;

    /**
     * 是否自动执行
     */
    private Boolean autoExecute;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    private LocalDate expireDate;

    /**
     * 状态(ACTIVE/INACTIVE/DRAFT)
     */
    private String status;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
