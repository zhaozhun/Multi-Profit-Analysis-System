package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分摊因子表
 */
@Data
@TableName("allocation_factor")
public class AllocationFactor {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 因子编码 */
    private String factorCode;

    /** 因子名称 */
    private String factorName;

    /** 因子类型 */
    private String factorType;

    /** 数据来源表 */
    private String sourceTable;

    /** 数据来源字段 */
    private String sourceField;

    /** 计算公式 */
    private String calcFormula;

    /** 描述 */
    private String description;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
