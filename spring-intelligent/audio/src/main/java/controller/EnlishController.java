package controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import service.node1.Node1Link;
import service.node2.Node2Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/Enlish")
public class EnlishController {

    @Autowired
    private Node1Link node1Link;

    @PostMapping
    public Object flow(@RequestParam("word") String word) {
        // 获取编译后的状态图
        CompiledGraph compiledGraph = node1Link.toEnglish();
        // 执行状态图，invoke() 方法接受 Map<String, Object> 类型参数
        // 返回 Optional<OverAllState>，通过 map 提取结果
        return compiledGraph.invoke(Map.of("word", word))
                .map(state -> "==>sentence==>"+state.value("sentence").orElse("null") +
                             "==>translation==>" + state.value("translation").orElse("null")+
                             "==>read==>" + state.value("read").orElse("null"))
                .orElse("执行失败");
    }
}
