package com.multiprofit.agent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 会话上下文缓存 - 支持追问场景
 */
@Slf4j
@Component
public class SessionContextCache {

    /**
     * 会话缓存 - 30分钟过期
     */
    private final Cache<String, SessionContext> sessionCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    /**
     * 添加消息到会话
     */
    public void addMessage(String sessionId, ChatMessage message) {
        SessionContext context = sessionCache.getIfPresent(sessionId);
        if (context == null) {
            context = new SessionContext();
            context.setSessionId(sessionId);
            context.setMessages(new ArrayList<>());
        }
        context.getMessages().add(message);
        sessionCache.put(sessionId, context);

        log.debug("会话 {} 添加消息，当前消息数: {}", sessionId, context.getMessages().size());
    }

    /**
     * 获取会话消息历史
     */
    public List<ChatMessage> getMessages(String sessionId) {
        SessionContext context = sessionCache.getIfPresent(sessionId);
        if (context == null) {
            return new ArrayList<>();
        }
        return context.getMessages();
    }

    /**
     * 获取会话上下文
     */
    public SessionContext getContext(String sessionId) {
        return sessionCache.getIfPresent(sessionId);
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        sessionCache.invalidate(sessionId);
        log.debug("清除会话: {}", sessionId);
    }

    /**
     * 会话上下文
     */
    @Data
    public static class SessionContext {
        private String sessionId;
        private List<ChatMessage> messages;
        private String currentAgent;
    }

    /**
     * 聊天消息
     */
    @Data
    public static class ChatMessage {
        private Role role;
        private String content;
        private long timestamp;

        public ChatMessage(Role role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        public enum Role {
            USER, ASSISTANT
        }
    }
}
