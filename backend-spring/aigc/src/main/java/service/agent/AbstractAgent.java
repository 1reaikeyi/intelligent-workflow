package service.agent;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import common.constants.Constant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import model.enums.ChatEventTypeEnum;
import model.vo.ChatEventVO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Flux;
import service.SessionService;
import service.chat.ChatService;

import java.util.Map;

@Slf4j
public abstract class AbstractAgent implements Agent {
    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId){
        return null;
    }
    @Override
    public String process(String question, String sessionId){
        return null;
    }
    @Override
    public void stop(String sessionId){

    }
    @Override
    public Map<String, Object> advisorParams(String sessionId, String requestId) {
        return Map.of();
    }
}
