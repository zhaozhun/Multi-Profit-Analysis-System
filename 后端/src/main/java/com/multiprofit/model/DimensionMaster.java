package com.multiprofit.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DimensionMaster {
    private Long id;
    private String code;
    private String name;
    private String dimType;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;
    private Integer status;
    private String extAttrs;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
