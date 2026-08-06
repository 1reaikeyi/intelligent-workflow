<div align="center">
  <h1>Intelligent-workflow - 智能 ，工作流</h1>
    <h2>基于 Spring Boot 3 + Spring AI + Graph的多功能 AI 助手后端服务平台，集成大语言模型对话、智能体路由、RAG 检索增强、工具调用、图像识别等多种 AI 能力。<h2>
    <h1>配置要求</h1>
    <img src="https://img.shields.io/badge/Java-17+ -6DB33F?style=flat-square&logo=java&logoColor=white" alt="Java" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.+ -6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/MySQL-8.0+ -6DB33F?style=flat-square&logo=mysql&logoColor=white" alt="mysql" />
    <img src="https://img.shields.io/badge/Redis-7.0+ -6DB33F?style=flat-square&logo=redis&logoColor=white" alt="redis" />
    <img src="https://img.shields.io/badge/Spring%20AI-1.1.+ -6DB33F?style=flat-square&logo=spring&logoColor=white" alt="spring ai" />
    <img src="https://img.shields.io/badge/Vue-Node.js20.19.+ -6DB33F?style=flat-square&logo=vuedotjs&logoColor=white" alt="vue" />
  </p>
</div>

------

# **启动步骤**

1. 创建数据库并导入 `sql/` 目录脚本。

2. 修改 `start/src/main/resources/application-dev.yml` 中数据库与 Redis 配置。

3. `npm run dev ` 前端启动服务。

![封面](D:说明/原型设计/2.png)

# 项目结构

```
intelligent-workflow/
├── backend-spring-ai/                    
│   ├── rag/                              # 智能体路由 & RAG 检索模块
│   ├── see/                              # 视觉识别模块
│   └── yu/                               # 语音合成模块
├── frontend-vue-ai/                  # 前端代码（Vue 3）
├── database-sql/                     # 数据库脚本目录
│   ├── sql.txt                       # 数据库初始化SQL
│   └── 数据库设计文档.md               # 完整的数据库设计说明
└── 说明
```

# 功能架构总览

| 模块    | 核心功能                                                     | 技术要点                            |
| :------ | :----------------------------------------------------------- | :---------------------------------- |
| **rag** | Agent 智能体路由、RAG 检索、工具调用、会话管理、流式输出中断 | spring-ai-starter-openai            |
| **see** | 敏感词过滤→图像识别→工具调用联动                             | spring-ai-starter-openai            |
| **yu**  | 英语学习工作流（造句→翻译→语音）、ASR/TTS                    | spring-ai-alibaba-starter-dashscope |

---

# 前端功能演示

| 多模态 | <img src="D:说明/原型设计/1.png" alt="封面" style="zoom:50%;" /> |
| ------ | ------------------------------------------------------------ |
|        |                                                              |

# 后端介绍

## 一、智能体路由模块（rag-chat）

### 需求阶段

需求背景：企业和个人对 AI 助手的需求日益增长，单一对话模型难以满足多样化业务场景——如结合私有知识库回答专业问题、通过工具调用查询业务数据等。

- 传统 AI 应用缺乏灵活性和可扩展性
- 难以快速集成多种 AI 能力并实现智能化任务路由
- 多轮对话需要会话记忆管理

### **策略流程图**

<img src="D:\a.github\intelligent-workflow\说明\graph流程图\路由.jpg" alt="流程" style="zoom:50%;" />

```java
				用户输入 → AgentController → AgentServiceImpl.chat()
  	  						        │
   							        ▼
				RouteAgent.process() 意图识别（独立ChatClient，无记忆干扰）
   						 			│
  						 			▼
				AgentTypeEnum.agentNameOf() 解析意图标识
  				  					│
   					 				▼
				SpringUtil.getBeansOfType(Agent.class) 动态查找匹配智能体
    								│
    								▼
   					 			 ROUTE → 路由失败，返回提示
    │										 │                 					 │
    ▼                                        ▼                 	    			 ▼
  KNOWLEDGE → KnowledgeAgent知识问答    RECOMMEND → RecommendAgent课程推荐     其他 → 动态路由到对应智能体
    │   									│ ├─ VectorStore RAG检索
    └─ chatClient→ 直接对话		             └─ CourseTools @Tool注解工具调用
    
```

### 编码阶段

