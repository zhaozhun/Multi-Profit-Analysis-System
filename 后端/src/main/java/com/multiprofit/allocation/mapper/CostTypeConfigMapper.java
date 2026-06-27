package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.CostTypeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 成本类型配置Mapper
 */
@Mapper
public interface CostTypeConfigMapper extends BaseMapper<CostTypeConfig> {

    /**
     * 根据状态查询成本类型列表
     */
    @Select("SELECT * FROM cost_type_config WHERE status = #{status} ORDER BY level, cost_type_code")
    List<CostTypeConfig> selectByStatus(@Param("status") String status);

    /**
     * 根据父级编码查询子类型
     */
    @Select("SELECT * FROM cost_type_config WHERE parent_code = #{parentCode} ORDER BY cost_type_code")
    List<CostTypeConfig> selectByParentCode(@Param("parentCode") String parentCode);

    /**
     * 查询所有启用的成本类型
     */
    @Select("SELECT * FROM cost_type_config WHERE status = 'ACTIVE' ORDER BY level, cost_type_code")
    List<CostTypeConfig> selectAllActive();
}
