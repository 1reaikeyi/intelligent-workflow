package tts;

public interface TTSService {
    /**
     * 文字转语音（TTS）
     *
     * @param text 待合成的文本内容
     * @return 异步响应输出
     */
    byte[] TTS(String text);
}
