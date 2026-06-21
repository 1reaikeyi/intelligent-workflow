package start.controller;

import jakarta.annotation.Resource;
import model.dto.ChatDTO;
import model.vo.ChatEventVO;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import service.chat.ChatService;

@RestController
@RequestMapping("chat")
public class ChatController {
    @Resource(name = "chatServiceImpl")
    private ChatService chatService;
    /**
     * 对话
     *
     * @param chatDTO
     * @return
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        return chatService.chat(chatDTO.getQuestion(), chatDTO.getSessionId());
    }
    /**
     * stop_chat
     */
    @PostMapping("/stop")
    public void stop(@RequestParam String sessionId) {
        chatService.stop(sessionId);
    }

}
