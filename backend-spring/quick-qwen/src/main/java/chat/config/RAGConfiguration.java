package chat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RAGConfiguration {
    @Bean
    public ChatClient ragClient(OpenAiChatModel model,
                                @Qualifier("loggerAdvisor") Advisor loggerAdvisor,
                                @Qualifier("messageWindowAdvisor") Advisor messageMemoryAdvisor,
                                VectorStore vectorStore) {
        return ChatClient
                .builder(model)
                .defaultSystem("你是一个游戏助手")
                .defaultAdvisors(loggerAdvisor, messageMemoryAdvisor)
                .defaultAdvisors(questionAnswerAdvisor(vectorStore))
                .build();
    }

    //目前是jvm
    @Bean
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder()
                                // 相似度阈值
                                .similarityThreshold(0.5)
                                // 检索最相关的3条
                                .topK(3)
                                .build()
                )
                // 可选：自定义提示模板（提升回答质量,使用template）
//                .promptTemplate(myPromptTemplate)
                .build();
    }
}
