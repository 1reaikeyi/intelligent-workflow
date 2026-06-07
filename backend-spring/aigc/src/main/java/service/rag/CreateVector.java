package service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Vector;

@Service
@Slf4j
public class CreateVector {
    @Autowired
    private VectorStore vectorStore;
    public void create(List<String> vectors){
        //构建文档
        List<Document> documents = vectors.stream()
                .map(message -> Document.builder().text(message).build())
                .toList();
        //存储到向量数据库中
        vectorStore.add(documents);
        log.info("保存到向量数据库中，消息数据：{}", vectors);
        log.info("保存到向量数据库成功, 数量：{}", vectors.size());
    }
}