```java
// Agent.java - 智能体统一接口定义
public interface Agent {
    Object[] EMPTY_OBJECTS = new Object[0];

    // 处理流式请求（如流式回答）
    Flux<ChatEventVO> processStream(String question, String sessionId);
    
    // 处理标准请求（非流式）
    String process(String question, String sessionId);
    
    // 获取智能体类型标识
    AgentTypeEnum getAgentType();
    
    // 停止指定会话的处理
    void stop(String sessionId);

    // 默认方法：系统提示、工具列表、Advisor列表等
    default String systemMessage() { return ""; }
    default Object[] tools() { return EMPTY_OBJECTS; }
    default List<Advisor> advisors() { return List.of(); }
    default Map<String, Object> toolContext(String sessionId, String requestId) { return Map.of(); }
    default Map<String, Object> advisorParams(String sessionId, String requestId) { return Map.of(); }
    default Map<String, Object> systemMessageParams() { return Map.of(); }
}
```

```java
// AbstractAgent.java - 抽象基类，实现流式处理核心逻辑
@Slf4j
@Component
public abstract class AbstractAgent implements Agent {

    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(ChatEventTypeEnum.STOP.getValue()).build();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ChatMemory chatMemory;

    private static final String GENERATE_STATUS_KEY = "GENERATE_STATUS";

    // 子类必须实现：返回自己的 ChatClient
    protected abstract ChatClient getChatClient();

    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        var requestId = this.generateRequestId();
        var hashOps = this.stringRedisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        var conversationId = ChatService.getConversationId(sessionId);
        var outputBuilder = new StringBuilder();

        return this.getChatClientRequest(question, sessionId, requestId)
                .stream()
                .chatResponse()
                .doFirst(() -> hashOps.put(sessionId, "true"))
                .doOnError(throwable -> hashOps.delete(sessionId))
                .doOnComplete(() -> hashOps.delete(sessionId))
                .doOnCancel(() -> saveStopHistoryRecord(conversationId, outputBuilder.toString()))
                .takeWhile(response -> hashOps.get(sessionId) != null)
                .map(chatResponse -> {
                    var text = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(text);
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    var result = ToolResultHolder.get(requestId);
                    if (ObjectUtil.isNotEmpty(result)) {
                        ToolResultHolder.remove(requestId);
                        return Flux.just(ChatEventVO.builder()
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .eventData(result)
                                .build(), STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
    }

    @Override
    public void stop(String sessionId) {
        var hashOps = this.stringRedisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        hashOps.delete(sessionId);
    }
}
```

```java
// RouteAgent.java - 路由智能体实现
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;

    // 使用专门的路由 ChatClient，避免记忆功能干扰
    @Resource(name = "routeChatClient")
    private final ChatClient routeChatClient;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.ROUTE;
    }

    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getRouteAgentSystemMessage().get();
    }

    @Override
    public List<Advisor> advisors() {
        // 路由智能体不需要记忆功能，返回空列表
        return List.of();
    }

    @Override
    public Object[] tools() {
        // 路由智能体不需要工具，返回空数组
        return EMPTY_OBJECTS;
    }

    @Override
    protected ChatClient getChatClient() {
        return routeChatClient;
    }
}
```

```java
// RecommendAgent.java - 推荐智能体（带RAG+工具调用）
@Component
@RequiredArgsConstructor
public class RecommendAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;
    private final VectorStore vectorStore;
    private final CourseTools courseTools;

    // 注入默认的 ChatClient（带记忆功能）
    @Qualifier("chatClient")
    private final ChatClient chatClient;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.RECOMMEND;
    }

    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getRecommendAgentSystemMessage().get();
    }

    @Override
    public Object[] tools() {
        return new Object[]{this.courseTools};
    }

    @Override
    public List<Advisor> advisors() {
        // 定义RAG增强：相似度阈值0.6，TopK=6
        var qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.6d)
                        .topK(6)
                        .build())
                .build();
        return List.of(qaAdvisor);
    }

    @Override
    public Map<String, Object> toolContext(String sessionId, String requestId) {
        return Map.of(Constant.REQUEST_ID, requestId);
    }

    @Override
    protected ChatClient getChatClient() {
        return chatClient;
    }
}
```

