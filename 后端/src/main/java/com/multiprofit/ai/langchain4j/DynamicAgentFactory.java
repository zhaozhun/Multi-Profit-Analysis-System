package com.multiprofit.ai.langchain4j;

import com.multiprofit.agent.AgentConfig;
import com.multiprofit.agent.AgentConfigLoader;
import com.multiprofit.ai.ModelApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;

/**
 * 动态Agent工厂
 * 根据MD配置文件动态创建Agent接口实例
 * 使用ModelApiClient（已验证可用的HTTP客户端）作为底层调用
 */
@Slf4j
@Component
public class DynamicAgentFactory {

    @Autowired
    private ModelApiClient claudeClient;

    @Autowired
    private AgentConfigLoader configLoader;

    /**
     * 根据MD配置动态创建Agent
     *
     * @param agentName     Agent名称（对应MD文件的name）
     * @param agentInterface Agent接口类
     * @return Agent接口实例
     */
    @SuppressWarnings("unchecked")
    public <T> T createAgent(String agentName, Class<T> agentInterface) {
        // 1. 读取MD配置
        AgentConfig config = configLoader.getConfig(agentName);
        if (config == null) {
            throw new IllegalArgumentException("Agent配置不存在: " + agentName);
        }

        log.info("创建动态Agent: {} - {}", config.getIcon(), config.getName());

        // 2. 创建动态代理
        return (T) Proxy.newProxyInstance(
            agentInterface.getClassLoader(),
            new Class[]{agentInterface},
            (proxy, method, args) -> {
                // 3. 读取MD文件的systemPrompt
                String systemPrompt = config.getSystemPrompt();

                // 4. 构建用户消息
                String userMessage = args[0].toString();

                // 5. 使用ModelApiClient调用（已验证代理API可用）
                return claudeClient.chat(systemPrompt, userMessage);
            }
        );
    }

    /**
     * 检查Agent配置是否存在
     */
    public boolean hasAgent(String agentName) {
        return configLoader.getConfig(agentName) != null;
    }
}
