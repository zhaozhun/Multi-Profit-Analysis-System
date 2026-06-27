package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.CostActualRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用实际发生记录Mapper
 */
@Mapper
public interface CostActualRecordMapper extends BaseMapper<CostActualRecord> {
}
