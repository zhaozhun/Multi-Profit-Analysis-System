package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_organization")
public class DwDimOrganization {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orgCode;
    private String orgName;
    private Long parentId;
    private Integer level;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
