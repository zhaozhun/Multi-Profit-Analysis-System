package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.AllocationFactorConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分摊因子配置Mapper
 */
@Mapper
public interface AllocationFactorConfigMapper extends BaseMapper<AllocationFactorConfig> {

    /**
     * 根据因子类型查询
     */
    @Select("SELECT * FROM allocation_factor_config WHERE factor_type = #{factorType} AND status = 'ACTIVE'")
    List<AllocationFactorConfig> selectByFactorType(@Param("factorType") String factorType);

    /**
     * 查询所有启用的因子
     */
    @Select("SELECT * FROM allocation_factor_config WHERE status = 'ACTIVE' ORDER BY factor_type, factor_code")
    List<AllocationFactorConfig> selectAllActive();

    /**
     * 根据适用成本类型查询因子
     */
    @Select("SELECT * FROM allocation_factor_config WHERE status = 'ACTIVE' AND JSON_CONTAINS(applicable_cost_types, JSON_QUOTE(#{costType}))")
    List<AllocationFactorConfig> selectByCostType(@Param("costType") String costType);
}
