package start;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import stt.STTService;

@RestController
@RequestMapping("/audio")
public class STTController {
    @Autowired
    private STTService sttService;

    @PostMapping("/stt")
    public String chat(MultipartFile file) {
        return sttService.stt(file);
    }
}
