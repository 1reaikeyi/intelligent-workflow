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
    public byte[] TTS(String text) {
        // 流式合成：调用 stream() 逐块获取音频数据后合并
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
        Flux<byte[]> audioFlux = dashScopeAudioSpeechModel.stream(prompt)
                .map(res -> res.getResult().getOutput());
        byte[] audio = audioFlux.collectList()
                .map(chunks -> {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    chunks.forEach(baos::writeBytes);
                    return baos.toByteArray();
                }).block();
        // 保存音频文件到本地
        String fileName = System.currentTimeMillis() + ".mp3";
        File path = new File("D:\\a.github\\ai-assistant\\backend-spring-ai\\audio\\mp3");
        if (!path.exists()) {
            path.mkdirs();
        }
        try (OutputStream outputStream = new FileOutputStream(new File(path, fileName))) {
            outputStream.write(audio);
            log.info("音频文件保存成功：{}", fileName);
        } catch (IOException e) {
            log.error("保存音频文件失败：{}", e.getMessage(), e);
        }
        return audio;
    }
}
