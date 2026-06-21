package node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
@Component
public class Sentence implements NodeAction {
    @Resource
    private ChatClient chatClient;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String word = state.value("word","");
        PromptTemplate promptTemplate = new PromptTemplate("你负责英语造句"+
                "根据给定的单词{word}造句，返回一个");
        promptTemplate.add("word", word);
        Flux<String> response = chatClient.prompt()
                .user(promptTemplate.render())
                .stream()
                .content();
        // 使用 collectList().block() 收集流式响应的所有内容
        String result = response.collectList().block()
                .stream()
                .reduce("", String::concat);
        return Map.of("sentence", result);
    }
}
