package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 原子指标实体类
 */
@Data
@TableName("atomic_indicator")
public class AtomicIndicator {
    @TableId(type = IdType.INPUT)
    private String code;
    private String name;
    private String businessLine;
    private String sourceTable;
    private String sourceField;
    private String filterCondition;
    private String detailTable;
    private String detailDimension;
    private String detailDisplayFields;
    private String detailGroupBy;
    private String unit;
    private Integer precisionVal;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
