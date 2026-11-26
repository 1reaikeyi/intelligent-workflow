package rag.service.memory.mysql;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;
import rag.mapper.ChatRecordMapper;
import rag.model.entity.ChatRecord;

@Service
public class ChatRecorServiceImpl extends ServiceImpl<ChatRecordMapper, ChatRecord>implements ChatRecordService {
}
