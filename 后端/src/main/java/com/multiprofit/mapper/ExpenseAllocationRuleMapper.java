package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.ExpenseAllocationRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分摊规则Mapper
 */
@Mapper
public interface ExpenseAllocationRuleMapper extends BaseMapper<ExpenseAllocationRule> {
}
