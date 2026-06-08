package chat.config;

import chat.service.tool.SetmealTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfiguration {
    @Bean
    public ChatClient toolClient(OpenAiChatModel model, ChatMemory chatMemory, SetmealTool setmealTool) {
        return ChatClient.builder(model)
                .defaultSystem("你是一个餐馆店员，必须牢记自己的身份")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(setmealTool)
                .build();
    }
}
