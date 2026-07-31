package audio.asr;

import org.springframework.web.multipart.MultipartFile;

public interface ASRService {

    String ASR();

    Object stt(MultipartFile file) throws Exception;
}
