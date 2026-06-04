package service.session;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.properties.SessionProperties;
import mapper.SessionMapper;
import model.entity.Session;
import model.vo.SessionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService{
    @Autowired
    private SessionProperties sessionProperties;
    @Override
    public SessionVO createSession(Long num) {
        var sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
        // 随机获取examples
        sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(),Integer.parseInt(num.toString())));

        // 随机生成sessionId
        sessionVO.setSessionId(IdUtil.fastSimpleUUID());

        // 构建持久化对象，并持久化
        var chatSession = Session.builder()
                .sessionId(sessionVO.getSessionId())
                .title(sessionVO.getTitle())
                .build();
        super.save(chatSession);

        return sessionVO;
    }
}
