//package config;
//
//import static com.alibaba.cloud.ai.graph.StateGraph.END;
//import static com.alibaba.cloud.ai.graph.StateGraph.START;
//import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
//
//import com.alibaba.cloud.ai.graph.GraphRepresentation;
//import com.alibaba.cloud.ai.graph.OverAllState;
//import com.alibaba.cloud.ai.graph.StateGraph;
//import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
//import com.alibaba.cloud.ai.graph.action.EdgeAction;
//import com.alibaba.cloud.ai.graph.action.NodeAction;
//import com.alibaba.cloud.ai.graph.exception.GraphStateException;
//import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.stream.Collectors;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RoutingGraphConfiguration {
//
//  private static Map<String, Object> availableRoutes = new HashMap<>();
//
//  static {
//    availableRoutes.put("billing", "You are a billing specialist. Help resolve billing issues...");
//    availableRoutes.put("technical",
//        "You are a technical support engineer. Help solve technical problems...");
//    availableRoutes.put("general",
//        "You are a customer service representative. Help with general inquiries...");
//  }
//
//  @Bean
//  public StateGraph routingGraph(ChatModel chatModel) throws GraphStateException {
//    ChatClient client = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor())
//        .build();
//
//    StateGraph graph = new StateGraph("ParallelDemo",
//        () -> Map.of("inputText", new ReplaceStrategy(), "selectionLlm", new ReplaceStrategy()))
//        // 注册节点
//        .addNode("routing", node_async(new LlmCallRouterNode(client, "inputText")))
//        .addNode("billing", node_async(new SelectionLlmNode(client)))
//        .addNode("technical", node_async(new SelectionLlmNode(client)))
//        .addNode("general", node_async(new SelectionLlmNode(client)))
//        // 构建并行边：使用单条边携带多目标
//        .addEdge(START, "routing")
//        .addConditionalEdges("routing", AsyncEdgeAction.edge_async(new EdgeAction() {
//          @Override
//          public String apply(OverAllState state) {
//            String selection = (String) state.value("selectionLlm").orElse("");
//            return selection;
//          }
//        }), Map.of("billing", "billing", "technical", "technical", "general", "general"))
//        .addEdge("billing", END)
//        .addEdge("technical", END)
//        .addEdge("general", END);
//
//    // 可视化
//    GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML,
//        "Routing  flow");
//    System.out.println("\n=== Routing  UML Flow ===");
//    System.out.println(representation.content());
//    System.out.println("==================================\n");
//
//    return graph;
//  }
//
//  //选择的专门任务处理节点
//  static class SelectionLlmNode implements NodeAction {
//
//    private final ChatClient client;
//
//    public SelectionLlmNode(ChatClient client) {
//      this.client = client;
//    }
//
//    @Override
//    public Map<String, Object> apply(OverAllState state) {
//      String inputText = (String) state.value("inputText").orElse("");
//      String selectionLlm = (String) state.value("selectionLlm").orElse("");
//      String result = client.prompt().system(availableRoutes.get(selectionLlm).toString())
//          .user(inputText).stream().content()
//          .collect(Collectors.joining()).block();
//      return Map.of("result", result);
//    }
//  }
//
//  //大模型问题分类路由节点
//  static class LlmCallRouterNode implements NodeAction {
//
//    private static final Logger log = LoggerFactory.getLogger(LlmCallRouterNode.class);
//    private static final ObjectMapper MAPPER = new ObjectMapper();
//
//    private final ChatClient client;
//    private final String inputTextKey;
//
//    public LlmCallRouterNode(ChatClient client, String inputTextKey) {
//      this.client = client;
//      this.inputTextKey = inputTextKey;
//    }
//
//    @Override
//    public Map<String, Object> apply(OverAllState state) {
//      String inputText = (String) state.value(inputTextKey).orElse("");
//      String selectorPrompt = String.format("""
//          You are a routing agent. Analyze the user input and select the most appropriate
//          support team from: %s.
//
//          Respond with ONLY a single JSON object (no markdown, no extra text):
//          {"selection": "<team-name>"}
//
//          Input: %s""", availableRoutes.keySet(), inputText);
//
//      // ModelScope API only supports streaming; use stream() and collect
//      String raw = client.prompt(selectorPrompt)
//          .stream()
//          .content()
//          .collect(Collectors.joining())
//          .block();
//      log.info("LLM routing raw response: {}", raw);
//
//      if (raw == null || raw.isBlank()) {
//        throw new RuntimeException("LLM returned empty response");
//      }
//
//      // strip markdown code fence if present
//      String json = raw.trim()
//          .replaceAll("(?s)^```json\\s*", "")
//          .replaceAll("(?s)^```\\s*", "")
//          .replaceAll("(?s)\\s*```$", "")
//          .trim();
//
//      try {
//        LlmRoutingResponse r = MAPPER.readValue(json, LlmRoutingResponse.class);
//        if (r.selection == null || r.selection.isBlank()) {
//          throw new RuntimeException("selection field missing in: " + json);
//        }
//        return Map.of("selectionLlm", r.selection);
//      } catch (Exception e) {
//        throw new RuntimeException("Failed to parse LLM routing response: " + json, e);
//      }
//    }
//  }
//
//  public record LlmRoutingResponse(String reasoning, String selection) {
//
//  }
//
//}