```java
// AgentServiceImpl.java - 智能体路由调度
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        // 先通过路由智能体，分析用户的意图
        var result = this.findAgentByType(AgentTypeEnum.ROUTE).process(question, sessionId);

        // 处理路由智能体返回的结果
        if (result == null || result.trim().isEmpty()) {
            return Flux.just(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData("抱歉，我无法识别您的意图，请重新提问。")
                    .build(), AbstractAgent.STOP_EVENT);
        }

        // 清理返回结果，提取有效的意图标识
        var cleanedResult = result.trim().toUpperCase();
        var agentTypeEnum = AgentTypeEnum.agentNameOf(cleanedResult);

        var agent = this.findAgentByType(agentTypeEnum);
        if (agent == null) {
            return Flux.just(ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(result)
                    .build(), AbstractAgent.STOP_EVENT);
        }
        // 执行智能体的逻辑
        return agent.processStream(question, sessionId);
    }

    // 根据代理类型查找对应的Agent实例
    private Agent findAgentByType(AgentTypeEnum agentTypeEnum) {
        if (agentTypeEnum == null) return null;
        var beans = SpringUtil.getBeansOfType(Agent.class);
        for (var agent : beans.values()) {
            if (agentTypeEnum == agent.getAgentType()) {
                return agent;
            }
        }
        return null;
    }

    // 停止生成
    @Override
    public void stop(String sessionId) {
        this.findAgentByType(AgentTypeEnum.ROUTE).stop(sessionId);
    }
}
```

```java
// CourseTools.java - 工具调用实现
@Service
public class CourseTools {
    private static final String FIELD_NAME_FORMAT = "{}_{}";
    @Autowired
    private CourseService courseService;

    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById(
            @ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId,
            ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> courseService.getById(id))
                .map(CourseInfo::of)
                .map(courseInfo -> {
                    // 动态生成字段名：courseInfo_课程ID
                    String className = CourseInfo.class.getSimpleName();
                    String lowerClassName = Character.toLowerCase(className.charAt(0)) 
                            + className.substring(1);
                    String field = String.format(FIELD_NAME_FORMAT, lowerClassName, courseInfo.getId());
                    // 存储工具调用结果，关联requestId
                    Object requestIdObj = toolContext.getContext().get(Constant.REQUEST_ID);
                    String requestId = requestIdObj != null ? String.valueOf(requestIdObj) : null;
                    ToolResultHolder.put(requestId, field, courseInfo);
                    return courseInfo;
                })
                .orElse(null);
    }
}
```

### 问题修复阶段

Q：为什么采用路由-执行模式（Routing-Execution Pattern）？

> A：路由-执行模式将意图识别和业务处理解耦。RouteAgent 作为入口网关分析用户意图，然后根据意图动态路由到专业智能体处理。这种架构支持灵活扩展，新增业务场景只需添加新的智能体实现。

Q：为什么路由智能体使用独立的 ChatClient？

> A：路由决策不需要会话记忆，独立的 ChatClient 可以避免记忆功能干扰路由判断，确保每次路由决策都是基于当前输入的纯粹分析。

Q：路由智能体返回结果包含多余字符，导致意图解析失败

> 修复方案：在 `AgentServiceImpl` 中添加结果清理逻辑，去除空白字符并转为大写
> ```java
> var cleanedResult = result.trim().toUpperCase();
> var agentTypeEnum = AgentTypeEnum.agentNameOf(cleanedResult);
> ```

Q：流式输出如何支持中断（停止生成）？

> 实现方案：使用 Redis Hash 存储生成状态标识，`takeWhile` 操作符判断是否继续生成
> ```java
> // 开始生成时设置标识
> .doFirst(() -> hashOps.put(sessionId, "true"))
> // 取消时删除标识，并保存已生成内容
> .doOnCancel(() -> saveStopHistoryRecord(conversationId, outputBuilder.toString()))
> // 判断是否继续生成
> .takeWhile(response -> hashOps.get(sessionId) != null)
> ```

Q：工具调用结果如何与请求关联并返回给前端？

> 实现方案：通过 `ToolContext` 传递 `requestId`，使用 `ToolResultHolder` 存储工具调用结果，在流结束时通过 `CHAT_EVENT_TYPE.PARAM` 事件类型返回
> ```java
> // 工具调用时存储结果
> ToolResultHolder.put(requestId, field, courseInfo);
> // 流结束时检查并返回工具结果
> var result = ToolResultHolder.get(requestId);
> if (ObjectUtil.isNotEmpty(result)) {
>     return Flux.just(ChatEventVO.builder()
>             .eventType(ChatEventTypeEnum.PARAM.getValue())
>             .eventData(result)
>             .build(), STOP_EVENT);
> }
> ```

---

## 二、视觉识别模块（see）

### 需求阶段

需求背景：实现图像识别功能，支持图片内容识别和工具调用联动。

