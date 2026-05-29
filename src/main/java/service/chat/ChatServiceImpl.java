package service.chat;

import jakarta.annotation.Resource;
import model.enums.ChatEventTypeEnum;
import model.vo.ChatEventVO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import start.config.SystemPromptConfig;

import java.time.LocalDateTime;

@Service
public class ChatServiceImpl implements ChatService {
    @Resource(name = "chatClient")
    private ChatClient chatClient;
    @Autowired
    private SystemPromptConfig systemPromptConfig;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final static String  OUTPUT_STATUS = "OUTPUT_STATUS";
    /**
     * chat
     *
     * @param question  问题
     * @param sessionId 会话id
     * @return 回答内容
     */
    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        //会话id-->转sessionId
        var conversationId = ChatService.getConversationId(sessionId);
        var outputHash = stringRedisTemplate.boundHashOps(OUTPUT_STATUS);
        return chatClient.prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(promptSystemSpec -> promptSystemSpec
                        .text(systemPromptConfig.getChatSystemMessage().get())
                        .param("now", LocalDateTime.now())
                )
                .stream()
                .chatResponse()
                // 第一次输出内容时执行
                // 出现异常时，删除标识
                // 完成时执行，删除标识
                .doFirst(() -> outputHash.put(sessionId, "true"))  // 将布尔值转换为字符串存入 Redis
                .doOnError(throwable -> outputHash.delete(sessionId))
                .doOnComplete(() -> outputHash.delete(sessionId))
                //控制是否继续
                .takeWhile(chatResponse -> outputHash.get(sessionId) != null )
                .map(chatResponse -> {
                    String response = chatResponse.getResult().getOutput().getText();
                    ChatEventVO chatEventVO = ChatEventVO.builder()
                            .eventData(response)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                    return chatEventVO;})
                .concatWith(Flux.just(ChatEventVO.builder().
                        eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }

    /**
     * 停止生成
     *
     * @param sessionId 会话id
     */
    @Override
    public void stop(String sessionId) {
        var outputHash = stringRedisTemplate.boundHashOps(OUTPUT_STATUS);
        // 移除标记
        outputHash.delete(sessionId);
    }
}
