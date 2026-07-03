package chat.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ToolNode implements NodeAction {
    @Resource(name = "toolClient")
    private ChatClient toolClient;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String question = state.value("question").toString();
        String input = state.value("visualResult").toString();
        PromptTemplate promptTemplate = new PromptTemplate("你负责查询"+
                "根据信息{input}查询");
        promptTemplate.add("input", input);

        String result = result(question, input);
        return Map.of("toolResult", result != null ? result : "没有查询到内容");
    }
    public String result(String question, String input) {
        return  toolClient.prompt()
                .user(promptUserSpec -> promptUserSpec.text(input))
                .call()
                .content();
    }
}
