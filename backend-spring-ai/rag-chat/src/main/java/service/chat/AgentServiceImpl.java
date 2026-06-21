package service.chat;

import cn.hutool.extra.spring.SpringUtil;
import lombok.RequiredArgsConstructor;
import model.enums.AgentTypeEnum;
import model.enums.ChatEventTypeEnum;
import model.vo.ChatEventVO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import service.flow.AbstractAgent;
import service.flow.Agent;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {
    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        // 先通过路由智能体，分析用户的意图，再执行后面的逻辑
        var result = this.findAgentByType(AgentTypeEnum.ROUTE).process(question, sessionId);

        // 处理路由智能体返回的结果，去除可能的空白字符和多余内容
        if (result == null || result.trim().isEmpty()) {
            // 如果路由智能体返回空结果，默认返回错误信息
            var errorEventVO = ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData("抱歉，我无法识别您的意图，请重新提问。")
                    .build();
            return Flux.just(errorEventVO, AbstractAgent.STOP_EVENT);
        }

        // 清理返回结果，提取有效的意图标识
        var cleanedResult = result.trim().toUpperCase();
        var agentTypeEnum = AgentTypeEnum.agentNameOf(cleanedResult);

        var agent = this.findAgentByType(agentTypeEnum);
        if (agent == null) {
            // 找不到对应的智能体，直接返回路由结果
            var chatEventVO = ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(result)
                    .build();
            return Flux.just(chatEventVO, AbstractAgent.STOP_EVENT);
        }
        // 执行智能体的逻辑
        return agent.processStream(question, sessionId);
    }

    /**
     * 根据代理类型查找对应的Agent实例
     *
     * @param agentTypeEnum 要查找的代理类型
     * @return 与给定类型匹配的Agent实例，如果未找到或类型为null则返回null
     */
    private Agent findAgentByType(AgentTypeEnum agentTypeEnum) {
        if (agentTypeEnum == null) {
            return null;
        }
        var beans = SpringUtil.getBeansOfType(Agent.class);
        // 遍历所有Agent Bean查找匹配类型
        for (var agent : beans.values()) {
            if (agentTypeEnum == agent.getAgentType()) {
                return agent;
            }
        }
        return null;
    }

    /**
     * 停止生成
     *
     * @param sessionId 会话ID
     */
    @Override
    public void stop(String sessionId) {
        this.findAgentByType(AgentTypeEnum.ROUTE).stop(sessionId);
    }
}

