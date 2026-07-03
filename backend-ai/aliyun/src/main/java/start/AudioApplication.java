package start;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 聊天对话 -> ChatModel
 * 文本向量化 -> EmbeddingModel
 * 图像生成 -> ImageModel
 * 语音合成 -> TextToSpeechModel
 * 语音识别 -> SpeechToTextModel
 */
// 日志配置
@Slf4j
//主程序入口
@SpringBootApplication
@ComponentScan({"tts","start","stt"})
public class AudioApplication {
    public static void main(String[] args){
        SpringApplication.run(AudioApplication.class, args);
        log.info("开启成功");
    }
}
