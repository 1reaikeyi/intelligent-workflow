package node;

import com.alibaba.cloud.ai.dashscope.audio.tts.DashScopeAudioSpeechModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
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
    private DashScopeAudioSpeechModel dashScopeAudioSpeechModel;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String text = state.value("sentence","") +
                state.value("translation","");
        byte[] audio = chat(text);

        // 2. 保存音频文件到本地
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".mp3";
        File dir = new File("D:\\a.github\\ai-assistant\\backend-spring-ai\\aliyun\\mp3");
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
    public byte[] chat(String text){
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
}
