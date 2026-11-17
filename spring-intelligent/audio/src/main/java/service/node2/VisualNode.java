package service.node2;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
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
                .user(promptUserSpec -> promptUserSpec.text("识别有哪些技术桟？").media(media))
                .call()
                .content();
    }

    /**
     * 文件图片转 base64
     */
    private String toBase64(File imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", bos);
        byte[] bytes = bos.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }
    /**
     * 本地文件识图
     */
    public String visionByFile(File file, String prompt) throws Exception {
        String base64 = toBase64(file);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, URI.create("data:image/jpeg;base64," + base64));
        return visualClient.prompt()
                .user(userMessage -> userMessage.text(prompt).media(media))
                .call()
                .content();
    }

    /**
     * byte数组图片转base64
     */
    private String toBase64(byte[] imageBytes) {
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    /**
     * 上传文件byte识图（接口常用）
     */
    public String visionByBytes(byte[] imgBytes, String prompt) {
        String base64 = toBase64(imgBytes);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, URI.create("data:image/jpeg;base64," + base64));
        return visualClient.prompt()
                .user(userMessage -> userMessage.text(prompt).media(media))
                .call()
                .content();
    }
}
