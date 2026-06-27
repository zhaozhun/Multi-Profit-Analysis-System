package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.OperationCostAllocationResultEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运营费用分摊结果Mapper
 */
@Mapper
public interface OperationCostAllocationResultMapper extends BaseMapper<OperationCostAllocationResultEntity> {
}
