package service.memory;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
@Service
public class RedisChatMemoryReposity implements ChatMemoryRepository {
    private static final String DEFAULT_PREFIX = "chat:";
    private final String prefix;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public RedisChatMemoryReposity() {
        this(DEFAULT_PREFIX);
    }
    public RedisChatMemoryReposity(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = this.stringRedisTemplate.keys(this.prefix + "*");
        if (null == keys) {
            return List.of();
        }
        return keys.stream()
                .map(key -> StrUtil.replace(key, this.prefix, ""))
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return List.of();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.notEmpty(messages, "消息列表不能为空");
        var redisKey = this.getKey(conversationId);
        var listOps = this.stringRedisTemplate.boundListOps(redisKey);
        // 保存数据时，会传入全部的消息数据，包括之前的数据，所以需要先删除之前的数据，再添加新的数据
        this.deleteByConversationId(conversationId);
        // 将消息序列化并添加到Redis列表的右侧
        messages.forEach(message -> listOps.rightPush(JSONUtil.toJsonStr(message)));
//        messages.forEach(message -> listOps.rightPush(MessageUtil.toJson(message, conversationId)));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        var redisKey = this.getKey(conversationId);
        this.stringRedisTemplate.delete(redisKey);
    }
    private String getKey(String conversationId) {
        return this.prefix+conversationId;
    }
}
