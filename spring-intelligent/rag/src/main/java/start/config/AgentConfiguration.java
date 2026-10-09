package start.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智能体相关配置
 */
@Configuration
public class AgentConfiguration {

    /**
     * 配置路由智能体专用的 ChatClient
     * 路由智能体不需要记忆功能，使用简单的配置
     */
    @Bean("routeChatClient")
    public ChatClient routeChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model).build();
    }
}
