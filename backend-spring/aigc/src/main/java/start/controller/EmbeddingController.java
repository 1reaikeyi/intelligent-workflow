package start.controller;

import cn.hutool.core.collection.CollStreamUtil;
import common.result.Result;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.dto.ChatDTO;
import model.vo.ChatEventVO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import service.chat.RagService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rag")
public class EmbeddingController {
    @Resource(name = "ragServiceImpl")
    private RagService ragService;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private EmbeddingModel embeddingModel;

    @PostMapping("/embedding")
    public Result saveVectorStore(@RequestParam("messages") List<String> messages) {
        return Result.success("保存到向量数据库数量:"+messages.size());
    }

    /**
     * rag
     * @param chatDTO
     * @return
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        return ragService.chat(chatDTO.getQuestion(), chatDTO.getSessionId());
    }
    /**
     * stop_chat
     */
    @PostMapping("/stop")
    public void stop(@RequestParam String sessionId) {
        ragService.stop(sessionId);
    }
    //返回 ：返回向量表示（通常是浮点数数组）
    @GetMapping
    public EmbeddingResponse embedding(@RequestParam("message") String message) {
        return embeddingModel.embedForResponse(List.of(message));
    }
    //将查询文本向量化后，在向量数据库中查找最相似的文档,2条
    @GetMapping("/search")
    public List<Document> search(@RequestParam("message") String message) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(message).topK(2).build());
    }
    //将查询文本向量化后，在向量数据库中查找最相似的文档,最大topk
    @GetMapping("/search/all")
    public List<Document> searchAll() {
        // 搜索全部数据
        return vectorStore.similaritySearch(SearchRequest.builder().query("").topK(999).build());
    }
    //删除
    @DeleteMapping
    public void deleteVectorStore(@RequestParam("ids") List<String> ids) {
        // 删除向量数据库中的数据
        this.vectorStore.delete(ids);
    }


}
