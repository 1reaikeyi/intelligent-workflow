package rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.springframework.stereotype.Repository;
import rag.model.entity.ChatRecord;

@Repository
public interface ChatRecordMapper extends BaseMapper<ChatRecord> {
}