- 图片上传需要尺寸校验和安全过滤
- 需要支持本地文件和上传文件两种输入方式
- 识别结果需要与工具调用联动

### **策略流程图**

每个智能体之间也是：平行

<img src="D:\a.github\intelligent-workflow\说明\graph流程图\链式.jpg" alt="流程" style="zoom:25%;" />

<img src="D:\a.github\intelligent-workflow\说明\graph流程图\平行.jpg" alt="流程" style="zoom:25%;" />

```java
用户上传图片 → ImgComperhendController
    │
    ├─ 尺寸校验（最大 2048×2048）
    ├─ SensitiveWordInterceptor 敏感词检测
    │
    ▼
VisionService 视觉识别服务
    │
    ├─ 本地文件：ImageIO.read → Base64 编码
    ├─ 上传文件：byte[] → Base64 编码
    │
    ▼
Media(IMAGE_JPEG, "data:image/jpeg;base64,...")
    │
    ▼
visualChatClient.prompt().user(media).call().content()
    │
    ▼
NodeLink 状态图编排
    │
    ├─ node1: VisualNode（视觉识别节点）→ 识别食物饮料
    └─ node2: ToolNode（工具调用节点）→ 查询套餐信息
```

### 编码阶段

```java
// VisionService.java - 视觉识别服务
@Service
public class VisionService {
    @Autowired
    private ChatClient visualChatClient;

    // 本地文件识图
    public String visionByFile(File file, String prompt) throws Exception {
        String base64 = toBase64(file);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, 
                URI.create("data:image/jpeg;base64," + base64));
        return visualChatClient.prompt()
                .user(userMessage -> userMessage.text(prompt).media(media))
                .call()
                .content();
    }

    // 上传文件byte识图（接口常用）
    public String visionByBytes(byte[] imgBytes, String prompt) {
        String base64 = Base64.getEncoder().encodeToString(imgBytes);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, 
                URI.create("data:image/jpeg;base64," + base64));
        return visualChatClient.prompt()
                .user(userMessage -> userMessage.text(prompt).media(media))
                .call()
                .content();
    }

    // 文件图片转 base64
    private String toBase64(File imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", bos);
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
}
```

```java
// VisualNode.java - 视觉识别节点
@Service
public class VisualNode implements NodeAction {
    @Resource(name = "visualChatClient")
    private ChatClient visualClient;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String base64 = (String) state.value("file").orElse("文件为空");
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, 
                URI.create("data:image/jpeg;base64," + base64));
        String result = result(media);
        return Map.of("visualResult", result != null ? result : "没有识别到内容");
    }
    
    public String result(Media media) {
        return visualClient.prompt()
                .user(promptUserSpec -> promptUserSpec
                        .text("识别有哪些食物,饮料？").media(media))
                .call()
                .content();
    }
}
```

```java
// NodeLink.java - 视觉识别状态图编排
@Configuration
public class NodeLink {
    @Autowired
    private VisualNode visualNode;
    @Autowired
    private ToolNode toolNode;

    @Bean
    public CompiledGraph comprehend() {
        // 定义状态传递策略
        KeyStrategyFactory strategyFactory = () -> Map.of(
                "visualResult", new ReplaceStrategy(),
                "toolResult", new ReplaceStrategy()
        );
        
        StateGraph graph = new StateGraph("img-comprehend", strategyFactory);
        
        // 添加异步节点：视觉识别 → 工具查询
        try {
            graph.addNode("node1", AsyncNodeAction.node_async(visualNode));
            graph.addNode("node2", AsyncNodeAction.node_async(toolNode));
            // 添加边：定义节点执行顺序
            graph.addEdge(StateGraph.START, "node1");
            graph.addEdge("node1", "node2");
            graph.addEdge("node2", StateGraph.END);
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        
        // 编译状态图
        try {
            CompiledGraph compiledGraph = graph.compile();
            // 生成 PlantUML 格式的可视化表示
            GraphRepresentation representation = graph.getGraph(
                    GraphRepresentation.Type.PLANTUML, "Image Comprehend Flow");
            System.out.println("=== Image Comprehend Flow UML Diagram ===");
            System.out.println(representation.content());
            return compiledGraph;
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
    }
}
```

