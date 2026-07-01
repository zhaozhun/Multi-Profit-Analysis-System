package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dw_indicator_fact")
public class DwIndicatorFact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String indicatorCode;
    private String period;
    private String periodType;
    private String dimType;
    private Long dimId;
    private String dimName;
    private BigDecimal calcValue;
    private String caliberType;
    private LocalDateTime calcTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
