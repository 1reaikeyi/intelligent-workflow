
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

    @PostMapping("/tts")
    public ResponseEntity<byte[]> ttsPlay(@RequestBody String text) {
        byte[] audioData = TTSService.TTS(text);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("tts/mpeg"))
                .body(audioData);
    }
}
