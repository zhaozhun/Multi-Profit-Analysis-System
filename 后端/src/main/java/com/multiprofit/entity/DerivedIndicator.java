package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 派生指标实体类
 */
@Data
@TableName("derived_indicator")
public class DerivedIndicator {
    @TableId(type = IdType.INPUT)
    private String code;
    private String name;
    private String businessLine;
    private String calcFormula;
    private String formulaVars;
    private String unit;
    private Integer precisionVal;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
