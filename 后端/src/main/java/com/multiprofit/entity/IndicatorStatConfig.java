package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 统计口径配置实体类
 */
@Data
@TableName("indicator_stat_config")
public class IndicatorStatConfig {
    @TableId(type = IdType.INPUT)
    private String indicatorCode;
    private String statType;
    private String calcFormula;
    private String description;
}
