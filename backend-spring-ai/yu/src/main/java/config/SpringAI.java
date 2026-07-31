package config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAI {
    @Bean
    public ChatClient chatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
