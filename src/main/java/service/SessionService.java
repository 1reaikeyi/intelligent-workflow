package service;

import com.baomidou.mybatisplus.extension.service.IService;
import model.entity.Session;
import model.vo.MessageVO;
import model.vo.SessionVO;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public interface SessionService extends IService<Session> {
    /**
     * 创建会话session
     *
     * @param num 热门问题的数量
     * @return 会话信息
     */
    SessionVO createSession(Long num);
    /**
     * 根据会话id查询消息列表
     *
     * @param sessionId 会话id
     * @return 消息列表
     */
    List<MessageVO> queryBySessionId(String sessionId);
}
