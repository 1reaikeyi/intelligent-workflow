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
public class JudgeNode implements NodeAction {
    @Resource
    private ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String story = state.value(Constant.story,"");
        String question = state.value(Constant.question,"");
        int retryCount = state.value(Constant.RetryCount, 0);
        PromptTemplate promptTemplate = new PromptTemplate("你负责审核故事。"+
                "判断给定的故事{story}是否和主题{question}相关、内容是否完整。"+
                "必须包含PASS 或 RETRY");
        promptTemplate.add("story", story);
        promptTemplate.add("question", question);

        String result = this.chat(promptTemplate.render());
        String pass = result != null && result.contains("PASS") ? "PASS" : "RETRY";

        return Map.of(
                Constant.judge, pass,
                Constant.RetryCount, retryCount + 1);
    }
    public String chat(String story) {
        return chatClient.prompt()
                .user(story)
                .call()
                .content();
    }
}
