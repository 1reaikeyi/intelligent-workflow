package start.controller;

import common.result.Result;
import model.vo.SessionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.SessionService;

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

}
