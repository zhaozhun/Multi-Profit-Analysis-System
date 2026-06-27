package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.AllocationRuleConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 分摊规则配置Mapper
 */
@Mapper
public interface AllocationRuleConfigMapper extends BaseMapper<AllocationRuleConfig> {

    /**
     * 根据成本类型查询启用的规则
     */
    @Select("SELECT * FROM allocation_rule_config WHERE cost_type = #{costType} AND status = 'ACTIVE' ORDER BY priority")
    List<AllocationRuleConfig> selectByCostType(@Param("costType") String costType);

    /**
     * 查询指定日期有效的规则
     */
    @Select("SELECT * FROM allocation_rule_config WHERE status = 'ACTIVE' " +
            "AND (effective_date IS NULL OR effective_date <= #{date}) " +
            "AND (expire_date IS NULL OR expire_date >= #{date}) " +
            "ORDER BY priority")
    List<AllocationRuleConfig> selectEffectiveRules(@Param("date") LocalDate date);

    /**
     * 根据状态查询规则
     */
    @Select("SELECT * FROM allocation_rule_config WHERE status = #{status} ORDER BY priority, rule_code")
    List<AllocationRuleConfig> selectByStatus(@Param("status") String status);

    /**
     * 查询所有启用的规则
     */
    @Select("SELECT * FROM allocation_rule_config WHERE status = 'ACTIVE' ORDER BY priority, rule_code")
    List<AllocationRuleConfig> selectAllActive();

    /**
     * 查询自动执行的规则
     */
    @Select("SELECT * FROM allocation_rule_config WHERE status = 'ACTIVE' AND auto_execute = 1 ORDER BY priority")
    List<AllocationRuleConfig> selectAutoExecuteRules();
}
