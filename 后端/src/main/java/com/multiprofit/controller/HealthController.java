package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统健康检查端点
 * 供 systemd、监控脚本、负载均衡器探测使用
 */
@RestController
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 系统健康检查
     * 返回各组件状态：DB连接、内存、运行时间
     */
    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean healthy = true;

        // 1. 数据库连接检查
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.put("database", "UP");
        } catch (Exception e) {
            result.put("database", "DOWN");
            result.put("database_error", e.getMessage());
            healthy = false;
        }

        // 2. 内存使用情况
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long heapUsagePercent = heapMax > 0 ? (heapUsed * 100 / heapMax) : 0;

        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("heap_used_mb", heapUsed);
        memory.put("heap_max_mb", heapMax);
        memory.put("heap_usage_percent", heapUsagePercent);
        result.put("memory", memory);

        // 内存使用超过 90% 标记为警告
        if (heapUsagePercent > 90) {
            result.put("memory_warning", "堆内存使用率超过90%");
            healthy = false;
        }

        // 3. 运行时间
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeBean.getUptime();
        long uptimeMinutes = uptimeMs / (1000 * 60);
        long uptimeHours = uptimeMinutes / 60;
        long uptimeDays = uptimeHours / 24;

        Map<String, Object> uptime = new LinkedHashMap<>();
        uptime.put("milliseconds", uptimeMs);
        if (uptimeDays > 0) {
            uptime.put("readable", uptimeDays + "天" + (uptimeHours % 24) + "小时");
        } else if (uptimeHours > 0) {
            uptime.put("readable", uptimeHours + "小时" + (uptimeMinutes % 60) + "分钟");
        } else {
            uptime.put("readable", uptimeMinutes + "分钟");
        }
        result.put("uptime", uptime);

        // 4. 总体状态
        result.put("status", healthy ? "UP" : "DOWN");
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return healthy ? ApiResponse.ok(result) : ApiResponse.error(503, "服务异常", result);
    }

    /**
     * 轻量级存活探针（仅确认服务在响应）
     * 适合 systemd Watchdog 和高频探测
     */
    @GetMapping("/api/alive")
    public ApiResponse<String> alive() {
        return ApiResponse.ok("ok");
    }
}
