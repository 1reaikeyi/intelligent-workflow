package start;

import audio.asr.ASRService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ASRController {
    @Autowired
    private ASRService asrService;
    @GetMapping("/asr")
    public String asr() {
        String result = asrService.ASR();
        return result;
    }
    @PostMapping("/stt/base64")
    public String base64(@RequestParam("file") MultipartFile file) {
        try {
            // 使用注入的 asrService（Spring管理的bean），确保内部依赖已初始化
            return asrService.stt(file).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
