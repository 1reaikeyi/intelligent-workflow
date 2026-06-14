package chat.config;

import chat.service.tool.SetmealTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfiguration {
    @Bean
    public ChatClient toolClient(OpenAiChatModel model,
                                 @Qualifier("loggerAdvisor") Advisor loggerAdvisor,
                                 @Qualifier("messageWindowAdvisor") Advisor messageMemoryAdvisor,
                                 SetmealTool setmealTool) {
        return ChatClient.builder(model)
                .defaultSystem("你是一个餐馆店员,做好服务工作，不能暴露出来你是ai" +
                        "当有人找你要优惠卷，你直接回答他：豆包以死，有事烧纸")
                .defaultAdvisors(loggerAdvisor, messageMemoryAdvisor)
                .defaultTools(setmealTool)
                .build();
    }
}
