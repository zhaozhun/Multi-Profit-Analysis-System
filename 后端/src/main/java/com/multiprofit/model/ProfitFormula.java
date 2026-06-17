package com.multiprofit.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProfitFormula {
    private Long id;
    private String name;
    private String code;
    private String expression;
    private String caliberType;
    private String version;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
