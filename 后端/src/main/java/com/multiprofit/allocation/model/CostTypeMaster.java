package com.multiprofit.allocation.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 费用类型主表实体
 * 支持3级层级结构
 */
@Data
@TableName("cost_type_master")
public class CostTypeMaster {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 费用编码
     */
    private String costCode;

    /**
     * 费用名称
     */
    private String costName;

    /**
     * 父级编码(支持3级层级)
     */
    private String parentCode;

    /**
     * 层级(1-大类,2-中类,3-小类)
     */
    private Integer level;

    /**
     * 费用性质(FIXED-固定/VARIABLE-变动/DIRECT-直接)
     */
    private String costCategory;

    /**
     * 费用归属(OPERATION-运营/MANAGEMENT-管理/SALES-销售/FINANCE-财务)
     */
    private String costNature;

    /**
     * 是否需要分摊(0-否,1-是)
     */
    private Boolean allocationRequired;

    /**
     * 默认分摊方法
     */
    private String allocationMethod;

    /**
     * 默认分摊因子配置(JSON)
     */
    private String allocationFactor;

    /**
     * 会计科目编码
     */
    private String accountingCode;

    /**
     * 会计科目名称
     */
    private String accountingName;

    /**
     * 是否预算控制(0-否,1-是)
     */
    private Boolean budgetControl;

    /**
     * 费用描述
     */
    private String description;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序
     */
    private Integer sortOrder;

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
