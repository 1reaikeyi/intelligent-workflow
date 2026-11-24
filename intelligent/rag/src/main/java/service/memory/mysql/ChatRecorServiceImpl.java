package service.memory.mysql;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import mapper.ChatRecordMapper;
import model.entity.ChatRecord;
import org.springframework.stereotype.Service;

@Service
public class ChatRecorServiceImpl extends ServiceImpl<ChatRecordMapper, ChatRecord>implements ChatRecordService {
}
