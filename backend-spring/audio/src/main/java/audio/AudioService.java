package audio;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

public interface AudioService {

    void TTS(String text);
}
