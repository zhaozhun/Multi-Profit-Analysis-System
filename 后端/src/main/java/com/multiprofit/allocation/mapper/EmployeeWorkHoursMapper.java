package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.EmployeeWorkHours;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工工时记录Mapper
 */
@Mapper
public interface EmployeeWorkHoursMapper extends BaseMapper<EmployeeWorkHours> {
}
