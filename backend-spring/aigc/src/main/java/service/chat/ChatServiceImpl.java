package service.chat;

import jakarta.annotation.Resource;
import model.enums.ChatEventTypeEnum;
import model.vo.ChatEventVO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import service.ChatSessionService;
import start.config.SystemPromptConfig;

import java.time.LocalDateTime;

@Service
public class ChatServiceImpl implements ChatService {
    //src/main/java/start/config/ChatConfiguratioon.java::chatClient
    @Resource(name = "chatClient")
    private ChatClient chatClient;
    @Autowired
    private SystemPromptConfig systemPromptConfig;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private ChatSessionService chatSessionService;

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
        // (1)大模型输出内容的缓存器，用于在输出中断后的数据存储
        var outputBuilder = new StringBuilder();
        //会话id-->转sessionId
        var conversationId = ChatService.getConversationId(sessionId);
        chatSessionService.updateTitle(sessionId,question);
        //控制是否stop
        var outputHash = stringRedisTemplate.boundHashOps(OUTPUT_STATUS);
        return chatClient.prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec
                        //会话记忆
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(promptSystemSpec -> promptSystemSpec
                        //系统role
                        .text(systemPromptConfig.getChatSystemMessage().get())
                        //param = 参数
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
                //(2)stop时仍然输出，当输出被取消时，保存输出的内容到历史记录中
                .doOnCancel(() -> {
                    this.saveStopHistoryRecord(conversationId, outputBuilder.toString());
                })
                //控制是否继续
                .takeWhile(chatResponse -> outputHash.get(sessionId) != null )

                .map(chatResponse -> {
                    String response = chatResponse.getResult().getOutput().getText();
                    // 追加到输出内容中
                    outputBuilder.append(response);
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
     * 保存停止输出的记录
     *
     * @param conversationId 会话id
     * @param content        大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId, String content) {
        chatMemory.add(conversationId, new AssistantMessage(content));
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
