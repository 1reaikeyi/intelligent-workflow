package service.node1;


import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.node1.edge.CirculateEdge;
import service.node1.node.JudgeNode;
import service.node1.node.ReadNode;
import service.node1.node.StoryNode;


import java.util.Map;

@Configuration
public class NodeLink1 {
    @Autowired
    private StoryNode storyAction;
    @Autowired
    private JudgeNode judgeNodeAction;
    @Autowired
    private ReadNode readNodeAction;
    @Autowired
    private CirculateEdge circulateEdge;
/**
 *      类	            合并语义	适用场景
    ReplaceStrategy	新值覆盖旧值	单值结果（你现在判断、故事、计数等）
    AppendStrategy	新值追加到 List 末尾	消息历史、多轮记录、多个结果累积
    MergeStrategy	新旧值都是 Map，合并 key	配置、字典类状态
 */
    @Bean
    public CompiledGraph circulate() {
        KeyStrategyFactory strategyFactory = new KeyStrategyFactory() {
            @Override
            public Map<String, KeyStrategy> apply() {
                return Map.of(
                        Constant.question, new ReplaceStrategy(),
                        Constant.story, new AppendStrategy(),
                        Constant.judge, new ReplaceStrategy(),
                        Constant.read, new ReplaceStrategy(),
                        Constant.RetryCount, new ReplaceStrategy()
                );
            }
        };
        StateGraph graph = new StateGraph("circulate",strategyFactory);
        //节点
        try {
            graph.addNode("node1", AsyncNodeAction.node_async(storyAction));
            graph.addNode("node2", AsyncNodeAction.node_async(judgeNodeAction));
            graph.addNode("node3", AsyncNodeAction.node_async(readNodeAction));
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        //边
        try {
            graph.addEdge(StateGraph.START,"node1");
            graph.addEdge("node1", "node2");
            //条件边：审核通过走 node3（语音），不通过回到 node1 重写（超过最大次数强制结束）
            graph.addConditionalEdges("node2",
                    AsyncEdgeAction.edge_async(circulateEdge),
                    Map.of(
                            Constant.RoutePass, "node3",
                            Constant.RouteRetry, "node1"
                    ));
            graph.addEdge("node3", StateGraph.END);
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
