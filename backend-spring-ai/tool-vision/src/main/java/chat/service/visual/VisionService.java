package chat.service.visual;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.io.File;
import java.net.URI;

@Service
public class VisionService {
    @Autowired
    private ChatClient visualChatClient;

    @Autowired
    private ImageBase64Util imageBase64Util;
    /**
     * 本地文件识图
     */
    public String visionByFile(File file, String prompt) throws Exception {
        String base64 = imageBase64Util.toBase64(file);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, URI.create("data:image/jpeg;base64," + base64));
        return visualChatClient.prompt()
                .user(userMessage -> userMessage.text(prompt).media(media))
                .call()
                .content();
    }

    /**
     * 上传文件byte识图（接口常用）
     */
    public String visionByBytes(byte[] imgBytes, String prompt) {
        String base64 = imageBase64Util.toBase64(imgBytes);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, URI.create("data:image/jpeg;base64," + base64));
        return visualChatClient.prompt()
                .user(userMessage -> userMessage.text(prompt).media(media))
                .call()
                .content();
    }


}