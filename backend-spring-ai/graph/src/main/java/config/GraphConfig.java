//package config;
//
//import com.alibaba.cloud.ai.graph.*;
//import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
//import com.alibaba.cloud.ai.graph.action.NodeAction;
//import com.alibaba.cloud.ai.graph.exception.GraphStateException;
//import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Map;
//
//@Configuration
//public class GraphConfig {
//    @Bean("quick")
//    public CompiledGraph quick() {
//        /**
//         * ReplaceStrategy 替换策略：新值直接覆盖旧值
//         * AppendStrategy 追加策略：将新值追加到旧值集合中
//         * IgnoreStrategy 忽略策略：忽略新值，保持旧值不变
//         */
//        KeyStrategyFactory strategyFactory = new KeyStrategyFactory() {
//            @Override
//            public Map<String, KeyStrategy> apply() {
//                return Map.of("input1",new ReplaceStrategy(), "input2",new ReplaceStrategy());
//            }
//        };
//        StateGraph graph = new StateGraph("quick",strategyFactory);
//        //节点
//        try {
//            graph.addNode("node1", AsyncNodeAction.node_async(new NodeAction() {
//                @Override
//                public Map<String, Object> apply(OverAllState state) throws Exception {
//                    return Map.of("input1",1,"input2",1);
//                }
//            }));
//            graph.addNode("node2", AsyncNodeAction.node_async(new NodeAction() {
//                @Override
//                public Map<String, Object> apply(OverAllState state) throws Exception {
//                    return Map.of("input1",2,"input2",2);
//                }
//            }));
//        } catch (GraphStateException e) {
//            throw new RuntimeException(e);
//        }
//        //边
//        try {
//            graph.addEdge(StateGraph.START,"node1");
//            graph.addEdge("node1","node2");
//            graph.addEdge("node2",StateGraph.END);
//        } catch (GraphStateException e) {
//            throw new RuntimeException(e);
//        }
//        CompiledGraph compiledGraph = null;
//        try {
//            compiledGraph = graph.compile();
//        } catch (GraphStateException e) {
//            throw new RuntimeException(e);
//        }
//        return compiledGraph;
//    }
//}
