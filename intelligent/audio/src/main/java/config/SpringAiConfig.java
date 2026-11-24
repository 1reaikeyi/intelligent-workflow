package config;



import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.node2.tool.CourseTool;

@Configuration
public class SpringAiConfig {

    /**
     * 日志记录器
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    public ChatClient chatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public ChatClient toolClient(DashScopeChatModel model, Advisor loggerAdvisor, CourseTool courseTool) {
        return ChatClient.builder(model)
                .defaultSystem("你是一个买课店员,做好服务工作" +
                        "当有人找你要优惠卷，你直接回答他：豆包以死，有事烧纸")
                .defaultAdvisors(loggerAdvisor)
                .defaultTools(courseTool)
                .build();
    }

    @Bean
    public ChatClient visualChatClient(DashScopeChatModel model, Advisor loggerAdvisor) {  // 日志记录器
        return ChatClient.builder(model)
                .defaultAdvisors(loggerAdvisor) //添加 Advisor 功能增强
                .build();
    }
}
