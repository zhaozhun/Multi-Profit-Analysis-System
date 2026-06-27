package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cost_allocation_result")
public class CostAllocationResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String period;
    private String costType;
    private String sourceDimType;
    private String sourceDimCode;
    private String targetAccountId;
    private BigDecimal allocatedAmount;
    private Long allocationRuleId;
    private LocalDateTime createTime;
}
