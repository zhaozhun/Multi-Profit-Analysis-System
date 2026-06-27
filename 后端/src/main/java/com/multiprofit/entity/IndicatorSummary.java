package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("indicator_summary")
public class IndicatorSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String period;
    private String indicatorCode;
    private String indicatorType;
    private String businessLine;
    private BigDecimal calcValue;
    private LocalDateTime calcTime;
    private Integer status;
}
