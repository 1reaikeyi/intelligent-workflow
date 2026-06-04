package start.controller;

import common.result.Result;
import model.vo.ChatTittleVO;
import model.vo.MessageVO;
import model.vo.SessionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import service.ChatSessionService;
import service.session.SessionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/session")
public class SessionController {
    @Autowired
    private SessionService sessionService;
    @Autowired
    private ChatSessionService chatSessionService;
    /**
     * 新建会话
     */
    @PostMapping("/start")
    public Result<SessionVO> createSession(@RequestParam(value = "n", defaultValue = "3") Long num) {
        return Result.success(sessionService.createSession(num));
    }

    /**
     * 查询单个历史对话详情
     *
     * @return 对话记录列表
     */
    @GetMapping("/{sessionId}")
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return chatSessionService.queryBySessionId(sessionId);
    }

    /**
     * 更新历史会话标题
     *
     * @param sessionId 会话id
     * @param title     标题
     */
    @PutMapping("/title")
    public Result updateTitle(String sessionId, String title){
        chatSessionService.updateTitle(sessionId, title);
        return Result.success(sessionId);
    }
    /**
     * 查询历史会话列表
     */
    @GetMapping("/history")
    public Result<Map<String, List<ChatTittleVO>>> queryHistorySession(){
        Map<String, List<ChatTittleVO>> map = chatSessionService.queryHistorySession();
        return Result.success(map);
    }

    /**
     * 删除历史会话
     *
     * @param sessionId 会话ID
     */
    @DeleteMapping
    public Result deleteHistorySession(@RequestParam String sessionId){
        chatSessionService.deleteHistorySession(sessionId);
        return Result.success(sessionId);
    }
}
