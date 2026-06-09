
package start;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


import audio.AudioService;

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
}
