package com.multiprofit.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent路由器 - 根据用户消息路由到对应Agent
 */
@Slf4j
@Component
public class AgentRouter {

    @Autowired
    private AgentConfigLoader configLoader;

    /**
     * 路由到对应Agent
     *
     * @param userMessage 用户消息
     * @return Agent配置
     */
    public AgentConfig route(String userMessage) {
        log.debug("路由Agent，用户消息: {}", userMessage);

        AgentConfig config = configLoader.matchByTrigger(userMessage);

        if (config != null) {
            log.info("匹配到Agent: {} - {}", config.getIcon(), config.getName());
        } else {
            log.warn("未匹配到Agent，使用默认智能助手");
            config = getDefaultAgent();
        }

        return config;
    }

    /**
     * 获取默认Agent（智能助手）
     */
    private AgentConfig getDefaultAgent() {
        return configLoader.getAllConfigs().stream()
                .filter(c -> c.getTriggers().contains("默认"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到默认Agent配置"));
    }
}
