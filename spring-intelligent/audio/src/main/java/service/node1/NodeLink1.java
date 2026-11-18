package service.node1;


import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class NodeLink1 {
    @Autowired
    private Sentence sentenceAction;
    @Autowired
    private Translation translationAction;
    @Autowired
    private Read readAction;

    @Bean
    public CompiledGraph toEnglish() {
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
            graph.addNode("service/node2", AsyncNodeAction.node_async(translationAction));
            graph.addNode("node3", AsyncNodeAction.node_async(readAction));
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        //边
        try {
            graph.addEdge(StateGraph.START,"node1");
            graph.addEdge("node1", "service/node2");
            graph.addEdge("service/node2","node3");
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

        GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "English Flow");
        // 打印 UML 图内容到控制台
        System.out.println("===UML Diagram ===");
        System.out.println(representation.content());
        System.out.println("==================================");
        
        return compiledGraph;
    }
}
