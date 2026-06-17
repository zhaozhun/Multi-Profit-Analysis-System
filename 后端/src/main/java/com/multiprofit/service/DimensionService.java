package com.multiprofit.service;

import com.multiprofit.dto.DimensionAnalysisDTO;
import java.util.List;
import java.util.Map;

/**
 * 维度分析服务接口（支持日期范围+树形层级+交叉钻取）
 */
public interface DimensionService {

    /**
     * 获取维度分析页全量数据
     */
    DimensionAnalysisDTO getAnalysisData(String dimType, String startDate, String endDate,
                                          String caliberType, Long parentId, Integer level);

    /**
     * 获取维度树形数据（层级展开）
     */
    List<DimensionAnalysisDTO.TreeNode> getTreeData(String dimType, String startDate, String endDate,
                                                     String caliberType, Long parentId);

    /**
     * 获取排名数据
     */
    List<DimensionAnalysisDTO.RankItem> getRanking(String dimType, String startDate, String endDate,
                                                    String caliberType, String rankBy, int limit);

    /**
     * 获取单个主体的利润明细
     */
    DimensionAnalysisDTO.TableRow getDetail(Long dimId, String dimType, String startDate, String endDate);

    /**
     * 交叉维度钻取（扁平列表）
     */
    List<DimensionAnalysisDTO.TableRow> crossDrill(String fromDimType, String fromDimName,
                                                    String toDimType, String startDate, String endDate,
                                                    String caliberType);

    /**
     * 交叉维度钻取（树状结构）
     */
    List<Map<String, Object>> crossDrillTree(String fromDimType, Long fromDimId,
                                              String toDimType, String startDate, String endDate,
                                              String caliberType);

    /**
     * 获取钻取路径（面包屑）
     */
    List<Map<String, Object>> getDrillPath(String dimType, Long currentId);

    /**
     * 获取维度层级结构
     */
    List<Map<String, Object>> getDimHierarchy(String dimType);
}
