package service;

import cn.hutool.core.stream.StreamUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import model.entity.ChatRecord;
import model.enums.MessageTypeEnum;
import model.vo.ChatTittleVO;
import model.vo.MessageVO;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import service.chat.ChatService;
import service.memory.AssistantMessageUtil;
import service.memory.mysql.ChatRecordService;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
@Service
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService{
    @Autowired
    private ChatRecordService chatRecordService;
    @Autowired
    private ChatMemory chatMemory;
    /**
     * 根据会话id查询消息列表
     *
     * @return 消息列表
     */
    @Override
    public List<MessageVO> queryBySessionId(String chatId) {
        // 根据会话ID获取对话ID
        String conversationId = ChatService.getConversationId(chatId);
        // 从Redis中获取历史消息
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
        LambdaQueryWrapper<ChatRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatRecord::getConversationId, sessionId);
        List<ChatRecord> list = chatRecordService.list(queryWrapper);
        if (list.isEmpty()) {
            return;
        }
        ChatRecord chatRecord = list.get(0);
        // 安全截取标题，避免长度不足时抛出异常
        chatRecord.setTitle(title.length() > 100 ? title.substring(0, 100) : title);
        chatRecordService.updateById(chatRecord);
    }

    /**
     * 查询历史会话列表
     */
    @Override
    public Map<String, List<ChatTittleVO>> queryHistorySession() {
        // 查询历史会话，限制返回条数
        var list = chatRecordService.lambdaQuery()
                .orderByDesc(ChatRecord::getUpdateTime)
                .last("LIMIT 30")
                .list();
        if (list.isEmpty()) {
            log.info("No chat sessions found");
            return Map.of();
        }

        // 转换为 ChatSessionVO 列表
        var chatSessionVOList = list.stream()
                .map(chat ->ChatTittleVO.builder()
                        .sessionId(chat.getConversationId())
                        .title(chat.getTitle())
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
        chatRecordService.removeById(sessionId);
        //2删除缓存
        chatMemory.clear(sessionId);
    }
}
