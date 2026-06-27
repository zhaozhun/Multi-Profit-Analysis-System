package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.OperationCostAllocationRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运营费用分摊规则Mapper
 */
@Mapper
public interface OperationCostAllocationRuleMapper extends BaseMapper<OperationCostAllocationRule> {
}
