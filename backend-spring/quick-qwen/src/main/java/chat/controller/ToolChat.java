package chat.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;

@RestController
public class ToolChat {
    @Autowired @Qualifier("toolClient")
    private ChatClient toolClient;
    @RequestMapping("/tool")
    public Flux<String> toolChat(@RequestParam("message") String message, String memoryId) {
        return toolClient.prompt()
                .user(message)
                .advisors(a -> a.param(String.valueOf(MessageChatMemoryAdvisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER), memoryId))
                .stream()
                .content();
    }

}
