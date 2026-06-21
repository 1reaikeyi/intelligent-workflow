package start.controller;

import jakarta.annotation.Resource;
import model.dto.ChatDTO;
import model.vo.ChatEventVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import service.chat.ToolService;

@RestController
@RequestMapping("/tool")
public class ToolController {
    @Resource(name = "toolServiceImpl")
    private ToolService toolService;

    /**
     * 对话
     *
     * @param chatDTO
     * @return
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        return toolService.chat(chatDTO.getQuestion(), chatDTO.getSessionId());
    }
    /**
     * stop_chat
     */
    @PostMapping("/stop")
    public void stop(@RequestParam String sessionId) {
        toolService.stop(sessionId);
    }
}
