package chat.node;


import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class NodeLink {
    @Autowired
    private VisualNode visualNode;
    @Autowired
    private ToolNode toolNode;
    @Bean
    public CompiledGraph comprehend() {
        KeyStrategyFactory strategyFactory = new KeyStrategyFactory() {
            @Override
            public Map<String, KeyStrategy> apply() {
                return Map.of(
                        "visualResult", new ReplaceStrategy(),
                        "toolResult", new ReplaceStrategy()
                );
            }
        };
        StateGraph graph = new StateGraph("img-comprehend",strategyFactory);
        //节点
        try {
            graph.addNode("node1", AsyncNodeAction.node_async(visualNode));
            graph.addNode("node2", AsyncNodeAction.node_async(toolNode));
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        //边
        try {
            graph.addEdge(StateGraph.START,"node1");
            graph.addEdge("node1","node2");
            graph.addEdge("node2",StateGraph.END);
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }

        CompiledGraph compiledGraph = null;
        try {
            compiledGraph = graph.compile();
            // UML 图生成与打印
            GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "English Flow");
            // 打印 UML 图内容到控制台
            System.out.println("=== English Flow UML Diagram ===");
            System.out.println(representation.content());
            System.out.println("==================================");
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        return compiledGraph;
    }
}
