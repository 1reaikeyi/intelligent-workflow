package chat.controller;

import chat.service.visual.VisionService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping
public class VisionController {

    @Value("${image.max-width:2048}")
    private int maxWidth;
    @Value("${image.max-height:2048}")
    private int maxHeight;

    @Resource
    private VisionService visionService;

    @PostMapping("/vision")
    public String recognizeImg(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", defaultValue = "描述图片+文字") String prompt) throws IOException {
        // 校验图片尺寸
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > maxWidth || height > maxHeight) {
            throw new IllegalArgumentException(
                    String.format("图片尺寸超出限制：当前 %dx%d，最大允许 %dx%d",
                            width, height, maxWidth, maxHeight));
        }
        return visionService.visionByBytes(file.getBytes(), prompt);
    }
}