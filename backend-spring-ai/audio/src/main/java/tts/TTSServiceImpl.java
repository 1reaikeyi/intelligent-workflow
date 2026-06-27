package tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
@Slf4j
public class TTSServiceImpl implements TTSService {
    @Autowired
    private OpenAiAudioSpeechModel openAiAudioSpeechModel;

    @Override
    public byte[] TTS(String text) {
        // 1. 调用 TTS 模型生成音频
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
        TextToSpeechResponse response = openAiAudioSpeechModel.call(prompt);
        byte[] audio = response.getResult().getOutput();

        // 2. 保存音频文件到本地
        String fileName = System.currentTimeMillis() + ".mp3";
        File directory = new File("D:\\a.github\\ai-assistant\\backend-spring-ai\\audio\\yu");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        try {
            Files.write(new File(directory, fileName).toPath(), audio);
            log.info("音频文件保存成功：{}", fileName);
        } catch (IOException e) {
            log.error("保存音频文件失败：{}", e.getMessage(), e);
        }
        return audio;
    }
}