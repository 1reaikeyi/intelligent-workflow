package node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Translation implements NodeAction{
    @Resource
    private ChatClient chatClient;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String sentence = state.value("sentence","");
        PromptTemplate promptTemplate = new PromptTemplate("你是负责英语翻译"+
                "根据给定的{sentence}翻译成中文");
        promptTemplate.add("sentence", sentence);
        String result = chatClient.prompt()
                .user(promptTemplate.render())
                .call()
                .content();
        return Map.of("translation", result != null ? result : "null");
    }

}
