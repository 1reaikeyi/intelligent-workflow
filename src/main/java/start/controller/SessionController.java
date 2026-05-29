package start.controller;

import common.result.Result;
import model.vo.MessageVO;
import model.vo.SessionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import service.SessionService;

import java.util.List;

@RestController
@RequestMapping("/session")
public class SessionController {
    @Autowired
    private SessionService chatSessionService;
    /**
     * 新建会话
     */
    @PostMapping
    public Result<SessionVO> createSession(@RequestParam(value = "n", defaultValue = "3") Long num) {
        return Result.success(chatSessionService.createSession(num));
    }
    /**
     * 查询单个历史对话详情
     *
     * @return 对话记录列表
     */
    @GetMapping("/{sessionId}")
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return this.chatSessionService.queryBySessionId(sessionId);
    }

}
