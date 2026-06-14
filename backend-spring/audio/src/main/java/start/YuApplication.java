package start; // 注意：包不要写错了

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


import java.net.UnknownHostException;

// 日志配置
@Slf4j
//主程序入口
@SpringBootApplication
@ComponentScan({"tts","start","stt"})
public class YuApplication {
    public static void main(String[] args) throws UnknownHostException {
        SpringApplication.run(YuApplication.class, args);
        log.info("开启成功");
    }
}
/**
 * 聊天对话 -> ChatModel
 * 文本向量化 -> EmbeddingModel
 * 图像生成 -> ImageModel
 * 语音合成 -> TextToSpeechModel
 * 语音识别 -> SpeechToTextModel
 */