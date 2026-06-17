package com.multiprofit.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertRecord {
    private Long id;
    private Long ruleId;
    private String level;
    private String content;
    private String dimType;
    private Long dimId;
    private String dimName;
    private BigDecimal anomalyValue;
    private String aiAnalysis;
    private String status;
    private String handler;
    private String handleNote;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
