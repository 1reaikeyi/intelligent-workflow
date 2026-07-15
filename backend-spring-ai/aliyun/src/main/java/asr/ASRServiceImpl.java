package asr;

import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAsrTranscriptionApiSpec;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionPrompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
public class ASRServiceImpl implements ASRService {
    @Autowired
    private DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel;

    @Override
    public String ASR() {
        DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
//                .model(DashScopeModel.AudioModel.FUN_ASR.getValue())
                .model("paraformer-v2")
                .build();
        String url = "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav";
        List<String> fileUrls = new ArrayList<>();
        fileUrls.add(url);

        DashScopeAudioTranscriptionPrompt prompt = new DashScopeAudioTranscriptionPrompt(options, fileUrls);
        AudioTranscriptionResponse response = dashScopeAudioTranscriptionModel.call(prompt);

        // 强转为通义大模型ASR返回实体，遍历结果
        DashScopeAsrTranscriptionApiSpec.DashScopeAudioAsrTranscriptionResponse responseResult
                = (DashScopeAsrTranscriptionApiSpec.DashScopeAudioAsrTranscriptionResponse) response;
        StringBuilder totalText = new StringBuilder();
        if (responseResult != null && responseResult.getTranscriptionResults() != null) {
            responseResult.getTranscriptionResults().forEach(result -> {
                if (result.transcripts() != null) {
                    result.transcripts().forEach(transcript -> {
                        String text = transcript.getText();
                        totalText.append(text);
                        System.out.println("音频文件地址：" + url + "，识别文本内容：" + text);
                    });
                }
            });
        }
        return totalText.toString();
    }
    /**
     * 音频文件转写接口（接收上传的音频文件）
     * 使用 Base64 编码方式将音频数据发送到阿里云 DashScope ASR 服务
     */
    @Override
    public Object stt(MultipartFile file) throws Exception {
        // 1. 从文件名中提取音频格式（如 mp3, wav, m4a 等），避免硬编码
        String originalFilename = file.getOriginalFilename();
        String audioFormat = "mp3"; // 默认格式
        if (originalFilename != null && originalFilename.contains(".")) {
            audioFormat = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        // 2. 将音频文件转换为 Base64
        String base64Audio = Base64.getEncoder().encodeToString(file.getBytes());

        // 3. 构造 Options，使用 paraformer-v2 模型（支持同步 base64 方式）
        DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
                .model("paraformer-v2")
                .build();

        // 4. 按照 SDK 源码结构，构造 InputAudio 和 Content 对象
        DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.InputAudio inputAudio = 
                new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.InputAudio(base64Audio, audioFormat);
        DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.Content content = 
                new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.Content("input_audio", inputAudio);
        DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage message = 
                new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage(List.of(content));

        // 5. 使用 (options, TranscriptionUserMessage) 构造器创建 Prompt
        DashScopeAudioTranscriptionPrompt prompt = new DashScopeAudioTranscriptionPrompt(options, message);

        // 6. 调用模型进行转写
        AudioTranscriptionResponse response = dashScopeAudioTranscriptionModel.call(prompt);

        return response.getResult().getOutput();
    }

}
