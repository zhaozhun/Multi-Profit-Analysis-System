package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_channel")
public class DwDimChannel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String channelCode;
    private String channelName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
