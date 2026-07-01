package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_product")
public class DwDimProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productCode;
    private String productName;
    private String productType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
