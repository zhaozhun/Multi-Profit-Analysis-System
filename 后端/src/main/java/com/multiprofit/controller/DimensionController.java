package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.dto.DimensionAnalysisDTO;
import com.multiprofit.service.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dimension")
public class DimensionController {

    @Autowired
    private DimensionService dimensionService;

    /**
     * 获取维度分析页全量数据（支持日期范围）
     */
    @GetMapping("/{dimType}/analysis")
    public ApiResponse<DimensionAnalysisDTO> getAnalysis(
            @PathVariable String dimType,
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Integer level) {
        return ApiResponse.ok(dimensionService.getAnalysisData(dimType, startDate, endDate, caliberType, parentId, level));
    }

    /**
     * 获取树形数据（层级展开）
     */
    @GetMapping("/{dimType}/tree")
    public ApiResponse<List<DimensionAnalysisDTO.TreeNode>> getTree(
            @PathVariable String dimType,
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false, defaultValue = "0") Long parentId) {
        return ApiResponse.ok(dimensionService.getTreeData(dimType, startDate, endDate, caliberType, parentId));
    }

    /**
     * 获取排名数据
     */
    @GetMapping("/{dimType}/ranking")
    public ApiResponse<List<DimensionAnalysisDTO.RankItem>> getRanking(
            @PathVariable String dimType,
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false, defaultValue = "net_profit") String rankBy,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ApiResponse.ok(dimensionService.getRanking(dimType, startDate, endDate, caliberType, rankBy, limit));
    }

    /**
     * 获取单个主体详情
     */
    @GetMapping("/{dimType}/detail/{dimId}")
    public ApiResponse<DimensionAnalysisDTO.TableRow> getDetail(
            @PathVariable String dimType,
            @PathVariable Long dimId,
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate) {
        return ApiResponse.ok(dimensionService.getDetail(dimId, dimType, startDate, endDate));
    }

    /**
     * 交叉维度钻取（扁平列表）
     */
    @GetMapping("/cross-drill")
    public ApiResponse<DimensionAnalysisDTO.CrossDimData> crossDrill(
            @RequestParam String fromDimType,
            @RequestParam String fromDimName,
            @RequestParam String toDimType,
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {

        List<DimensionAnalysisDTO.TableRow> rows = dimensionService.crossDrill(
            fromDimType, fromDimName, toDimType, startDate, endDate, caliberType
        );

        DimensionAnalysisDTO.CrossDimData crossData = new DimensionAnalysisDTO.CrossDimData();
        crossData.setCrossDimType(toDimType);
        crossData.setCrossDimLabel(getDimLabel(toDimType));
        crossData.setFromDimName(fromDimName);

        List<DimensionAnalysisDTO.CrossDimRow> crossRows = rows.stream().map(r -> {
            DimensionAnalysisDTO.CrossDimRow cr = new DimensionAnalysisDTO.CrossDimRow();
            cr.setName(r.getName());
            cr.setRevenue(r.getRevenue());
            cr.setNetProfit(r.getNetProfit());
            cr.setLoanProfit(r.getLoanProfit());
            cr.setDepositProfit(r.getDepositProfit());
            cr.setCostIncomeRatio(r.getCostIncomeRatio());
            cr.setProfitStatus(r.getProfitStatus());
            return cr;
        }).toList();

        crossData.setRows(crossRows);
        return ApiResponse.ok(crossData);
    }

    /**
     * 交叉维度钻取（树状结构）
     */
    @GetMapping("/cross-drill-tree")
    public ApiResponse<List<Map<String, Object>>> crossDrillTree(
            @RequestParam String fromDimType,
            @RequestParam Long fromDimId,
            @RequestParam String toDimType,
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        return ApiResponse.ok(dimensionService.crossDrillTree(
            fromDimType, fromDimId, toDimType, startDate, endDate, caliberType
        ));
    }

    /**
     * 获取钻取路径（面包屑）
     */
    @GetMapping("/{dimType}/drill-path/{dimId}")
    public ApiResponse<List<Map<String, Object>>> getDrillPath(
            @PathVariable String dimType,
            @PathVariable Long dimId) {
        return ApiResponse.ok(dimensionService.getDrillPath(dimType, dimId));
    }

    /**
     * 获取维度层级结构
     */
    @GetMapping("/{dimType}/hierarchy")
    public ApiResponse<List<Map<String, Object>>> getHierarchy(@PathVariable String dimType) {
        return ApiResponse.ok(dimensionService.getDimHierarchy(dimType));
    }

    private String getDimLabel(String dimType) {
        return switch (dimType) {
            case "ORG" -> "机构";
            case "BIZ_LINE" -> "条线";
            case "DEPT" -> "部门";
            case "PRODUCT" -> "产品";
            case "CHANNEL" -> "渠道";
            case "MANAGER" -> "客户经理";
            case "CUSTOMER" -> "客户";
            default -> "维度";
        };
    }
}
