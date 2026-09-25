package service.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import model.enums.ChatEventTypeEnum;
import model.vo.ChatEventVO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import service.SessionService;
import service.chat.ChatService;
import start.config.SystemPromptConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RagServiceImpl implements RagService {
    //src/main/java/start/config/RagConfiguratioon.java::ragClient
    @Resource(name = "ragClient")
    private ChatClient ragClient;
    @Autowired
    private SystemPromptConfig systemPromptConfig;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private VectorStore vectorStore;

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
        sessionService.updateTitle(sessionId,question);
        // (1)大模型输出内容的缓存器，用于在输出中断后的数据存储
        var outputBuilder = new StringBuilder();
        var conversationId = ChatService.getConversationId(sessionId);
        var outputHash = stringRedisTemplate.boundHashOps(OUTPUT_STATUS);
        // 创建RAG
        String result = rag(question,sessionId);
        if (result == null){
            return Flux.just(
                    ChatEventVO.builder()
                            .eventData("抱歉，知识库中未找到相关信息，无法回答。")
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build(),
                    ChatEventVO.builder()
                            .eventType(ChatEventTypeEnum.STOP.getValue())
                            .build());
        }
//        if (result != null){}
        return ragClient.prompt()
                .user(result)
                .advisors(advisorSpec -> advisorSpec
                            //会话记忆
                            .param(ChatMemory.CONVERSATION_ID, conversationId))
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
                .doOnCancel(() -> {
                    // 当输出被取消时，保存输出的内容到历史记录中
                    saveStopHistoryRecord(conversationId, outputBuilder.toString());
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
                .concatWith(Flux.just(ChatEventVO.builder()
                        .eventType(ChatEventTypeEnum.STOP.getValue())
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

    private String rag(String question, String sessionId) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .similarityThreshold(0.6d)
                .topK(6)
                .build();
        List<Document> retrievedDocs = vectorStore.similaritySearch(searchRequest);

        // topK 中任意一条相似度 ≥ 0.6 即命中
        List<Document> hitDocs = retrievedDocs.stream()
                .filter(d -> d.getScore() != null && d.getScore() >= 0.6d)
                .toList();

        if (hitDocs.isEmpty()) {
            log.warn("RAG未命中, question={}, sessionId={}, 召回数={}, 最高分={}",
                    question, sessionId, retrievedDocs.size(),
                    retrievedDocs.stream()
                            .map(Document::getScore)
                            .filter(java.util.Objects::nonNull)
                            .max(Double::compareTo)
                            .map(String::valueOf)
                            .orElse("无召回"));
            return null;
        }

        log.info("RAG命中, question={}, 召回数={}, 达标数={}, top1分数={}",
                question, retrievedDocs.size(), hitDocs.size(),
                retrievedDocs.get(0).getScore());

        // context 只拼达标文档，不达标的别喂给模型（省 token、降低干扰）
        String context = hitDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
        String prompt = """
                         请根据以下参考上下文回答问题。如果上下文中没有答案，请明确说"不知道"。
                         ## 参考上下文
                         %s
                         ## 问题
                         %s
                        """ .formatted(context, question);
        return prompt;
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
