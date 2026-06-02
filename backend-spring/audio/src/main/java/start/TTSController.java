
package start;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import audio.AudioService;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import jakarta.annotation.PreDestroy;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;


@RestController
@RequestMapping("/audio")
public class TTSController {
   @Autowired
   private AudioService audioService;
    @PostMapping("/tts")
    public void tts(@RequestBody String text) throws IOException {
        audioService.TTS(text);
    }
    @PostMapping("/tts-stream")
    public void streamTTS(@RequestBody String text) {

    }


}
