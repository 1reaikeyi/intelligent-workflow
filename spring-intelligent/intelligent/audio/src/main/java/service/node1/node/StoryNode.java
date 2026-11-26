package service.node1.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import service.node1.Constant;

import java.util.Map;
@Component
public class StoryNode implements NodeAction {

    @Resource
    private ChatClient chatClient;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String question = state.value(Constant.question,"");
        PromptTemplate promptTemplate = new PromptTemplate("你负责编写搞笑的故事,根据给定的内容{question}");
        promptTemplate.add("question", question);

        String result = this.chat(promptTemplate.render());
        return Map.of(Constant.story, result != null ? result : "null");
    }
    public String chat(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
