package node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


import java.util.Map;

@Component
public class Translation implements NodeAction{
    @Resource
    private ChatClient chatClient;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String sentence = state.value("sentence","");
        PromptTemplate promptTemplate = new PromptTemplate("你是负责英语翻译"+
                "根据给定的{sentence}翻译");
        promptTemplate.add("sentence", sentence);
        Flux<String> response = chatClient.prompt()
                .user(promptTemplate.render())
                .stream()
                .content();
        // 使用 collectList().block() 收集流式响应的所有内容
        String result = response.collectList().block()
                .stream()
                .reduce("", String::concat);
        return Map.of("translation", result);
    }

}
