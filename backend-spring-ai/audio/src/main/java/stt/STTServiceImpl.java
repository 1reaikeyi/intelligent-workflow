package stt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
@Slf4j
public class STTServiceImpl implements STTService {
    @Autowired
    private OpenAiAudioTranscriptionModel audioTranscriptionModel;

    @Override
    public String stt(MultipartFile multipartFile) {
        // 将MultipartFile转换为Resource
        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(multipartFile.getResource());
        // 调用OpenAiAudioTranscriptionModel进行语音识别
        AudioTranscriptionResponse response = audioTranscriptionModel.call(transcriptionRequest);
        // 获取识别结果
        String output = response.getResult().getOutput();
        return output;
    }

}