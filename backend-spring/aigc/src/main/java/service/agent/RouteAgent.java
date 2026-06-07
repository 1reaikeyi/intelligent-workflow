package service.agent;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.enums.AgentTypeEnum;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import start.config.SystemPromptConfig;

import java.util.List;

/**
 * 路由智能体
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;

    // 使用专门的路由 ChatClient，避免记忆功能干扰
    @Resource(name = "routeChatClient")
    private final ChatClient routeChatClient;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.ROUTE;
    }

    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getRouteAgentSystemMessage().get();
    }

    @Override
    public List<Advisor> advisors() {
        // 路由智能体不需要记忆功能，返回空列表
        return List.of();
    }

    @Override
    public Object[] tools() {
        // 路由智能体不需要工具，返回空数组
        return EMPTY_OBJECTS;
    }

    @Override
    protected ChatClient getChatClient() {
        return routeChatClient;
    }
}
