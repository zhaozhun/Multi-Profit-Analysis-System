package com.multiprofit.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent配置加载器 - 从MD文件加载Agent配置
 */
@Slf4j
@Component
public class AgentConfigLoader {

    @Value("${agent.config.path:classpath:agents/*.md}")
    private String configPath;

    private final Map<String, AgentConfig> configs = new HashMap<>();

    @PostConstruct
    public void loadConfigs() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(configPath);

            for (Resource resource : resources) {
                try {
                    AgentConfig config = parseMarkdown(resource);
                    configs.put(config.getName(), config);
                    log.info("加载Agent配置: {} - {}", config.getIcon(), config.getName());
                } catch (Exception e) {
                    log.error("加载Agent配置失败: {}", resource.getFilename(), e);
                }
            }

            log.info("共加载 {} 个Agent配置", configs.size());
        } catch (Exception e) {
            log.error("扫描Agent配置文件失败", e);
        }
    }

    /**
     * 解析Markdown文件
     */
    private AgentConfig parseMarkdown(Resource resource) throws Exception {
        String content = readResourceContent(resource);

        // 解析YAML frontmatter
        String yaml = extractFrontmatter(content);
        AgentConfig config = parseYamlConfig(yaml);

        // 提取系统提示词
        String systemPrompt = extractSystemPrompt(content);
        config.setSystemPrompt(systemPrompt);

        return config;
    }

    /**
     * 读取资源内容
     */
    private String readResourceContent(Resource resource) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 提取YAML frontmatter
     */
    private String extractFrontmatter(String content) {
        int firstStart = content.indexOf("---");
        if (firstStart == -1) {
            throw new IllegalArgumentException("MD文件缺少frontmatter");
        }

        int secondStart = content.indexOf("---", firstStart + 3);
        if (secondStart == -1) {
            throw new IllegalArgumentException("MD文件frontmatter格式错误");
        }

        return content.substring(firstStart + 3, secondStart).trim();
    }

    /**
     * 解析YAML配置
     */
    private AgentConfig parseYamlConfig(String yaml) {
        AgentConfig config = new AgentConfig();
        List<String> triggers = new ArrayList<>();
        List<String> tools = new ArrayList<>();

        String[] lines = yaml.split("\n");
        String currentKey = null;
        boolean inList = false;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("- ") && inList) {
                String value = line.substring(2).trim();
                if ("triggers".equals(currentKey)) {
                    triggers.add(value);
                } else if ("tools".equals(currentKey)) {
                    tools.add(value);
                }
            } else if (line.contains(":")) {
                inList = false;
                String[] parts = line.split(":", 2);
                currentKey = parts[0].trim();
                String value = parts[1].trim();

                switch (currentKey) {
                    case "name":
                        config.setName(value);
                        break;
                    case "icon":
                        config.setIcon(value);
                        break;
                    case "description":
                        config.setDescription(value);
                        break;
                    case "max_iterations":
                        config.setMaxIterations(Integer.parseInt(value));
                        break;
                    case "triggers":
                    case "tools":
                        inList = true;
                        break;
                }
            }
        }

        config.setTriggers(triggers);
        config.setTools(tools);

        return config;
    }

    /**
     * 提取系统提示词
     */
    private String extractSystemPrompt(String content) {
        int startIndex = content.indexOf("# 系统提示词");
        if (startIndex == -1) {
            throw new IllegalArgumentException("MD文件缺少系统提示词");
        }

        return content.substring(startIndex).trim();
    }

    /**
     * 获取Agent配置
     */
    public AgentConfig getConfig(String agentName) {
        return configs.get(agentName);
    }

    /**
     * 获取所有Agent配置
     */
    public List<AgentConfig> getAllConfigs() {
        return new ArrayList<>(configs.values());
    }

    /**
     * 根据触发词匹配Agent
     */
    public AgentConfig matchByTrigger(String message) {
        String lowerMessage = message.toLowerCase();

        for (AgentConfig config : configs.values()) {
            // 跳过默认Agent
            if (config.getTriggers().contains("默认")) {
                continue;
            }

            for (String trigger : config.getTriggers()) {
                if (lowerMessage.contains(trigger.toLowerCase())) {
                    return config;
                }
            }
        }

        // 返回默认Agent
        return configs.values().stream()
                .filter(c -> c.getTriggers().contains("默认"))
                .findFirst()
                .orElse(null);
    }
}
