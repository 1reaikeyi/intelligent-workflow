package service.agent;

import lombok.RequiredArgsConstructor;
import model.enums.AgentTypeEnum;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import start.config.SystemPromptConfig;

@Component
@RequiredArgsConstructor
public class KnowledgeAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;

    // 注入默认的 ChatClient（带记忆功能）
    @Qualifier("chatClient")
    private final ChatClient chatClient;

    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getKnowledgeAgentSystemMessage().get();
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.KNOWLEDGE;
    }

    @Override
    protected ChatClient getChatClient() {
        return chatClient;
    }
}
