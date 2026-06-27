package stt;

import org.springframework.web.multipart.MultipartFile;

public interface STTService {

    /**
     * 语音转文字（STT）
     * @param audioFile 音频文件
     * @return 识别结果文本
     */
    String stt(MultipartFile audioFile);
}
