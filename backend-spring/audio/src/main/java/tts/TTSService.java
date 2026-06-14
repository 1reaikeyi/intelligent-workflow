package tts;

public interface TTSService {

    /** 生成语音并保存文件 */
    void TTS(String text);

    /** 生成语音，保存文件，同时返回音频字节数据（供前端播放） */
    byte[] TTSform(String text);
}
