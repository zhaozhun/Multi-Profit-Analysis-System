package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.EmployeeMaster;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工主数据Mapper
 */
@Mapper
public interface EmployeeMasterMapper extends BaseMapper<EmployeeMaster> {
}
