package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.AllocationDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分摊明细Mapper
 */
@Mapper
public interface AllocationDetailMapper extends BaseMapper<AllocationDetail> {

    /**
     * 根据批次ID查询明细
     */
    @Select("SELECT * FROM allocation_detail WHERE batch_id = #{batchId} ORDER BY allocated_amount DESC")
    List<AllocationDetail> selectByBatchId(@Param("batchId") Long batchId);

    /**
     * 根据期间查询明细
     */
    @Select("SELECT * FROM allocation_detail WHERE period = #{period} ORDER BY created_at DESC")
    List<AllocationDetail> selectByPeriod(@Param("period") String period);

    /**
     * 查询目标维度的分摊汇总
     */
    @Select("SELECT target_dim_code, SUM(allocated_amount) as allocated_amount " +
            "FROM allocation_detail WHERE batch_id = #{batchId} " +
            "GROUP BY target_dim_code ORDER BY allocated_amount DESC")
    List<Object> selectSummaryByBatchId(@Param("batchId") Long batchId);

    /**
     * 查询指定目标维度的分摊明细
     */
    @Select("SELECT * FROM allocation_detail WHERE batch_id = #{batchId} AND target_dim_code = #{targetDimCode}")
    List<AllocationDetail> selectByTargetDim(@Param("batchId") Long batchId, @Param("targetDimCode") String targetDimCode);
}
