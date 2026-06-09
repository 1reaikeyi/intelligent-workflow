package audio;
import com.alibaba.cloud.ai.dashscope.audio.tts.DashScopeAudioSpeechModel;
import com.alibaba.cloud.ai.dashscope.audio.tts.DashScopeAudioSpeechOptions;
import com.alibaba.cloud.ai.dashscope.audio.tts.DashScopeTTSApiSpec;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

@Service
@Slf4j
public class AudioServiceImpl implements AudioService{
    @Autowired
    private DashScopeAudioSpeechModel dashScopeAudioSpeechModel;

    @Override
    public void TTS(String text) {
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);

        Flux<byte[]> audioFlux = dashScopeAudioSpeechModel.stream(prompt)
                .map(res -> res.getResult().getOutput());

        byte[] fullAudio = audioFlux.collectList()
                .map(chunks -> {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    chunks.forEach(baos::writeBytes);
                    return baos.toByteArray();
                }).block();

        String fileName = System.currentTimeMillis() + ".mp3";
        // 指定音频文件保存目录
        File directory = new File("audio/src/main/resources/yu");
        // 如果目录不存在则创建
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, fileName);
        try {
            // 使用 Files.write 将音频数据写入文件
            Path path = file.toPath();
            Files.write(path, fullAudio);
            log.info("音频文件保存成功：{}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("保存音频文件失败：{}", e.getMessage(), e);
        }
    }


}
