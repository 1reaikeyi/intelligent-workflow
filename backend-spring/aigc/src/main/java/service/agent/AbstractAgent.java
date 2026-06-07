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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import service.chat.ChatService;
import service.tools.ToolResultHolder;

import java.util.Map;

@Slf4j
@Component
public abstract class AbstractAgent implements Agent {

    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ChatMemory chatMemory;

    private static final String GENERATE_STATUS_KEY = "GENERATE_STATUS";

    /**
     * 获取 ChatClient，子类必须实现此方法返回自己的 ChatClient
     */
    protected abstract ChatClient getChatClient();

    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        // 生成请求id
        var requestId = this.generateRequestId();
        var hashOps = this.stringRedisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        // 将会话id转化为对话id
        var conversationId = ChatService.getConversationId(sessionId);
        // 大模型输出内容的缓存器，用于在输出中断后的数据存储
        var outputBuilder = new StringBuilder();

        return this.getChatClientRequest(question, sessionId, requestId)
                .stream()
                .chatResponse()
                .doFirst(() -> hashOps.put(sessionId, "true")) // 生成开始时，设置标识
                .doOnError(throwable -> hashOps.delete(sessionId)) // 异常结束时，删除标识
                .doOnComplete(() -> hashOps.delete(sessionId)) // 正常结束时，删除标识
                .doOnCancel(() -> {
                    // 当输出被取消时，保存输出的内容到历史记录中
                    this.saveStopHistoryRecord(conversationId, outputBuilder.toString());
                }) // 打断输出的事件
                .takeWhile(response -> hashOps.get(sessionId) != null) // 后续生成的条件，true：继续生成，false：停止生成
                .map(chatResponse -> {
                    // 大模型生成的内容
                    var text = chatResponse.getResult().getOutput().getText();
                    // 追加到输出内容中
                    outputBuilder.append(text);

                    // 获取到消息的结束原因
                    var finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equals(finishReason, Constant.STOP)) {
                        // 获取到消息id
                        var messageId = chatResponse.getMetadata().getId();
                        // 将消息id与请求id进行关联
                        ToolResultHolder.put(messageId, Constant.REQUEST_ID, requestId);
                    }

                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    var result = ToolResultHolder.get(requestId);
                    if (ObjectUtil.isNotEmpty(result)) {
                        ToolResultHolder.remove(requestId);
                        // 工具被调用了，需要向前端传递参数
                        return Flux.just(ChatEventVO.builder()
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .eventData(result)
                                .build(), STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT); // 结束标识
                }));
    }

    @Override
    public String process(String question, String sessionId) {
        try {
            log.info("开始处理路由请求，问题：{}，会话ID：{}", question, sessionId);
            
            // 生成请求id
            var requestId = this.generateRequestId();

            // 使用流式调用并收集结果
            StringBuilder resultBuilder = new StringBuilder();
            
            this.getChatClientRequest(question, sessionId, requestId)
                    .stream()
                    .chatResponse()
                    .doOnNext(chatResponse -> {
                        var text = chatResponse.getResult().getOutput().getText();
                        if (text != null) {
                            resultBuilder.append(text);
                        }
                    })
                    .blockLast(); // 阻塞等待流式调用完成

            String result = resultBuilder.toString();
            log.info("路由智能体返回结果：{}，问题：{}", result, question);

            // 处理可能的空结果
            if (result == null || result.trim().isEmpty()) {
                log.warn("路由智能体返回空结果，问题：{}", question);
                return "抱歉我只处理课程相关问题";
            }

            return result;
        } catch (Exception e) {
            log.error("路由智能体处理异常，问题：{}，错误：{}", question, e.getMessage(), e);
            return "抱歉我只处理课程相关问题";
        }
    }

    private ChatClient.ChatClientRequestSpec getChatClientRequest(String question, String sessionId, String requestId) {
        return this.getChatClient().prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(this.systemMessage()).params(this.systemMessageParams()))
                .advisors(advisorSpec -> advisorSpec.advisors(this.advisors()).params(this.advisorParams(sessionId, requestId)))
                .tools(this.tools())
                .toolContext(this.toolContext(sessionId, requestId))
                .user(question);
    }

    /**
     * 保存停止输出的记录
     *
     * @param conversationId 会话id
     * @param content        大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId, String content) {
        this.chatMemory.add(conversationId, new AssistantMessage(content));
    }

    private String generateRequestId() {
        return IdUtil.fastSimpleUUID();
    }

    @Override
    public void stop(String sessionId) {
        var hashOps = this.stringRedisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        hashOps.delete(sessionId);
    }

    @Override
    public Map<String, Object> advisorParams(String sessionId, String requestId) {
        // 将会话id转化为对话id
        var conversationId = ChatService.getConversationId(sessionId);
        return Map.of(ChatMemory.CONVERSATION_ID, conversationId);
    }
}
