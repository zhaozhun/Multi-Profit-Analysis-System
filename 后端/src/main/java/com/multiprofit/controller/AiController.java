package com.multiprofit.controller;

import com.multiprofit.agent.*;
import com.multiprofit.ai.ModelApiClient;
import com.multiprofit.ai.ReportGenerator;
import com.multiprofit.dto.AiChatRequest;
import com.multiprofit.dto.AiChatResponse;
import com.multiprofit.dto.ApiResponse;
import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.service.AiService;
import com.multiprofit.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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
    private ModelApiClient claudeClient;

    @Autowired
    private AgentExecutor agentExecutor;

    @Autowired
    private AgentRouter agentRouter;

    @Autowired
    private AgentConfigLoader configLoader;

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

    /**
     * Agent对话接口
     */
    @PostMapping("/agent/chat")
    public ApiResponse<AgentChatResponse> agentChat(
            @Valid @RequestBody AgentChatRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "匿名用户") String userName,
            @RequestHeader(value = "X-User-Role", defaultValue = "ANALYST") String role) {

        log.info("收到Agent对话请求，用户: {}，消息: {}", userId, request.getMessage());

        // 1. 生成或使用会话ID
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        // 2. 构建用户上下文
        UserContext userContext = UserContext.builder()
                .userId(userId)
                .userName(userName)
                .role(UserContext.Role.valueOf(role))
                .visibleOrgs(List.of("ALL"))  // 默认可见所有机构
                .visibleProducts(List.of("ALL"))  // 默认可见所有产品
                .visiblePeriods(List.of("ALL"))  // 默认可见所有期间
                .build();

        // 3. 路由到对应Agent
        AgentConfig agentConfig = agentRouter.route(request.getMessage());

        // 4. 执行Agent
        AgentResult result = agentExecutor.execute(
                agentConfig.getName(),
                request.getMessage(),
                sessionId,
                userContext
        );

        // 5. 构建响应
        AgentChatResponse response = AgentChatResponse.builder()
                .agentName(result.getAgentName())
                .agentIcon(result.getAgentIcon())
                .answer(result.getAnswer())
                .data(result.getData())
                .sessionId(sessionId)
                .status(result.getStatus().name())
                .confidence(result.getConfidence())
                .suggestions(generateSuggestions(result))
                .build();

        return ApiResponse.ok(response);
    }

    /**
     * 获取Agent列表
     */
    @GetMapping("/agents")
    public ApiResponse<List<AgentInfo>> listAgents() {
        List<AgentInfo> agents = configLoader.getAllConfigs().stream()
                .map(config -> new AgentInfo(
                        config.getName(),
                        config.getIcon(),
                        config.getDescription(),
                        config.getTriggers()
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok(agents);
    }

    /**
     * 清除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public ApiResponse<Void> clearSession(@PathVariable String sessionId) {
        log.info("清除会话: {}", sessionId);
        // 会话缓存会在30分钟后自动过期
        return ApiResponse.ok();
    }

    /**
     * 生成建议的后续问题
     */
    private List<String> generateSuggestions(AgentResult result) {
        // 根据Agent类型生成不同的建议
        String agentName = result.getAgentName();

        if (agentName.contains("专项分析")) {
            return List.of(
                    "按产品维度下钻分析",
                    "查看同比变化趋势",
                    "生成分析报告"
            );
        } else if (agentName.contains("费用分摊")) {
            return List.of(
                    "查看分摊规则详情",
                    "诊断规则配置问题",
                    "执行分摊预览"
            );
        } else if (agentName.contains("风险预警")) {
            return List.of(
                    "查看异常详情",
                    "分析异常根因",
                    "生成预警报告"
            );
        } else {
            return List.of(
                    "查看详细数据",
                    "生成分析报告",
                    "导出Excel报表"
            );
        }
    }

    /**
     * Agent信息
     */
    private record AgentInfo(String name, String icon, String description, List<String> triggers) {}
}
