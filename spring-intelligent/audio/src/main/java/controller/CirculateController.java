package controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import service.node1.Constant;
import service.node1.NodeLink1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/Enlish")
public class CirculateController {

    @Autowired
    private NodeLink1 nodeLink1;

    @PostMapping
    public Object flow(@RequestParam("question") String question) {
        // 获取编译后的状态图
        CompiledGraph compiledGraph = nodeLink1.circulate();
        // 执行状态图，invoke() 方法接受 Map<String, Object> 类型参数
        // 返回 Optional<OverAllState>，通过 map 提取结果
        return compiledGraph.invoke(Map.of(Constant.question, question))
                .map(state -> "question==>" + state.value(Constant.question, "")
                        + "story==>" + state.value(Constant.story, "")
                        + "judge==>" + state.value(Constant.judge, "")
                        + "retryCount==>" + state.value(Constant.RetryCount, 0))
                .orElse("执行失败");
    }
}
