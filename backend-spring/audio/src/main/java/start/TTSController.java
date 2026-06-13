
package start;

import java.io.IOException;


import audio.AudioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/audio")
public class TTSController {
   @Autowired
   private AudioService audioService;
    @PostMapping("/tts")
    public void tts(@RequestBody String text) throws IOException {
        audioService.TTS(text);
    }
}
