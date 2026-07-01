package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.dto.DimensionAnalysisDTO;
import com.multiprofit.service.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dimension")
public class DimensionController {

    @Autowired
    private DimensionService dimensionService;

    /**
     * 解析period参数，转换为startDate和endDate
     */
    private String[] parsePeriod(String period) {
        if (period == null || period.isEmpty()) {
            return new String[]{getDefaultStartDate(), getDefaultEndDate()};
        }
        YearMonth ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
        return new String[]{
            ym.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
            ym.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE)
        };
    }

    /**
     * 获取当月第一天
     */
    private String getDefaultStartDate() {
        return YearMonth.now().atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 获取今天日期
     */
    private String getDefaultEndDate() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 获取维度分析页全量数据（支持日期范围或period）
     */
    @GetMapping("/{dimType}/analysis")
    public ApiResponse<DimensionAnalysisDTO> getAnalysis(
            @PathVariable String dimType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Integer level) {
        if (startDate == null && endDate == null && period != null) {
            String[] dates = parsePeriod(period);
            startDate = dates[0];
            endDate = dates[1];
        }
        if (startDate == null) startDate = getDefaultStartDate();
        if (endDate == null) endDate = getDefaultEndDate();
        return ApiResponse.ok(dimensionService.getAnalysisData(dimType, startDate, endDate, caliberType, parentId, level));
    }

    /**
     * 获取树形数据（层级展开）
     */
    @GetMapping("/{dimType}/tree")
    public ApiResponse<List<DimensionAnalysisDTO.TreeNode>> getTree(
            @PathVariable String dimType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false, defaultValue = "0") Long parentId) {
        if (startDate == null && endDate == null && period != null) {
            String[] dates = parsePeriod(period);
            startDate = dates[0];
            endDate = dates[1];
        }
        if (startDate == null) startDate = getDefaultStartDate();
        if (endDate == null) endDate = getDefaultEndDate();
        return ApiResponse.ok(dimensionService.getTreeData(dimType, startDate, endDate, caliberType, parentId));
    }

    /**
     * 获取排名数据
     */
    @GetMapping("/{dimType}/ranking")
    public ApiResponse<List<DimensionAnalysisDTO.RankItem>> getRanking(
            @PathVariable String dimType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false, defaultValue = "net_profit") String rankBy,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        if (startDate == null && endDate == null && period != null) {
            String[] dates = parsePeriod(period);
            startDate = dates[0];
            endDate = dates[1];
        }
        if (startDate == null) startDate = getDefaultStartDate();
        if (endDate == null) endDate = getDefaultEndDate();
        return ApiResponse.ok(dimensionService.getRanking(dimType, startDate, endDate, caliberType, rankBy, limit));
    }

    /**
     * 获取维度详情
     */
    @GetMapping("/{dimType}/detail/{dimId}")
    public ApiResponse<DimensionAnalysisDTO.TableRow> getDetail(
            @PathVariable String dimType,
            @PathVariable Long dimId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        if (startDate == null && endDate == null && period != null) {
            String[] dates = parsePeriod(period);
            startDate = dates[0];
            endDate = dates[1];
        }
        if (startDate == null) startDate = getDefaultStartDate();
        if (endDate == null) endDate = getDefaultEndDate();
        return ApiResponse.ok(dimensionService.getDetail(dimId, dimType, startDate, endDate));
    }

    /**
     * 交叉钻取
     */
    @GetMapping("/cross-drill")
    public ApiResponse<List<DimensionAnalysisDTO.TableRow>> crossDrill(
            @RequestParam String fromDimType,
            @RequestParam String fromDimName,
            @RequestParam String toDimType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        if (startDate == null && endDate == null && period != null) {
            String[] dates = parsePeriod(period);
            startDate = dates[0];
            endDate = dates[1];
        }
        if (startDate == null) startDate = getDefaultStartDate();
        if (endDate == null) endDate = getDefaultEndDate();
        return ApiResponse.ok(dimensionService.crossDrill(fromDimType, fromDimName, toDimType, startDate, endDate, caliberType));
    }

    /**
     * 获取钻取路径
     */
    @GetMapping("/{dimType}/drill-path/{dimId}")
    public ApiResponse<List<Map<String, Object>>> getDrillPath(
            @PathVariable String dimType,
            @PathVariable Long dimId) {
        return ApiResponse.ok(dimensionService.getDrillPath(dimType, dimId));
    }

    /**
     * 获取维度层级
     */
    @GetMapping("/{dimType}/hierarchy")
    public ApiResponse<List<Map<String, Object>>> getDimHierarchy(
            @PathVariable String dimType) {
        return ApiResponse.ok(dimensionService.getDimHierarchy(dimType));
    }
}
