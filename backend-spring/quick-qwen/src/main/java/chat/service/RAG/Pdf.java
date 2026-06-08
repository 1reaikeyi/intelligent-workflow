package chat.service.RAG;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class Pdf {
    // 固定PDF文件路径（项目资源目录下的论语.pdf）
    public static final String FIXED_PDF_PATH = "论语.pdf";
    // 固定文件名（用于向量库过滤）
    public static final String FIXED_PDF_NAME = "论语.pdf";
    private final VectorStore vectorStore;

    /**
     * 添加API访问延迟，每次调用相差0.5秒
     */
    public void delayApiAccess() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("→→→API延迟被中断: " + e.getMessage());
        }
    }

    @PostConstruct
    public void loadFixedPdfToVectorStore() {
        // 从 classpath 加载 resources 目录下的 论语.pdf
        Resource fixedPdf = new ClassPathResource(FIXED_PDF_PATH);
        if (!fixedPdf.exists()) {
            System.out.println("》》》PDF文件不存在,请检查文件路径：" + FIXED_PDF_PATH);
            return;
        }
        // 读取PDF并写入向量库
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                fixedPdf,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每页作为一个Document
                        .build()
        );
        List<Document> documents = reader.read();
        vectorStore.add(documents);
        System.out.println("》》》PDF文件已成功加载到向量库：" + FIXED_PDF_PATH);

        // 添加API访问延迟
        delayApiAccess();

    }

}
