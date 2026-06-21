package start.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.tools.CourseTools;

@Configuration
public class ToolConfiguratioon {
    /**
     * 配置 tool 客户端配置
     */
    @Bean
    public ChatClient toolClient(OpenAiChatModel model,
                                 @Qualifier("loggerAdvisor") Advisor loggerAdvisor,
                                 @Qualifier("messageWindowAdvisor") Advisor messageWindowAdvisor,
                                 CourseTools courseTools) {  // 日志记录器
        return ChatClient.builder(model)
                .defaultAdvisors(loggerAdvisor, messageWindowAdvisor) //添加 Advisor 功能增强
                .defaultTools(courseTools)
                .build();
    }
}