```java
// SensitiveWordInterceptor.java - 敏感词拦截器
@Component
@Slf4j
public class SensitiveWordInterceptor implements HandlerInterceptor {
    @Autowired
    private SensitiveWordBs sensitiveWordBs;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
            HttpServletResponse response, Object handler) throws Exception {
        String message = request.getParameter("message");
        if (message != null && !message.isEmpty()) {
            boolean containsSensitiveWord = sensitiveWordBs.contains(message);
            if (containsSensitiveWord) {
                log.info("检测到敏感词==>请求路径: {}, 参数内容: {}", 
                        request.getRequestURI(), message);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("请求包含敏感词，请修改后重试");
                return false;
            }
        }
        return true;
    }
}
```

### 问题修复阶段

Q：为什么将图片转为 Base64 而不是直接传递文件路径？

> A：多模态模型需要将图片数据嵌入请求中，Base64 编码是标准的嵌入方式。同时，Base64 编码便于在节点间传递，无需处理文件路径问题。

Q：为什么需要敏感词拦截器？

> A：图像识别可能返回包含敏感内容的结果，敏感词拦截器可以在识别后进行过滤，确保返回内容符合安全规范。

Q：图片尺寸过大导致识别失败

> 修复方案：在控制器中添加图片尺寸校验，限制最大尺寸为 2048×2048

Q：视觉识别结果如何与工具调用联动？

> 实现方案：使用 DashScope Graph 状态图，VisualNode 识别结果存入 `visualResult`，ToolNode 基于识别结果查询套餐信息
> ```java
> // 状态传递策略：ReplaceStrategy 新值覆盖旧值
> KeyStrategyFactory strategyFactory = () -> Map.of(
>         "visualResult", new ReplaceStrategy(),
>         "toolResult", new ReplaceStrategy()
> );
> ```

---

## 三、语音工作流模块（speech）

<img src="D:\a.github\intelligent-workflow\说明\graph流程图\链式.jpg" alt="流程" style="zoom:25%;" />

每个智能体之间也是：平行

<img src="D:\a.github\intelligent-workflow\说明\graph流程图\平行.jpg" alt="流程" style="zoom:25%;" />

### 需求阶段

需求背景：实现英语学习辅助功能，支持单词造句、翻译和语音合成的完整工作流。

- 学习流程需要多步骤编排（造句→翻译→语音）
- 各步骤之间需要状态传递
- 需要支持工作流的可视化和可维护性

### **策略流程图**

```java
用户输入单词 → EnlishController → NodeLink.toEnlish()
    │
    ▼
StateGraph 状态图编排（DashScope Graph）
    │
    ├─ node1: Sentence 节点（异步）
    │   └─ ChatClient.prompt().user(word).call().content()
    │   └─ 输出 sentence（英文例句）
    │
    ├─ node2: Translation 节点（异步）
    │   └─ ChatClient.prompt().user(sentence).call().content()
    │   └─ 输出 translation（中文翻译）
    │
    └─ node3: Read 节点（异步）
        └─ OpenAiAudioSpeechModel.call(TextToSpeechPrompt)
        └─ 输出 read（MP3音频文件路径）
    │
    ▼
返回 {sentence, translation, audioPath}
```

### 编码阶段

```java
// Sentence.java - 造句节点
@Component
public class Sentence implements NodeAction {
    @Resource
    private ChatClient chatClient;
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String word = state.value("word", "");
        PromptTemplate promptTemplate = new PromptTemplate(
                "你负责英语造句，根据给定的单词{word}造句，返回一个英文句子");
        promptTemplate.add("word", word);
        String result = chat(promptTemplate.render());
        return Map.of("sentence", result != null ? result : "null");
    }
    
    public String chat(String word) {
        return chatClient.prompt()
                .user(word)
                .call()
                .content();
    }
}
```

```java
// Translation.java - 翻译节点
@Component
public class Translation implements NodeAction {
    @Resource
    private ChatClient chatClient;
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String sentence = state.value("sentence", "");
        PromptTemplate promptTemplate = new PromptTemplate(
                "你是负责英语翻译，根据给定的{sentence}翻译成中文");
        promptTemplate.add("sentence", sentence);
        String result = chat(promptTemplate.render());
        return Map.of("translation", result != null ? result : "null");
    }
}
```

