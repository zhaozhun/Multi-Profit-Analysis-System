package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.CostAllocationResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用分摊结果Mapper
 */
@Mapper
public interface CostAllocationResultMapper extends BaseMapper<CostAllocationResult> {
}
