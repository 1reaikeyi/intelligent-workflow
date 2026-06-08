import chat.QwenApplication;
import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = QwenApplication.class)
public class Pdf {
    @Autowired
    private VectorStore vectorStore;

    @Test
    public void testVectorStore(){
        Resource resource = new FileSystemResource("论语.pdf");
        // 1.创建PDF的读取器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource, // 文件源
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每1页PDF作为一个Document
                        .build()
        );
        // 2.读取PDF文档，拆分为Document
        List<Document> documents = reader.read();
        // 3.写入向量库
        vectorStore.add(documents);
        // 4.搜索
        SearchRequest request = SearchRequest.builder()
                .query("《论语》中‘己所不欲，勿施于人’出自哪一篇")
                .topK(1)
                .similarityThreshold(0.6)
                .filterExpression("file_name == '论语.pdf'")
                .build();
        List<Document> docs = vectorStore.similaritySearch(request);
        if (docs == null) {
            System.out.println("没有搜索到任何内容");
            return;
        }
        for (Document doc : docs) {
            System.out.println("相似度得分: " + doc.getScore());
            System.out.println("文档内容: " + doc.getText());
        }
    }



    @Test
    public void testTxtVectorStore() {
        // 1. 指定TXT文件路径（替换为你的实际路径）
        String txtFilePath = "src/main/resources/apple.md";
        Resource txtResource = new FileSystemResource(txtFilePath);

        List<Document> txtDocuments = new ArrayList<>();
        try {
            // 2. 原生IO读取TXT文件内容（UTF-8编码，适配中文）
            String txtContent = Files.readString(Paths.get(txtResource.getURI()), StandardCharsets.UTF_8);

            // 3. 手动构建Document对象（核心步骤，替代TxtDocumentReader）
            Document txtDoc = new Document(txtContent);
            // 添加元数据（用于后续过滤，和PDF保持一致的逻辑）
            txtDoc.getMetadata().put("file_name", "apple.md");
            txtDoc.getMetadata().put("file_type", "md");
            txtDocuments.add(txtDoc);

        } catch (IOException e) {
            System.err.println("读取TXT文件失败：" + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 4. 将TXT内容写入向量库（和PDF逻辑一致）
        vectorStore.add(txtDocuments);

        // 5. 构建搜索请求（和PDF逻辑一致）
        SearchRequest txtSearchRequest = SearchRequest.builder()
                .query("版本与售价") // 替换为你的搜索关键词
                .topK(1)
                .similarityThreshold(0.6)
                .filterExpression("file_name == 'info.txt'") // 过滤TXT文件
                .build();

        // 6. 执行搜索并输出结果
        List<Document> searchResults = vectorStore.similaritySearch(txtSearchRequest);
        if (searchResults == null || searchResults.isEmpty()) {
            System.out.println("未从TXT文件中搜索到相关内容");
            return;
        }

        // 7. 输出结果（格式优化，便于查看）
        for (Document result : searchResults) {
            System.out.println("相似度得分: " + result.getScore());
            System.out.println("文档内容: " + result.getText());
        }
    }
}