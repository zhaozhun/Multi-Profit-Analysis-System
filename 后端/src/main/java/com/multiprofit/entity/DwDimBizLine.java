package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_biz_line")
public class DwDimBizLine {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String lineCode;
    private String lineName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
