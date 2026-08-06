package chat.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.util.Map;

@Service
public class VisualNode implements NodeAction {
    @Resource(name = "visualChatClient")
    private ChatClient visualClient;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String base64 = (String) state.value("file").orElse(("文件为空"));
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, URI.create("data:image/jpeg;base64," + base64));
        String result = result(media);
        return Map.of("visualResult", result != null ? result : null);
    }
    public String result(Media media) {
        return visualClient.prompt()
                .user(promptUserSpec -> promptUserSpec.text("识别有哪些食物,饮料？").media(media))
                .call()
                .content();
    }
}
