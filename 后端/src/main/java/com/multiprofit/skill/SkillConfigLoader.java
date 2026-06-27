package com.multiprofit.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 技能配置加载器
 * 从classpath加载所有MD配置文件，解析YAML frontmatter和技能说明
 */
@Slf4j
@Component
public class SkillConfigLoader {

    @Value("${skill.config.path:classpath:skills/}")
    private String configPath;

    private final Map<String, SkillConfig> configs = new LinkedHashMap<>();

    @PostConstruct
    public void loadConfigs() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(configPath + "*.md");

            for (Resource resource : resources) {
                try {
                    SkillConfig config = parseMarkdown(resource);
                    configs.put(config.getName(), config);
                    log.info("加载技能配置: {} - {}", config.getName(), config.getDescription());
                } catch (Exception e) {
                    log.error("加载技能配置失败: {}", resource.getFilename(), e);
                }
            }

            log.info("共加载 {} 个技能配置", configs.size());

        } catch (IOException e) {
            log.error("扫描技能配置文件失败", e);
        }
    }

    /**
     * 解析Markdown文件
     */
    private SkillConfig parseMarkdown(Resource resource) throws IOException {
        String content = readResourceContent(resource);

        SkillConfig config = new SkillConfig();

        // 解析YAML frontmatter
        String yaml = extractFrontmatter(content);
        parseYaml(yaml, config);

        // 提取技能说明
        String documentation = extractDocumentation(content);
        config.setDocumentation(documentation);

        return config;
    }

    /**
     * 读取资源内容
     */
    private String readResourceContent(Resource resource) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 提取YAML frontmatter
     */
    private String extractFrontmatter(String content) {
        Pattern pattern = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 提取技能说明
     */
    private String extractDocumentation(String content) {
        int start = content.indexOf("# ");
        if (start >= 0) {
            return content.substring(start).trim();
        }
        return "";
    }

    /**
     * 解析YAML（简化实现）
     */
    private void parseYaml(String yaml, SkillConfig config) {
        String[] lines = yaml.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("name:")) {
                config.setName(line.substring(5).trim());
            } else if (line.startsWith("icon:")) {
                config.setIcon(line.substring(5).trim());
            } else if (line.startsWith("description:")) {
                config.setDescription(line.substring(12).trim());
            } else if (line.startsWith("priority:")) {
                try {
                    config.setPriority(Integer.parseInt(line.substring(9).trim()));
                } catch (NumberFormatException e) {
                    config.setPriority(100);
                }
            } else if (line.startsWith("- ") && config.getTriggers() != null) {
                // 列表项
                config.getTriggers().add(line.substring(2).trim());
            } else if (line.startsWith("triggers:")) {
                config.setTriggers(new ArrayList<>());
            } else if (line.startsWith("tools:")) {
                config.setTools(new ArrayList<>());
            }
        }
    }

    /**
     * 根据技能名称获取配置
     */
    public SkillConfig getConfig(String skillName) {
        return configs.get(skillName);
    }

    /**
     * 获取所有配置
     */
    public List<SkillConfig> getAllConfigs() {
        return new ArrayList<>(configs.values());
    }

    /**
     * 根据触发词匹配技能
     */
    public SkillConfig matchByTrigger(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        String lowerMessage = message.toLowerCase();

        // 按优先级排序后匹配
        return configs.values().stream()
            .sorted(Comparator.comparingInt(SkillConfig::getPriority))
            .filter(config -> config.getTriggers() != null)
            .filter(config -> config.getTriggers().stream()
                .anyMatch(trigger -> lowerMessage.contains(trigger.toLowerCase())))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有技能触发词
     */
    public Map<String, List<String>> getAllTriggers() {
        Map<String, List<String>> triggers = new LinkedHashMap<>();
        for (SkillConfig config : configs.values()) {
            triggers.put(config.getName(), config.getTriggers());
        }
        return triggers;
    }
}
