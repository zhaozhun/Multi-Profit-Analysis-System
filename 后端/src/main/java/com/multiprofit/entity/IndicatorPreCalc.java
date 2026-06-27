package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标预计算结果实体类
 */
@Data
@TableName("indicator_pre_calc")
public class IndicatorPreCalc {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String indicatorCode;
    private String indicatorType;
    private String statType;
    private String calcPeriod;
    private String periodValue;
    private BigDecimal calcValue;
    private LocalDateTime calcTime;
    private Integer status;
}
