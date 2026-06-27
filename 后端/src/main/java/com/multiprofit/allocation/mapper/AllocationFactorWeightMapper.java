package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.AllocationFactorWeight;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分摊因子权重Mapper
 */
@Mapper
public interface AllocationFactorWeightMapper extends BaseMapper<AllocationFactorWeight> {

    /**
     * 根据规则ID查询权重配置
     */
    @Select("SELECT * FROM allocation_factor_weight WHERE rule_id = #{ruleId}")
    List<AllocationFactorWeight> selectByRuleId(@Param("ruleId") Long ruleId);

    /**
     * 删除规则的所有权重配置
     */
    @Select("DELETE FROM allocation_factor_weight WHERE rule_id = #{ruleId}")
    int deleteByRuleId(@Param("ruleId") Long ruleId);
}
