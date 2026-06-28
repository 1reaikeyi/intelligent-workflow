package tts;

public interface TTSService {
    /**
     * 文字转语音（TTS）
     *
     * @param text 待合成的文本内容
     * @return 音频字节数组
     */
    byte[] TTS(String text);
}
