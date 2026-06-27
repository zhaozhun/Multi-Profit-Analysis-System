package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.CostAllocationRuleConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用分摊规则配置Mapper
 */
@Mapper
public interface CostAllocationRuleConfigMapper extends BaseMapper<CostAllocationRuleConfig> {
}
