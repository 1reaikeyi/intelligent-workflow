package audio;

import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

@Service
@Slf4j
public class AudioServiceImpl implements AudioService{
    @Autowired
    private SpeechSynthesisModel speechSynthesisModel;

    private static final String FILE_PATH = "audio/src/main/resources/yu";

    @Override
    public void TTS(String text) {
        SpeechSynthesisResponse response = speechSynthesisModel.call(
                new SpeechSynthesisPrompt(text)
        );
        File file = new File(FILE_PATH , "output.mp3");
        log.info("位置{}", file.getAbsolutePath());
        
        // 在写入文件前确保目标目录存在
        Path parentDir = Paths.get(file.getParent());
        try {
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("创建目录: {}", parentDir.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建存储目录: " + parentDir, e);
        }
        
        try (FileOutputStream outputStream = new FileOutputStream(file.getAbsolutePath())) {
            ByteBuffer byteBuffer = response.getResult().getOutput().getAudio();
            outputStream.write(byteBuffer.array());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void streamTTS(String text) {
        Flux<SpeechSynthesisResponse> response = speechSynthesisModel
                .stream(new SpeechSynthesisPrompt(text));
        CountDownLatch latch = new CountDownLatch(1);
        File file = new File(FILE_PATH ,"output-stream.mp3");
        log.info("位置{}", file.getAbsolutePath());
        
        // 在写入文件前确保目标目录存在
        Path parentDir = Paths.get(file.getParent());
        try {
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("创建目录: {}", parentDir.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建存储目录: " + parentDir, e);
        }
        
        try (FileOutputStream outputStream = new FileOutputStream(file.getAbsolutePath())) {
            response.doFinally(signal -> latch.countDown())
                    .subscribe(synthesisResponse -> {
                ByteBuffer byteBuffer = synthesisResponse.getResult().getOutput().getAudio();
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            });
            latch.await();
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
