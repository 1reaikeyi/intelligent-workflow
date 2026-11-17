package service.tools;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import service.chat.ChatService;
import common.constants.Constant;
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
import service.session.SessionService;
import start.config.SystemPromptConfig;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ToolServiceImpl implements ToolService {
    //src/main/java/start/config/ToolConfiguratioon.java::toolClient
    @Resource(name = "toolClient")
    private ChatClient toolClient;
    @Autowired
    private SystemPromptConfig systemPromptConfig;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private SessionService sessionService;

    private final static String  OUTPUT_STATUS = "OUTPUT_STATUS";
    // 输出结束的标记
    private static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
                                .eventType(ChatEventTypeEnum.STOP.getValue())
                                .build();
    private static final String REQUEST_ID = "requestId";
    /**
     * chat
     *
     * @param question  问题
     * @param sessionId 会话id
     * @return 回答内容
     */
    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        sessionService.updateTitle(sessionId,question);
        // (1)大模型输出内容的缓存器，用于在输出中断后的数据存储
        var outputBuilder = new StringBuilder();
        //会话id-->转sessionId
        var conversationId = ChatService.getConversationId(sessionId);
        // 生成请求id
        var requestId = IdUtil.fastSimpleUUID();
        //控制是否stop
        var outputHash = stringRedisTemplate.boundHashOps(OUTPUT_STATUS);
        return toolClient.prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(promptSystemSpec -> promptSystemSpec
                        .text(systemPromptConfig.getChatSystemMessage().get())
                        .param("now", LocalDateTime.now())
                )

                .toolContext(Map.of(REQUEST_ID, requestId)) //通过工具上下文传递参数

                .stream()
                .chatResponse()
                // 第一次输出内容时执行
                // 出现异常时，删除标识
                // 完成时执行，删除标识
                //stop时仍然输出，当输出被取消时，保存输出的内容到历史记录中
                .doFirst(() -> outputHash.put(sessionId, "true"))  // 将布尔值转换为字符串存入 Redis
                .doOnError(throwable -> outputHash.delete(sessionId))
                .doOnComplete(() -> outputHash.delete(sessionId))
                .doOnCancel(() -> {
                    this.saveStopHistoryRecord(conversationId, outputBuilder.toString());
                })
                //控制是否继续
                .takeWhile(chatResponse -> outputHash.get(sessionId) != null )

                // 流式返回的部分分块getResult() 可能为 null，直接跳过
                .filter(chatResponse -> chatResponse != null
                        && chatResponse.getResult() != null
                        && chatResponse.getResult().getOutput() != null
                        && chatResponse.getResult().getOutput().getText() != null)

                .map(chatResponse -> {
                    var finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equals("STOP", finishReason)) {
                        var messageId = chatResponse.getMetadata().getId();
                        ToolResultHolder.put(messageId, "requestId", requestId);
                    }
                    String response = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(response);
                    ChatEventVO chatEventVO = ChatEventVO.builder()
                            .eventData(response)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                    return chatEventVO;
                })

                .concatWith(Flux.defer(() -> {
                    // 通过请求id获取到参数列表，如果不为空，就将其追加到返回结果中
                    var map = ToolResultHolder.get(requestId);
                    if (map.isEmpty()) {
                        return Flux.just(STOP_EVENT);
                    }
                    ToolResultHolder.remove(requestId); // 清除参数列表
                    // 响应给前端的参数数据
                    return Flux.just(new ChatEventVO(map,ChatEventTypeEnum.DATA.getValue()),
                                    STOP_EVENT);
                }));
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
