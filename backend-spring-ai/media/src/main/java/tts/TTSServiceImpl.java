package tts;
import com.alibaba.cloud.ai.dashscope.audio.tts.DashScopeAudioSpeechModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.*;
import java.nio.file.Files;

@Service
@Slf4j
public class TTSServiceImpl implements TTSService {
    @Autowired
    private DashScopeAudioSpeechModel dashScopeAudioSpeechModel;

    @Override
    public void TTS(String text) {
        saveAudio(generateAudio(text));
    }

    @Override
    public byte[] TTSform(String text) {
        byte[] audio = generateAudio(text);
        saveAudio(audio);
        return audio;
    }

    /** 生成音频字节数据 */
    private byte[] generateAudio(String text) {
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
        Flux<byte[]> audioFlux = dashScopeAudioSpeechModel.stream(prompt)
                .map(res -> res.getResult().getOutput());
        return audioFlux.collectList()
                .map(chunks -> {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    chunks.forEach(baos::writeBytes);
                    return baos.toByteArray();
                }).block();
    }

    /** 保存音频文件 */
    private void saveAudio(byte[] audio) {
        String fileName = System.currentTimeMillis() + ".mp3";
        File directory = new File("D:/a.github/ai-assistant/backend-spring/audio");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        try {
            Files.write(new File(directory, fileName).toPath(), audio);
            log.info("音频文件保存成功：{}", fileName);
        } catch (IOException e) {
            log.error("保存音频文件失败：{}", e.getMessage(), e);
        }
    }
}