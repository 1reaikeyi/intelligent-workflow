package node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
@Component
public class Read implements NodeAction {
    @Autowired
    private OpenAiAudioSpeechModel openAiAudioSpeechModel;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String text = state.value("sentence","") +
                state.value("translation","");
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
        TextToSpeechResponse response = openAiAudioSpeechModel.call(prompt);
        byte[] audio = response.getResult().getOutput();

        // 2. 保存音频文件到本地
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".mp3";
        File dir = new File("D:\\a.github\\ai-assistant\\backend-spring-ai\\graph\\mp3");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File path = new File(dir.getAbsolutePath(), fileName);
        try (OutputStream outputStream = new FileOutputStream(path)) {
            outputStream.write(audio);
            outputStream.flush();
        } catch (IOException e) {
            throw new IOException("音频文件保存失败", e);
        }
        return Map.of("read", path.getAbsolutePath());
    }
}
