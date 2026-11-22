package start.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 测试示例：GET /chat?question=请调用getWeather工具查询城市101010100的天气
     */
    @GetMapping
    public String chat(@RequestParam("question") String question) {
        return chatClient.prompt().user(question).call().content();
    }
}
