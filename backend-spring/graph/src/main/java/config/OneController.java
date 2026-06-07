//package start;
//
//import com.alibaba.cloud.ai.graph.CompiledGraph;
//import com.alibaba.cloud.ai.graph.OverAllState;
//import com.alibaba.cloud.ai.graph.RunnableConfig;
//import config.GraphConfig;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import reactor.core.publisher.Flux;
//
//import java.util.Map;
//import java.util.Optional;
//
///**
// * 状态图执行控制器
// * 使用阿里云AI图引擎执行预定义的状态图
// */
//@RestController
//@RequestMapping("/flow")
//public class OneController {
//
//    // 注入GraphConfig，通过它获取编译后的状态图
//    @Autowired
//    private GraphConfig graphConfig;
//    @GetMapping
//    public String flow() {
//        CompiledGraph compiledGraph = graphConfig.quick();
//        // 执行状态图并返回结果
//        return compiledGraph.invoke(Map.of())
//                .map(state -> "input1=" + state.value("input1").orElse("null") +
//                             ", input2=" + state.value("input2").orElse("null"))
//                .orElse("执行失败");
//    }
//
//}
