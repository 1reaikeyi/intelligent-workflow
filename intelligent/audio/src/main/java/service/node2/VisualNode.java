package service.node2;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;

@Service
public class VisualNode implements NodeAction {
   
    private static final String IMAGE_DATA_URI_PREFIX = "data:image/jpeg;base64,";

    @Resource(name = "visualChatClient")
    private ChatClient visualClient;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String fileBase64 = (String) state.value("file").orElse(("文件为空"));
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, URI.create("data:image/jpeg;base64," + fileBase64));
        String result = result(media);
        return Map.of("visualResult", result != null ? result : null);
    }

    public String result(Media media) {
        return visualClient.prompt()
                .user(promptUserSpec -> promptUserSpec.text("识别有哪些计算机技术？").media(media))
                .call()
                .content();
    }


}
