package start.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RagConfiguratioon {

    /**
     * 配置 RAG 客户端配置
     */
    @Bean
    public ChatClient ragClient(OpenAiChatModel model,
                                 @Qualifier("loggerAdvisor") Advisor loggerAdvisor,
                                 @Qualifier("messageWindowAdvisor") Advisor messageWindowAdvisor) {  // 日志记录器
        return ChatClient.builder(model)
                .defaultAdvisors(loggerAdvisor, messageWindowAdvisor) //添加 Advisor 功能增强
                .build();
    }

    /**
     * 配置 Redis 连接池
     * 用于 RAG（检索增强生成）功能的向量数据库
     *
     * @return JedisPooled 实例
     */
    @Value("${spring.ai.vectorstore.redis.password}")
    private String auth;
    @Value("${spring.ai.vectorstore.redis.port:16379}")
    private int redisPort;
    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled("redis://:"+auth+"@192.168.80.128:"+redisPort);
    }

    @Bean
    public VectorStore vectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("spring-ai-index")
                .prefix("embedding:")
                .initializeSchema(true)
                .build();
    }
}
