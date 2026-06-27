package node;


import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
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
    private Sentence sentenceAction;
    @Autowired
    private Translation translationAction;
    @Autowired
    private Read readAction;

    @Bean
    public CompiledGraph toEnlish() {
        KeyStrategyFactory strategyFactory = new KeyStrategyFactory() {
            @Override
            public Map<String, KeyStrategy> apply() {
                return Map.of(
                        "word", new ReplaceStrategy(),
                        "sentence", new ReplaceStrategy(),
                        "translation",new ReplaceStrategy()
                );
            }
        };
        StateGraph graph = new StateGraph("Enlish",strategyFactory);
        //节点
        try {
            graph.addNode("node1", AsyncNodeAction.node_async(sentenceAction));
            graph.addNode("node2", AsyncNodeAction.node_async(translationAction));
            graph.addNode("node3", AsyncNodeAction.node_async(readAction));
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        //边
        try {
            graph.addEdge(StateGraph.START,"node1");
            graph.addEdge("node1","node2");
            graph.addEdge("node2","node3");
            graph.addEdge("node3",StateGraph.END);
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        CompiledGraph compiledGraph = null;
        try {
            compiledGraph = graph.compile();
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        // UML 图生成与打印
        // 使用 PlantUML 格式生成图的可视化表示
        GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "English Flow");
        // 打印 UML 图内容到控制台
        System.out.println("\n=== English Flow UML Diagram ===");
        System.out.println(representation.content());
        System.out.println("==================================\n");
        
        return compiledGraph;
    }
}
