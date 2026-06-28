package node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class Sentence implements NodeAction {
    @Resource
    private ChatClient chatClient;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String word = state.value("word","");
        PromptTemplate promptTemplate = new PromptTemplate("你负责英语造句"+
                "根据给定的单词{word}造句,返回一个英文句子");
        promptTemplate.add("word", word);
        String result = chat(promptTemplate.render());
        return Map.of("sentence", result != null ? result : "null");
    }
    public String chat(String word) {
        return chatClient.prompt()
                .user(word)
                .call()
                .content();
    }
}
