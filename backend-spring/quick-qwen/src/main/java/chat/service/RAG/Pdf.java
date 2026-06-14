package chat.service.RAG;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class Pdf {
    private static final String FIXED_PDF_PATH = "论语.pdf";
    // 固定文件名
    public static final String FIXED_PDF_NAME = "论语.pdf";

    private final VectorStore vectorStore;
    @PostConstruct
    public void loadFixedPdfToVectorStore() {
        // 从类路径加载 resources 目录下的 PDF 文件
        ClassPathResource resource = new ClassPathResource(FIXED_PDF_PATH);
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每页作为一个Document
                        .build()
        );
        List<Document> documents = reader.read();
        vectorStore.add(documents);
        log.info("PDF文件已成功加载到向量库"+FIXED_PDF_NAME);

    }

}
