package service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import start.properties.SessionProperties;
import lombok.extern.slf4j.Slf4j;
import mapper.SessionMapper;
import model.entity.ChatRecord;
import model.entity.Session;
import model.enums.MessageTypeEnum;
import model.vo.ChatSessionVO;
import model.vo.MessageVO;
import model.vo.SessionVO;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import service.chat.ChatService;
import service.memory.AssistantMessageUtil;
import service.memory.mysql.ChatRecordService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {
    @Autowired
    private SessionProperties sessionProperties;
    @Autowired
    private ChatRecordService chatRecordService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ChatMemory chatMemory;
    @Override
    public SessionVO createSession(Long num) {
        var sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
        // 随机获取examples
        sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(),Integer.parseInt(num.toString())));
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
        String formattedDate = now.format(formatter);
        // 随机生成sessionId
        sessionVO.setSessionId(formattedDate);

        // 构建持久化对象，并持久化
        var chatSession = Session.builder()
                .sessionId(sessionVO.getSessionId())
                .title(sessionVO.getTitle())
                .build();
        super.save(chatSession);

        return sessionVO;
    }
    /**
     * 根据会话id查询消息列表
     *
     * @return 消息列表
     */
    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
//        // 根据会话ID获取对话ID
        String conversationId = ChatService.getConversationId(sessionId);
//        // 从chatmemory中获取历史消息
        List<Message> messageList = chatMemory.get(conversationId);
        // 过滤并转换消息列表
        return StreamUtil.of(messageList)
                // 过滤掉非用户消息和助手消息
                .filter(message -> message.getMessageType() == MessageType.ASSISTANT || message.getMessageType() == MessageType.USER)
                // 转换为MessageVO对象
                // 转换为MessageVO对象
                .map(message -> {
                    if (message instanceof AssistantMessageUtil) {
                        return MessageVO.builder()
                                .content(message.getText())
                                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                .params(((AssistantMessageUtil) message).getParams())
                                .build();
                    }
                    return MessageVO.builder()
                            .content(message.getText())
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                            .build();
                })
                .toList();
    }
    /**
     * 更新历史会话标题
     *
     * @param sessionId 会话id
     * @param title     标题
     */
    @Async
    @Override
    public void updateTitle(String sessionId, String title) {
        LambdaQueryWrapper<Session> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Session::getSessionId, sessionId);
        List<Session> list = this.list(queryWrapper);
        if (list.isEmpty()) {
            return;
        }
        Session session= list.get(0);
        // 安全截取标题，避免长度不足时抛出异常
        session.setTitle(title.length() > 100 ? title.substring(0, 100) : title);
        this.updateById(session);
    }
    /**
     * 查询历史会话列表
     */
    @Override
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        // 查询历史会话，限制返回条数
        var list = this.lambdaQuery()
                .orderByDesc(Session::getUpdateTime)
                .last("LIMIT 30")
                .list();
        if (list.isEmpty()) {
            log.info("No chat sessions found");
            return Map.of();
        }

        // 转换为 ChatSessionVO 列表
        var chatSessionVOList = list.stream()
                .map(chat -> ChatSessionVO.builder()
                        .sessionId(chat.getSessionId())
                        .updateTime(chat.getUpdateTime())
                        .build())
                .toList();
        final var TODAY = "当天";
        final var LAST_30_DAYS = "最近30天";
        final var LAST_YEAR = "最近1年";
        final var MORE_THAN_YEAR = "1年以上";

        // 当前时间
        var now = LocalDateTime.now().toLocalDate();

        // 按照更新时间分组
        return chatSessionVOList.stream()
                .collect(Collectors.groupingBy(vo -> {
                    // 计算两个日期之间的天数差
                    long between = Math.abs(ChronoUnit.DAYS.between(vo.getUpdateTime().toLocalDate(), now));
                    if (between == 0) {
                        return TODAY;
                    } else if (between <= 30) {
                        return LAST_30_DAYS;
                    } else if (between <= 365) {
                        return LAST_YEAR;
                    } else {
                        return MORE_THAN_YEAR;
                    }
                }));
    }

    /**
     * 删除历史会话
     *
     * @param sessionId 会话ID
     */
    @Override
    public void deleteHistorySession(String sessionId) {
        //1删除mysql
        this.remove(new LambdaQueryWrapper<Session>()
                .eq(Session::getSessionId, sessionId));
        chatRecordService.remove(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getSessionId, sessionId));
        //2删除缓存
        stringRedisTemplate.delete(sessionId);
    }

}
