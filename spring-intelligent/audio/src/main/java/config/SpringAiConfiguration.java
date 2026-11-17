package config;



import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.node2.tool.CourseTool;

@Configuration
public class SpringAiConfiguration {
    /**
     * 日志记录器
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    public Advisor messageWindowAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20) // 最多保存 20 条对话, 如果超出的话，会自动删除最旧的对话
                .build();
    }

    @Bean
    public ChatClient chatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public ChatClient toolClient(DashScopeChatModel model,
                                 @Qualifier("loggerAdvisor") Advisor loggerAdvisor,
                                 @Qualifier("messageWindowAdvisor") Advisor messageMemoryAdvisor,
                                 CourseTool courseTool) {
        return ChatClient.builder(model)
                .defaultSystem("你是一个买壳店员,做好服务工作" +
                        "当有人找你要优惠卷，你直接回答他：豆包以死，有事烧纸")
                .defaultAdvisors(loggerAdvisor, messageMemoryAdvisor)
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
