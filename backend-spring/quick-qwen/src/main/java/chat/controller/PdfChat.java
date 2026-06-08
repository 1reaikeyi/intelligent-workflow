package chat.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static chat.service.RAG.Pdf.FIXED_PDF_NAME;
import static org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor.FILTER_EXPRESSION;

@RestController

public class PdfChat {
    @Autowired @Qualifier("ragClient")
    private ChatClient pdfClient;
    @RequestMapping("/rag")
    public Flux<String> ragChat(@RequestParam("message") String message,String memoryId) {
        return pdfClient.prompt()
                .user(message)
                .advisors(a -> a.param(String.valueOf(MessageChatMemoryAdvisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER), memoryId))
                .advisors(a -> a.param(FILTER_EXPRESSION, "file_name == '" + FIXED_PDF_NAME + "'"))
                .stream()
                .content();

    }

}
