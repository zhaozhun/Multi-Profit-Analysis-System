package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.CostActualRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运营费用实际发生Mapper
 */
@Mapper
public interface OperationCostActualMapper extends BaseMapper<CostActualRecord> {
}
