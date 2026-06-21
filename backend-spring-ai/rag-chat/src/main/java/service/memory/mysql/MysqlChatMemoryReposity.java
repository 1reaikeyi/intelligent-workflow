package service.memory.mysql;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import model.entity.ChatRecord;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import service.memory.MessageUtil;

import java.util.List;

@Service
@Profile("mysql")
public class MysqlChatMemoryReposity implements ChatMemoryRepository {
    @Autowired
    private ChatRecordService chatRecordService;
    @Override
    public List<String> findConversationIds() {
        var chatRecordList = chatRecordService.lambdaQuery()
                .select(ChatRecord::getSessionId)
                .list();
        return CollStreamUtil.toList(chatRecordList, ChatRecord::getSessionId);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        var chatRecordList = chatRecordService.lambdaQuery()
                .eq(ChatRecord::getSessionId, conversationId)
                .orderByAsc(ChatRecord::getCreateTime)
                .list();
        return CollStreamUtil.toList(chatRecordList, chatRecord -> MessageUtil.toMessage(chatRecord.getData()));
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 先删除原有数据
        this.deleteByConversationId(conversationId);
        // 批量保存数据到数据库
        var chatRecordList = CollStreamUtil.toList(messages, message -> ChatRecord.builder()
                .data(MessageUtil.toJson(message, conversationId))
                .sessionId(conversationId)
                .build());
        this.chatRecordService.saveBatch(chatRecordList);
    }

    @Override
    public void deleteByConversationId(String sessionId) {
        var queryWrapper = Wrappers.<ChatRecord>lambdaQuery()
                .eq(ChatRecord::getSessionId, sessionId);
        this.chatRecordService.remove(queryWrapper);
    }

}

