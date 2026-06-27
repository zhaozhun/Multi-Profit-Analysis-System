package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.AllocationFactorSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 因子快照Mapper
 */
@Mapper
public interface AllocationFactorSnapshotMapper extends BaseMapper<AllocationFactorSnapshot> {

    /**
     * 根据批次ID查询因子快照
     */
    @Select("SELECT * FROM allocation_factor_snapshot WHERE batch_id = #{batchId}")
    List<AllocationFactorSnapshot> selectByBatchId(@Param("batchId") Long batchId);

    /**
     * 根据期间和因子编码查询
     */
    @Select("SELECT * FROM allocation_factor_snapshot WHERE period = #{period} AND factor_code = #{factorCode}")
    List<AllocationFactorSnapshot> selectByPeriodAndFactor(@Param("period") String period, @Param("factorCode") String factorCode);

    /**
     * 查询指定维度的因子值
     */
    @Select("SELECT * FROM allocation_factor_snapshot WHERE period = #{period} AND factor_code = #{factorCode} AND dim_code = #{dimCode}")
    AllocationFactorSnapshot selectByDimCode(@Param("period") String period, @Param("factorCode") String factorCode, @Param("dimCode") String dimCode);

    /**
     * 查询因子汇总
     */
    @Select("SELECT dim_code, factor_value FROM allocation_factor_snapshot WHERE period = #{period} AND factor_code = #{factorCode} AND dim_type = #{dimType}")
    List<Object> selectFactorSummary(@Param("period") String period, @Param("factorCode") String factorCode, @Param("dimType") String dimType);
}
