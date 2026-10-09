package start.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
@Configuration
@RequiredArgsConstructor
public class SystemPromptConfig {

    @Value("${tj.ai.prompt.system.chat.file:classpath:system-message.txt}")
    private String systemChatFilePath;
    @Value("${tj.ai.prompt.route.agent.file:classpath:route-agent-system-message.txt}")
    private String routeAgentFilePath;
    @Value("${tj.ai.prompt.recommend.agent.file:classpath:recommend-agent-system-message.txt}")
    private String recommendAgentFilePath;
    @Value("${tj.ai.prompt.knowledge.agent.file:classpath:knowledge-agent-system-message.txt}")
    private String knowledgeAgentFilePath;

    private final AtomicReference<String> chatSystemMessage = new AtomicReference<>();
    private final AtomicReference<String> routeAgentSystemMessage = new AtomicReference<>();
    private final AtomicReference<String> recommendAgentSystemMessage = new AtomicReference<>();
    private final AtomicReference<String> knowledgeAgentSystemMessage = new AtomicReference<>();
    @PostConstruct
    public void init() {
        loadSystemPrompt();
        loadRouteAgentConfig();
        loadRecommendConfig();
        loadKnowledgeConfig();
    }

    private void loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(systemChatFilePath.replace("classpath:", ""));
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            chatSystemMessage.set(content);
            log.info("成功加载系统提示词文件: {}, 内容长度: {} 字符", systemChatFilePath, content.length());
        } catch (IOException e) {
            log.error("加载系统提示词文件失败: {}", systemChatFilePath, e);
            chatSystemMessage.set("");
        }
    }
    private void loadRouteAgentConfig() {
        try {
            // 加载路由代理系统消息文件，路径配置在 application.yml 中
            ClassPathResource resource = new ClassPathResource(routeAgentFilePath.replace("classpath:", ""));
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            routeAgentSystemMessage.set(content);
            log.info("成功加载路由agent系统消息文件: {}, 内容长度: {} 字符", routeAgentFilePath, content.length());
        } catch (IOException e) {
            log.error("加载路由agent系统消息文件失败: {}", routeAgentFilePath, e);
            routeAgentSystemMessage.set("");
        }
    }
    private void loadRecommendConfig() {
        try {
            // 加载推荐智能体系统消息文件，路径配置在 application.yml 中
            ClassPathResource resource = new ClassPathResource(recommendAgentFilePath.replace("classpath:", ""));
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            recommendAgentSystemMessage.set(content);
            log.info("成功加载推荐agent系统消息文件: {}, 内容长度: {} 字符", recommendAgentFilePath, content.length());
        } catch (IOException e) {
            log.error("加载推荐agent系统消息文件失败: {}", recommendAgentFilePath, e);
            recommendAgentSystemMessage.set("");
        }
    }
    private void loadKnowledgeConfig() {
        try {
            // 加载知识智能体系统消息文件，路径配置在 application.yml 中
            ClassPathResource resource = new ClassPathResource(knowledgeAgentFilePath.replace("classpath:", ""));
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            knowledgeAgentSystemMessage.set(content);
            log.info("成功加载知识agent系统消息文件: {}, 内容长度: {} 字符", knowledgeAgentFilePath, content.length());
        } catch (IOException e) {
            log.error("加载知识agent系统消息文件失败: {}", knowledgeAgentFilePath, e);
            knowledgeAgentSystemMessage.set("");
        }
    }

}