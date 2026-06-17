package com.multiprofit.controller;

import com.multiprofit.ai.ClaudeClient;
import com.multiprofit.ai.ReportGenerator;
import com.multiprofit.dto.AiChatRequest;
import com.multiprofit.dto.AiChatResponse;
import com.multiprofit.dto.ApiResponse;
import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.service.AiService;
import com.multiprofit.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ReportGenerator reportGenerator;

    @Autowired
    private ClaudeClient claudeClient;

    /**
     * AI自然语言问答
     */
    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return ApiResponse.ok(aiService.chat(request));
    }

    /**
     * AI生成经营简报
     */
    @PostMapping("/brief")
    public ApiResponse<String> generateBrief(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "全行") String scope) {
        return ApiResponse.ok(aiService.generateBusinessBrief(period, scope));
    }

    /**
     * NL2SQL - 自然语言转SQL
     */
    @PostMapping("/nl2sql")
    public ApiResponse<String> nl2sql(@RequestBody AiChatRequest request) {
        return ApiResponse.ok(aiService.naturalLanguageToSql(request.getMessage()));
    }

    /**
     * 导出Excel报表
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            DashboardDTO dashboard = dashboardService.getDashboardData(startDate, endDate, caliberType, null);
            byte[] excel = reportGenerator.generateExcelReport(dashboard, startDate + "~" + endDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "经营报表_" + startDate + "_" + endDate + ".xlsx");

            return ResponseEntity.ok().headers(headers).body(excel);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * AI健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        if (claudeClient.isAvailable()) {
            return ApiResponse.ok("AI服务正常（Claude API已接入）");
        } else {
            return ApiResponse.ok("AI服务Mock模式（请配置ANTHROPIC_API_KEY）");
        }
    }
}
