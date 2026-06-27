package com.multiprofit.allocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.allocation.model.AllocationBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分摊批次Mapper
 */
@Mapper
public interface AllocationBatchMapper extends BaseMapper<AllocationBatch> {

    /**
     * 根据期间查询批次
     */
    @Select("SELECT * FROM allocation_batch WHERE period = #{period} ORDER BY created_at DESC")
    List<AllocationBatch> selectByPeriod(@Param("period") String period);

    /**
     * 根据状态查询批次
     */
    @Select("SELECT * FROM allocation_batch WHERE status = #{status} ORDER BY created_at DESC")
    List<AllocationBatch> selectByStatus(@Param("status") String status);

    /**
     * 查询最近的批次
     */
    @Select("SELECT * FROM allocation_batch WHERE period = #{period} AND cost_type = #{costType} ORDER BY created_at DESC LIMIT 1")
    AllocationBatch selectLatestBatch(@Param("period") String period, @Param("costType") String costType);

    /**
     * 根据批次号查询
     */
    @Select("SELECT * FROM allocation_batch WHERE batch_no = #{batchNo}")
    AllocationBatch selectByBatchNo(@Param("batchNo") String batchNo);
}
