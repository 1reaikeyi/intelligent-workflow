package service.agent;

import lombok.extern.slf4j.Slf4j;
import model.vo.ChatEventVO;
import reactor.core.publisher.Flux;

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
