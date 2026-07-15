package chat.start;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class ASRContrller {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @PostMapping(value = "/asr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> transcribe(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 生成随机 boundary
            String boundary = UUID.randomUUID().toString().replace("-", "");

            // 手动构建 multipart/form-data 请求体
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 文件部分
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + 
                    file.getOriginalFilename() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Type: " + file.getContentType() + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write(file.getBytes());
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // 模型参数部分
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"model\"\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write("FunAudioLLM/SenseVoiceSmall".getBytes(StandardCharsets.UTF_8));
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // 结束 boundary
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            // 构建 HTTP 请求
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/audio/transcriptions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                    .build();

            // 发送请求并获取响应
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            // 解析响应
            String responseBody = new String(response.body(), StandardCharsets.UTF_8);
            JSONObject jsonObject = JSONUtil.parseObj(responseBody);
            String transcription = jsonObject.getStr("text");

            result.put("success", true);
            result.put("transcription", transcription);
            result.put("filename", file.getOriginalFilename());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "转录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