```java
// Read.java - 语音合成节点
@Component
public class Read implements NodeAction {
    @Autowired
    private OpenAiAudioSpeechModel openAiAudioSpeechModel;
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String text = state.value("sentence", "") + state.value("translation", "");
        byte[] audio = chat(text);
        
        // 保存音频文件到本地
        String fileName = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".mp3";
        File dir = new File("mp3");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File path = new File(dir.getAbsolutePath(), fileName);
        try (OutputStream outputStream = new FileOutputStream(path)) {
            outputStream.write(audio);
            outputStream.flush();
        }
        return Map.of("read", path.getAbsolutePath());
    }
    
    public byte[] chat(String text) {
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
        TextToSpeechResponse response = openAiAudioSpeechModel.call(prompt);
        return response.getResult().getOutput();
    }
}
```

```java
// NodeLink.java - 状态图编译与节点编排
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
        // 定义状态传递策略：ReplaceStrategy 表示新值直接覆盖旧值
        KeyStrategyFactory strategyFactory = () -> Map.of(
                "word", new ReplaceStrategy(),
                "sentence", new ReplaceStrategy(),
                "translation", new ReplaceStrategy()
        );
        
        StateGraph graph = new StateGraph("Enlish", strategyFactory);
        
        // 添加异步节点：造句 → 翻译 → 语音合成
        try {
            graph.addNode("node1", AsyncNodeAction.node_async(sentenceAction));
            graph.addNode("node2", AsyncNodeAction.node_async(translationAction));
            graph.addNode("node3", AsyncNodeAction.node_async(readAction));
            // 添加边：定义节点执行顺序
            graph.addEdge(StateGraph.START, "node1");
            graph.addEdge("node1", "node2");
            graph.addEdge("node2", "node3");
            graph.addEdge("node3", StateGraph.END);
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        
        // 编译状态图
        CompiledGraph compiledGraph = graph.compile();
        // 生成 PlantUML 格式的图可视化表示
        GraphRepresentation representation = graph.getGraph(
                GraphRepresentation.Type.PLANTUML, "English Flow");
        System.out.println("=== English Flow UML Diagram ===");
        System.out.println(representation.content());
        
        return compiledGraph;
    }
}
```

### 问题修复阶段

Q：为什么用 DashScope Graph 而不是传统的链式调用？

> A：Graph 状态图引擎提供了可视化的工作流编排能力，支持节点化设计、状态传递和流程控制。相比链式调用，Graph 更灵活、更易于维护和扩展。

Q：为什么采用异步节点设计？

> A：异步节点可以并行执行多个任务，提高工作流的执行效率。同时，异步设计可以避免单个节点的阻塞影响整个流程。

Q：节点间状态传递如何实现？

> 实现方案：使用 `OverAllState` 的 `Map<String, Object>` 格式传递状态，配合 `ReplaceStrategy` 新值覆盖旧值
> ```java
> KeyStrategyFactory strategyFactory = () -> Map.of(
>         "word", new ReplaceStrategy(),
>         "sentence", new ReplaceStrategy(),
>         "translation", new ReplaceStrategy()
> );
> ```

---

## 四、语音合成模块

### 需求阶段

需求背景：实现语音识别（ASR）和语音合成（TTS）功能，支持中英文语音交互。

- 语音文件格式多样，需要统一处理
- 大文件上传需要流式处理
- 音频转写结果需要准确可靠

### **策略流程图**

```java
语音识别（ASR）：
上传音频(MultipartFile) → 提取音频格式 → Base64编码 → 
DashScopeAudioTranscriptionModel(paraformer-v2) → 
DashScopeAudioTranscriptionPrompt → AudioTranscriptionResponse → 返回文本结果

语音合成（TTS）：
输入文本 → DashScopeAudioSpeechModel.stream(TextToSpeechPrompt) → 
Flux<byte[]> → collectList()合并 → ByteArrayOutputStream → 保存为 MP3 文件
```

### 编码阶段

```java
// ASRServiceImpl.java - 语音识别服务
@Service
@Slf4j
public class ASRServiceImpl implements ASRService {
    @Autowired
    private DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel;
    
    @Override
    public Object stt(MultipartFile file) throws Exception {
        // 从文件名中提取音频格式
        String originalFilename = file.getOriginalFilename();
        String audioFormat = "mp3"; // 默认格式
        if (originalFilename != null && originalFilename.contains(".")) {
            audioFormat = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        
        // 将音频文件转换为 Base64
        String base64Audio = Base64.getEncoder().encodeToString(file.getBytes());
        
        // 构造 ASR 请求：使用 paraformer-v2 模型
        DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
                .model("paraformer-v2")
                .build();
        
        // 构造 InputAudio 和 Content 对象
        DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.InputAudio inputAudio = 
                new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.InputAudio(
                        base64Audio, audioFormat);
        DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.Content content = 
                new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.Content(
                        "input_audio", inputAudio);
        DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage message = 
                new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage(List.of(content));
        
        DashScopeAudioTranscriptionPrompt prompt = 
                new DashScopeAudioTranscriptionPrompt(options, message);
        AudioTranscriptionResponse response = dashScopeAudioTranscriptionModel.call(prompt);
        
        return response.getResult().getOutput();
    }
}
```

