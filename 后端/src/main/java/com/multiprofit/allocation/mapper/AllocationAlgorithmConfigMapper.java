package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.AllocationAlgorithmConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分摊算法配置Mapper
 */
@Mapper
public interface AllocationAlgorithmConfigMapper extends BaseMapper<AllocationAlgorithmConfig> {

    /**
     * 根据算法类型查询
     */
    @Select("SELECT * FROM allocation_algorithm_config WHERE algorithm_type = #{algorithmType} AND status = 'ACTIVE'")
    List<AllocationAlgorithmConfig> selectByAlgorithmType(@Param("algorithmType") String algorithmType);

    /**
     * 查询所有启用的算法
     */
    @Select("SELECT * FROM allocation_algorithm_config WHERE status = 'ACTIVE' ORDER BY algorithm_type, algorithm_code")
    List<AllocationAlgorithmConfig> selectAllActive();

    /**
     * 查询所有内置算法
     */
    @Select("SELECT * FROM allocation_algorithm_config WHERE is_builtin = 1 AND status = 'ACTIVE' ORDER BY algorithm_code")
    List<AllocationAlgorithmConfig> selectAllBuiltin();
}
