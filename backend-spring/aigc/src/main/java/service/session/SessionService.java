package service.session;

import com.baomidou.mybatisplus.extension.service.IService;
import model.entity.Session;
import model.vo.SessionVO;

public interface SessionService extends IService<Session> {
    /**
     * 创建会话session
     *
     * @param num 热门问题的数量
     * @return 会话信息
     */
    SessionVO createSession(Long num);



}
