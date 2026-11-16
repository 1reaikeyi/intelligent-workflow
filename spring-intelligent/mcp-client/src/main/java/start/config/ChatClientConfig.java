package start.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    /**
     * ToolCallbackProvider 由 MCP 客户端自动装配，
     * 挂到 ChatClient 后模型即可调用 mcp 服务暴露的工具
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel, ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }
}
