 package chat.controller;

 import org.springframework.ai.chat.client.ChatClient;
 import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Qualifier;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.RestController;
 import reactor.core.publisher.Flux;

 import java.util.Map;

 @RestController
public class MemoryChat {
     @Autowired @Qualifier("memoryClient") // 指定注入Config中chatClient方法创建的Bean
     private ChatClient memoryClient;
     @RequestMapping("/memory")
     public Flux<String> memoryChat(@RequestParam("message") String message,@RequestParam String memoryId) {
         Flux<String> response = memoryClient.prompt()
                 .user(message)
                 .advisors(a -> a.param(String.valueOf(MessageChatMemoryAdvisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER), memoryId))
                 .stream()
                 .content();
         return response;
//         // 从 ChatResponse 中提取文本内容
//         String content = response.getResult().getOutput().getText();
//
//         Map.of(
//                 "question", request.getQuestion(),
//                 "answer", content,
//                 // metadata 中包含 token 用量等信息
//                 "metadata", response.getMetadata()
//         );

     }

 }

