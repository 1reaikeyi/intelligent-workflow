package start;


import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeTranscriptionApiSpec;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
public class STT {
    @Autowired
    private DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel;
    @Value("${spring.ai.dashscope.api-key}")
    String api;
    @PostMapping("/stt")
    public String stt() {
        // 构造请求体
        Map<String, Object> requestBody = Map.of(
                "model", "paraformer-v2",
                "input", Map.of("file_urls", List.of("https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav")),
                "parameters", Map.of("channel_id", List.of(0))
        );

        // 发起 HTTP POST 请求
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer "+api);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-DashScope-Async", "enable"); // 必须启用异步

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription",
                request,
                String.class
        );

        return response.getBody(); // 返回任务ID等响应信息
    }
    @GetMapping("{task_id}")
    public String get(@PathVariable String task_id) {
        String url = "https://dashscope.aliyuncs.com/api/v1/tasks/" + task_id;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + api);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                String.class
        );
        return response.getBody();
    }

}