```java
// TTSServiceImpl.java - 语音合成服务
@Service
@Slf4j
public class TTSServiceImpl implements TTSService {
    @Autowired
    private DashScopeAudioSpeechModel dashScopeAudioSpeechModel;
    
    @Override
    public byte[] TTS(String text) {
        // 流式合成：调用 stream() 逐块获取音频数据后合并
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
        Flux<byte[]> audioFlux = dashScopeAudioSpeechModel.stream(prompt)
                .map(res -> res.getResult().getOutput());
        
        byte[] audio = audioFlux.collectList()
                .map(chunks -> {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    chunks.forEach(baos::writeBytes);
                    return baos.toByteArray();
                }).block();
        
        // 保存音频文件到本地
        String fileName = System.currentTimeMillis() + ".mp3";
        File path = new File("mp3");
        if (!path.exists()) {
            path.mkdirs();
        }
        try (OutputStream outputStream = new FileOutputStream(new File(path, fileName))) {
            outputStream.write(audio);
            log.info("音频文件保存成功：{}", fileName);
        } catch (IOException e) {
            log.error("保存音频文件失败：{}", e.getMessage(), e);
        }
        return audio;
    }
}
```

### 问题修复阶段

Q：为什么使用 Base64 编码方式上传音频？

> A：Base64 编码可以将二进制音频数据转为文本格式，便于在 HTTP 请求中传输，无需处理文件上传的复杂逻辑。

Q：为什么 TTS 使用流式合成？

> A：流式合成可以逐块获取音频数据，减少内存占用，同时提高响应速度。

Q：音频格式硬编码问题

> 修复方案：从文件名中自动提取音频格式，支持 mp3、wav、m4a 等多种格式
> ```java
> String originalFilename = file.getOriginalFilename();
> String audioFormat = "mp3"; // 默认格式
> if (originalFilename != null && originalFilename.contains(".")) {
>     audioFormat = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
> }
> ```

Q：大文件合成内存溢出

> 修复方案：使用流式合成（`stream()`），逐块获取音频数据并合并，减少内存占用

---

# 核心组件设计

## 1. 会话记忆管理（ChatMemory）

Q：为什么提供两种记忆存储方式（MySQL 和 Redis）？

> A：MySQL 适合持久化存储，支持会话的长期保存和复杂查询；Redis 适合缓存存储，读写速度快，适合高并发场景。通过 `@Profile` 注解实现运行时切换。

Q：为什么保存时先删除再批量保存？

> A：Spring AI 的 `saveAll` 方法会传入全部消息数据，包括之前的历史记录。先删除再保存可以确保数据一致性，避免重复存储。

### 编码阶段

```java
// MysqlChatMemoryReposity.java - MySQL 持久化实现
@Service
@Profile("mysql")
public class MysqlChatMemoryReposity implements ChatMemoryRepository {
    @Autowired
    private ChatRecordService chatRecordService;
    
    @Override
    public List<Message> findByConversationId(String conversationId) {
        var chatRecordList = chatRecordService.lambdaQuery()
                .eq(ChatRecord::getSessionId, conversationId)
                .orderByAsc(ChatRecord::getCreateTime)
                .list();
        return CollStreamUtil.toList(chatRecordList, 
                chatRecord -> MessageUtil.toMessage(chatRecord.getData()));
    }
    
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        this.deleteByConversationId(conversationId);
        var chatRecordList = CollStreamUtil.toList(messages, message -> ChatRecord.builder()
                .data(MessageUtil.toJson(message, conversationId))
                .sessionId(conversationId)
                .build());
        this.chatRecordService.saveBatch(chatRecordList);
    }
}
```

