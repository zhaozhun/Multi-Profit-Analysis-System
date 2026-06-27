package com.multiprofit.ai.langchain4j;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j配置类
 * 配置模型实例（支持小米、豆包等国产模型）
 */
@Configuration
public class LangChain4jConfig {

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.base-url:}")
    private String baseUrl;

    @Value("${ai.model:}")
    private String model;

    @Value("${ai.max-tokens:4096}")
    private int maxTokens;

    /**
     * 创建Claude模型实例
     */
    @Bean
    public AnthropicChatModel chatModel() {
        // 确保baseUrl以/结尾
        String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(url)
                .modelName(model)
                .maxTokens(maxTokens)
                .build();
    }
}
