package chat.service.visual;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;


@Service
public class VisionService {
    @Autowired
    private ChatClient visualChatClient;
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
        return visualChatClient.prompt()
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
        return visualChatClient.prompt()
                .user(userMessage -> userMessage.text(prompt).media(media))
                .call()
                .content();
    }





}