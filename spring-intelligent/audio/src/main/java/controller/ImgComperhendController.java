package controller;

import service.node2.Node2Link;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
public class ImgComperhendController {
    @Autowired
    private Node2Link node2Link;
    @PostMapping("/see")
    public Object flow(@RequestParam MultipartFile file,
                       @RequestParam String question) throws IOException {
        // 获取编译后的状态图
        CompiledGraph compiledGraph = node2Link.comprehend();

        String fileBase64 = Base64.getEncoder().encodeToString(file.getBytes());
        return compiledGraph.invoke(Map.of("file", fileBase64, "question", question))
                .map(state -> "==>1"+state.value("visualResult").orElse("null") +
                        "==>2==>" + state.value("toolResult").orElse("null"))
                .orElse("执行失败");
    }
}