```java
// RedisChatMemoryReposity.java - Redis 缓存实现
@Service
@Profile("redis")
public class RedisChatMemoryReposity implements ChatMemoryRepository {
    private static final String DEFAULT_PREFIX = "chat:";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Override
    public List<Message> findByConversationId(String conversationId) {
        var redisKey = this.getKey(conversationId);
        var listOps = this.stringRedisTemplate.boundListOps(redisKey);
        var messages = listOps.range(0, -1);
        return CollStreamUtil.toList(messages, MessageUtil::toMessage);
    }
    
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        this.deleteByConversationId(conversationId);
        var redisKey = this.getKey(conversationId);
        var listOps = this.stringRedisTemplate.boundListOps(redisKey);
        messages.forEach(message -> listOps.rightPush(MessageUtil.toJson(message, conversationId)));
    }
}
```

## 2. RAG 检索增强（QuestionAnswerAdvisor）

Q：为什么设置相似度阈值为 0.6？

> A：相似度阈值控制检索结果的准确性。0.6 是一个经验值，可以过滤掉不相关的文档，同时保留足够的上下文信息。

Q：为什么设置 Top-K 为 6？

> A：Top-K 控制返回的文档数量。6 个文档可以提供足够的上下文信息，同时避免过多文档导致 Token 超限。

### 编码阶段

```java
// RecommendAgent.java - RAG 配置（位于 advisors() 方法）
@Override
public List<Advisor> advisors() {
    var qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
            .searchRequest(SearchRequest.builder()
                    .similarityThreshold(0.6d) // 相似度阈值
                    .topK(6) // 搜索的条数
                    .build())
            .build();
    return List.of(qaAdvisor);
}
```

## 3. Agent 接口层次结构

Q：为什么使用接口+抽象类+实现类的三层结构？

> A：接口 `Agent` 定义智能体的核心能力契约；抽象类 `AbstractAgent` 实现通用逻辑（流式处理、请求ID生成、工具上下文管理、中断支持）；具体实现类（`RouteAgent`、`KnowledgeAgent`、`RecommendAgent`）专注于各自的业务逻辑。这种结构实现了代码复用和职责分离。

Q：如何支持流式输出中断（停止生成）？

> A：`AbstractAgent` 使用 Redis Hash 存储生成状态，`takeWhile` 操作符判断是否继续生成。`doOnCancel` 保存已生成内容到历史记录。

### 接口层次结构

```
Agent（接口）→ AbstractAgent（抽象类）→ RouteAgent/KnowledgeAgent/RecommendAgent（实现类）
    │                                      ↑
    ├── processStream() 流式处理           │  getChatClient() 子类实现
    ├── process() 非流式处理               │  systemMessage() 系统提示
    ├── stop() 停止生成                    │  tools() 工具列表
    ├── systemMessageParams()              │  advisors() Advisor列表
    ├── advisorParams()                    │  toolContext() 工具上下文
    └── 默认方法扩展点                     └── 模板方法模式
```

## 4. 工具结果保持器（ToolResultHolder）

Q：为什么需要 ToolResultHolder？

> A：工具调用的结果需要与具体请求关联，以便后续使用。`ToolResultHolder` 使用 `ConcurrentHashMap` 存储工具调用结果，以 `requestId` 为键，支持多线程并发访问。

Q：为什么使用嵌套 Map 结构？

> A：外层 Map 以 `requestId` 为键，内层 Map 以字段名（如 `courseInfo_123`）为键。这种结构支持一个请求调用多个工具，每个工具结果存储在不同的字段中。

### 编码阶段

```java
// ToolResultHolder.java - 工具结果保持器
public class ToolResultHolder {
    // ConcurrentHashMap 保证线程安全
    private static final Map<String, Map<String, Object>> HANDLER_MAP = new ConcurrentHashMap<>();
    
    private ToolResultHolder() {
        // 工具类，禁止实例化
    }
    
    // 存储工具调用结果
    public static void put(String key, String field, Object result) {
        Assert.notNull(key, "key is not null!");
        Assert.notNull(field, "field is not null!");
        HANDLER_MAP.computeIfAbsent(key, k -> new HashMap<>()).put(field, result);
    }
    
    // 获取指定请求的所有工具结果
    public static Map<String, Object> get(String key) {
        return key == null ? null : HANDLER_MAP.get(key);
    }
    
    // 获取指定请求的指定字段结果
    public static Object get(String key, String field) {
        Assert.notNull(key, "key is not null!");
        Assert.notNull(field, "field is not null!");
        return Optional.ofNullable(HANDLER_MAP.get(key))
                .map(map -> map.get(field))
                .orElse(null);
    }
    
    // 清理指定请求的工具结果
    public static void remove(String key) {
        Assert.notNull(key, "key is not null!");
        HANDLER_MAP.remove(key);
    }
}
```

