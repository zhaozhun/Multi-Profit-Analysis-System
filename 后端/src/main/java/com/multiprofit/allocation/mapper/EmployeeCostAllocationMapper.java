package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.EmployeeCostAllocation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工费用分摊Mapper
 */
@Mapper
public interface EmployeeCostAllocationMapper extends BaseMapper<EmployeeCostAllocation> {
}
