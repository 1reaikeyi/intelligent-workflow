package start;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import node.NodeLink;
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
    private NodeLink nodeLink;
    
    /**
     * 执行英语学习流程：单词 -> 造句 -> 翻译
     * @param word 输入的英文单词
     * @return 翻译结果
     */
    @PostMapping
    public Object flow(@RequestParam("word") String word) {
        // 获取编译后的状态图
        CompiledGraph compiledGraph = nodeLink.toEnlish();
        // 执行状态图，invoke() 方法接受 Map<String, Object> 类型参数
        // 返回 Optional<OverAllState>，通过 map 提取结果
        return compiledGraph.invoke(Map.of("word", word))
                .map(state -> "sentence:: "+state.value("sentence").orElse("null") +
                             "translation::" + state.value("translation").orElse("null"))
                .orElse("执行失败");
    }
}
