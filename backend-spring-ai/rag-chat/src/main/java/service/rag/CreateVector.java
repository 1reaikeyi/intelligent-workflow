package service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

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
    public void createWithParagraphSplitting(List<String> texts) {
        List<Document> documents = new ArrayList<>();

        for (String text : texts) {
            // 按段落切分（按换行符或空行）
            Pattern paragraphPattern = Pattern.compile("\\n\\s*\\n");
            String[] paragraphs = paragraphPattern.split(text);

            for (String paragraph : paragraphs) {
                if (!paragraph.trim().isEmpty()) {
                    documents.add(Document.builder()
                            .text(paragraph.trim())
                            .metadata(Map.of("source_type", "paragraph"))
                            .build());
                }
            }
        }

        vectorStore.add(documents);
        log.info("保存到向量数据库中，消息数据：{}", texts);
        log.info("段落切分后保存到向量数据库，文档数量：{}", documents.size());
    }
//    public void createWithSemanticSplitting(List<String> texts) {
//        List<Document> documents = new ArrayList<>();
//        TextSplitter textSplitter = new TokenTextSplitter(500, 50); // 500 tokens per chunk, 50 tokens overlap
//
//        for (String text : texts) {
//            // 语义切分 - 保持语义完整性
//            List<String> chunks = textSplitter.split(text);
//
//            for (String chunk : chunks) {
//                documents.add(Document.builder()
//                        .text(chunk)
//                        .metadata(Map.of("source_type", "semantic_chunk"))
//                        .build());
//            }
//        }
//
//        vectorStore.add(documents);
//        log.info("语义切分后保存到向量数据库，文档数量：{}", documents.size());
//    }
}
