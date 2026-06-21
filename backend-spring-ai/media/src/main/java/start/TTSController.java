
package start;

import tts.TTSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/audio")
public class TTSController {
   @Autowired
   private TTSService TTSService;

    /** 仅保存文件 */
    @PostMapping("/tts")
    public void tts(@RequestBody String text) {
        TTSService.TTS(text);
    }

    /** 保存文件并返回音频流 */
    @PostMapping("/tts/play")
    public ResponseEntity<byte[]> ttsPlay(@RequestBody String text) {
        byte[] audioData = TTSService.TTSform(text);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("tts/mpeg"))
                .body(audioData);
    }
}
