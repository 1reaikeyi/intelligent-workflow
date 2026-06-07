package node;


import com.alibaba.cloud.ai.graph.CompiledGraph;
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

    @Bean("Enlish")
    public CompiledGraph toEnlish() {
        KeyStrategyFactory strategyFactory = new KeyStrategyFactory() {
            @Override
            public Map<String, KeyStrategy> apply() {
                return Map.of(
                        "word", new ReplaceStrategy(),
                        "sentence", new ReplaceStrategy()
                );
            }
        };
        StateGraph graph = new StateGraph("Enlish",strategyFactory);
        //节点
        try {
            graph.addNode("node1", AsyncNodeAction.node_async(sentenceAction));
            graph.addNode("node2", AsyncNodeAction.node_async(translationAction));
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
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        return compiledGraph;
    }
}
