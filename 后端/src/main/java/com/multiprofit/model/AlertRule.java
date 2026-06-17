package com.multiprofit.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertRule {
    private Long id;
    private String name;
    private String alertType;
    private String metricCode;
    private BigDecimal threshold;
    private String thresholdType;
    private String level;
    private Integer status;
    private LocalDateTime createTime;
}
