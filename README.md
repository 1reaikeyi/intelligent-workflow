# Intelligent-workflow - 智能 AI

基于 Spring Boot 3 + Spring AI 的多功能 AI 助手后端服务平台，集成大语言模型对话、智能体路由、RAG 检索增强、工具调用、图像识别、工作流引擎等多种 AI 能力。

------

# 后端介绍

可以上传图片+输入问题，多模态

![封面](D:说明\原型设计\2.png)

# 项目结构

```
intelligent-workflow/
├── backend-spring-ai/                # 后端maven
└── frontend-vue-ai/                  # 前端代码（Vue 3）
├── database-sql/                     # 数据库脚本目录
│   ├── sql.txt                       # 数据库初始化SQL
│   └── 数据库设计文档.md              # 完整的数据库设计说明
└── 说明
```

# 环境要求

- JDK 17+
- Spring Boot 3.3.8+
- Spring AI 1.1.0+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

---

## 一、智能体路由模块（rag-chat）

### 需求阶段

**需求背景**：企业和个人对 AI 助手的需求日益增长，单一对话模型难以满足多样化业务场景——如结合私有知识库回答专业问题、通过工具调用查询业务数据等。

**痛点**：
- 传统 AI 应用缺乏灵活性和可扩展性
- 难以快速集成多种 AI 能力并实现智能化任务路由
- 多轮对话需要会话记忆管理

### 设计阶段

**设计思路**：

Q：为什么采用路由-执行模式（Routing-Execution Pattern）？
> A：路由-执行模式将意图识别和业务处理解耦。RouteAgent 作为入口网关分析用户意图，然后根据意图动态路由到专业智能体处理。这种架构支持灵活扩展，新增业务场景只需添加新的智能体实现。

Q：为什么路由智能体使用独立的 ChatClient？
> A：路由决策不需要会话记忆，独立的 ChatClient 可以避免记忆功能干扰路由判断，确保每次路由决策都是基于当前输入的纯粹分析。

**架构设计**：
```
用户输入 → [AgentController] → [AgentServiceImpl]
                                    │
                                    ▼
                           [RouteAgent] 意图识别
                                    │
                                    ▼
                    ┌─────────────┴─────────────┐
                    │  SpringUtil.getBeansOfType │
                    │   动态查找匹配智能体        │
                    └─────────────┬─────────────┘
                                    │
           ┌───────────────────────┼───────────────────────┐
           ▼                       ▼                       ▼
    [KnowledgeAgent]        [RecommendAgent]        [其他智能体]
    (RAG 知识问答)           (课程推荐工具调用)        (CONSULT/BUY...)
           │                       │                       │
           ▼                       ▼                       ▼
    [QuestionAnswerAdvisor]   [CourseTools]           ...
    Redis VectorStore         @Tool 注解工具
```

### 编码阶段

**核心代码实现**：

```java
// Agent.java - 智能体统一接口定义
public interface Agent {
    /**
     * 处理流式请求（如流式回答）
     */
    Flux<ChatEventVO> processStream(String question, String sessionId);
    
    /**
     * 处理标准请求（非流式）
     */
    String process(String question, String sessionId);
    
    /**
     * 获取智能体类型标识
     */
    AgentTypeEnum getAgentType();
    
    // 默认方法：系统提示、工具列表、Advisor列表等
    default String systemMessage() { return ""; }
    default Object[] tools() { return EMPTY_OBJECTS; }
    default List<Advisor> advisors() { return List.of(); }
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
    public List<Advisor> advisors() {
        // 路由智能体不需要记忆功能，返回空列表
        return List.of();
    }
    
    @Override
    public Object[] tools() {
        // 路由智能体不需要工具，返回空数组
        return EMPTY_OBJECTS;
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
    
    private Agent findAgentByType(AgentTypeEnum agentTypeEnum) {
        var beans = SpringUtil.getBeansOfType(Agent.class);
        for (var agent : beans.values()) {
            if (agentTypeEnum == agent.getAgentType()) {
                return agent;
            }
        }
        return null;
    }
}
```

```java
// CourseTools.java - 工具调用实现
@Service
public class CourseTools {
    @Autowired
    private CourseService courseService;
    
    @Tool(description = "根据课程ID查询课程信息")
    public CourseInfo queryCourseById(
            @ToolParam(description = "课程ID") Long courseId,
            ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> courseService.getById(id))
                .map(CourseInfo::of)
                .map(courseInfo -> {
                    // 将工具调用结果存入 ToolResultHolder
                    Object requestIdObj = toolContext.getContext().get(Constant.REQUEST_ID);
                    String requestId = requestIdObj != null ? String.valueOf(requestIdObj) : null;
                    ToolResultHolder.put(requestId, "courseInfo_" + courseInfo.getId(), courseInfo);
                    return courseInfo;
                })
                .orElse(null);
    }
}
```

### 问题修复阶段

**问题1**：路由智能体返回结果包含多余字符，导致意图解析失败

**修复方案**：在 `AgentServiceImpl` 中添加结果清理逻辑，去除空白字符并转为大写

```java
var cleanedResult = result.trim().toUpperCase();
var agentTypeEnum = AgentTypeEnum.agentNameOf(cleanedResult);
```

**问题2**：工具调用结果无法与请求关联

**修复方案**：通过 `ToolContext` 传递 `requestId`，使用 `ToolResultHolder` 存储工具调用结果

```java
public Map<String, Object> toolContext(String sessionId, String requestId) {
    return Map.of(Constant.REQUEST_ID, requestId);
}
```

---

## 二、语音工作流模块（speech）

### 需求阶段

**需求背景**：实现英语学习辅助功能，支持单词造句、翻译和语音合成的完整工作流。

**痛点**：
- 学习流程需要多步骤编排（造句→翻译→语音）
- 各步骤之间需要状态传递
- 需要支持工作流的可视化和可维护性

### 设计阶段

**设计思路**：

Q：为什么用 DashScope Graph 而不是传统的链式调用？
> A：Graph 状态图引擎提供了可视化的工作流编排能力，支持节点化设计、状态传递和流程控制。相比链式调用，Graph 更灵活、更易于维护和扩展。

Q：为什么采用异步节点设计？
> A：异步节点可以并行执行多个任务，提高工作流的执行效率。同时，异步设计可以避免单个节点的阻塞影响整个流程。

**架构设计**：
```
用户输入单词 → [EnlishController] → [NodeLink] 编译状态图
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
            [Sentence 节点]    [Translation 节点]    [Read 节点]
            AI 生成英文例句    AI 翻译成中文          语音合成 MP3
                    │                 │                 │
                    └─────────────────┴─────────────────┘
                                      │
                                      ▼
                          返回 {sentence, translation, audioPath}
```

### 编码阶段

**核心代码实现**：

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
        return chatClient.prompt().user(word).call().content();
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
// EnlishController.java - 英语学习流程入口
@RestController
@RequestMapping("/Enlish")
public class EnlishController {
    @Autowired
    private NodeLink nodeLink;
    
    @PostMapping
    public Object flow(@RequestParam("word") String word) {
        CompiledGraph compiledGraph = nodeLink.toEnlish();
        return compiledGraph.invoke(Map.of("word", word))
                .map(state -> "==>sentence==>" + state.value("sentence").orElse("null") +
                             "==>translation==>" + state.value("translation").orElse("null") +
                             "==>read==>" + state.value("read").orElse("null"))
                .orElse("执行失败");
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
        
        // 编译状态图并生成 PlantUML 可视化
        try {
            CompiledGraph compiledGraph = graph.compile();
            // 生成 PlantUML 格式的图可视化表示
            GraphRepresentation representation = graph.getGraph(
                    GraphRepresentation.Type.PLANTUML, "English Flow");
            System.out.println("=== English Flow UML Diagram ===");
            System.out.println(representation.content());
            return compiledGraph;
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### 问题修复阶段

**问题1**：音频文件保存路径硬编码

**修复方案**：将保存路径配置化，支持通过配置文件指定存储位置

**问题2**：节点间状态传递不够灵活

**修复方案**：使用 `OverAllState` 的 `Map<String, Object>` 格式传递状态，支持任意类型的数据传递

---

## 三、视觉识别模块（see）

### 需求阶段

**需求背景**：实现图像识别功能，支持图片内容识别和工具调用联动。

**痛点**：
- 图片上传需要尺寸校验和安全过滤
- 需要支持本地文件和上传文件两种输入方式
- 识别结果需要与工具调用联动

### 设计阶段

**设计思路**：

Q：为什么将图片转为 Base64 而不是直接传递文件路径？
> A：多模态模型需要将图片数据嵌入请求中，Base64 编码是标准的嵌入方式。同时，Base64 编码便于在节点间传递，无需处理文件路径问题。

Q：为什么需要敏感词拦截器？
> A：图像识别可能返回包含敏感内容的结果，敏感词拦截器可以在识别后进行过滤，确保返回内容符合安全规范。

**架构设计**：
```
用户上传图片 → [ImgComperhendController]
                    │
                    ├→ 尺寸校验（最大 2048×2048）
                    ├→ SensitiveWordInterceptor 敏感词检测
                    │
                    ▼
            [VisionService]
                    │
                    ├→ Base64 编码（data:image/jpeg;base64,...）
                    │
                    ▼
            [OpenAiChatModel] 多模态调用 → 返回识别结果
```

### 编码阶段

**核心代码实现**：

```java
// VisionService.java - 视觉识别服务
@Service
public class VisionService {
    @Autowired
    private ChatClient visualChatClient;
    
    /**
     * 本地文件识图
     */
    public String visionByFile(File file, String prompt) throws Exception {
        String base64 = toBase64(file);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, 
                URI.create("data:image/jpeg;base64," + base64));
        return visualChatClient.prompt()
                .user(userMessage -> userMessage.text(prompt).media(media))
                .call()
                .content();
    }
    
    /**
     * 上传文件byte识图（接口常用）
     */
    public String visionByBytes(byte[] imgBytes, String prompt) {
        String base64 = Base64.getEncoder().encodeToString(imgBytes);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, 
                URI.create("data:image/jpeg;base64," + base64));
        return visualChatClient.prompt()
                .user(userMessage -> userMessage.text(prompt).media(media))
                .call()
                .content();
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
        
        // 编译状态图并生成 PlantUML 可视化
        try {
            CompiledGraph compiledGraph = graph.compile();
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

### 问题修复阶段

**问题1**：图片尺寸过大导致识别失败

**修复方案**：在控制器中添加图片尺寸校验，限制最大尺寸为 2048×2048

**问题2**：识别结果可能包含敏感内容

**修复方案**：集成 `SensitiveWordInterceptor` 敏感词拦截器，对 `/rag` 和 `/tool` 路径启用敏感词检测

---

## 四、语音合成模块（aliyun）

### 需求阶段

**需求背景**：实现语音识别（ASR）和语音合成（TTS）功能，支持中英文语音交互。

**痛点**：
- 语音文件格式多样，需要统一处理
- 大文件上传需要流式处理
- 音频转写结果需要准确可靠

### 设计阶段

**设计思路**：

Q：为什么使用 Base64 编码方式上传音频？
> A：Base64 编码可以将二进制音频数据转为文本格式，便于在 HTTP 请求中传输，无需处理文件上传的复杂逻辑。

Q：为什么 TTS 使用流式合成？
> A：流式合成可以逐块获取音频数据，减少内存占用，同时提高响应速度。

**架构设计**：
```
语音识别：上传音频 → Base64 编码 → DashScope ASR → 返回文本结果
语音合成：输入文本 → DashScope TTS 流式合成 → 返回音频字节 → 保存为 MP3 文件
```

### 编码阶段

**核心代码实现**：

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
        String audioFormat = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase() 
                : "mp3";
        
        // 将音频文件转换为 Base64
        String base64Audio = Base64.getEncoder().encodeToString(file.getBytes());
        
        // 构造 ASR 请求
        DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
                .model("paraformer-v2")
                .build();
        
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

**问题1**：音频格式硬编码

**修复方案**：从文件名中自动提取音频格式，支持 mp3、wav、m4a 等多种格式

```java
String audioFormat = originalFilename != null && originalFilename.contains(".") 
        ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase() 
        : "mp3";
```

**问题2**：大文件合成内存溢出

**修复方案**：使用流式合成（`stream()`），逐块获取音频数据并合并，减少内存占用

---

# 核心组件设计

## 1. 会话记忆管理（ChatMemory）

**设计思路**：

Q：为什么提供两种记忆存储方式（MySQL 和 Redis）？
> A：MySQL 适合持久化存储，支持会话的长期保存和复杂查询；Redis 适合缓存存储，读写速度快，适合高并发场景。通过 `@Profile` 注解实现运行时切换。

Q：为什么保存时先删除再批量保存？
> A：Spring AI 的 `saveAll` 方法会传入全部消息数据，包括之前的历史记录。先删除再保存可以确保数据一致性，避免重复存储。

**代码实现**：

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

**设计思路**：

Q：为什么设置相似度阈值为 0.6？
> A：相似度阈值控制检索结果的准确性。0.6 是一个经验值，可以过滤掉不相关的文档，同时保留足够的上下文信息。

Q：为什么设置 Top-K 为 6？
> A：Top-K 控制返回的文档数量。6 个文档可以提供足够的上下文信息，同时避免过多文档导致 Token 超限。

**代码实现**：

```java
// RecommendAgent.java - RAG 配置
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

**设计思路**：

Q：为什么使用接口+抽象类+实现类的三层结构？
> A：接口 `Agent` 定义智能体的核心能力契约；抽象类 `AbstractAgent` 实现通用逻辑（流式处理、请求ID生成、工具上下文管理）；具体实现类（`RouteAgent`、`KnowledgeAgent`、`RecommendAgent`）专注于各自的业务逻辑。这种结构实现了代码复用和职责分离。

Q：为什么使用默认方法（default）定义扩展点？
> A：默认方法允许在接口中提供默认实现，子类可以按需覆盖。这样新增智能体时只需实现核心方法，扩展点可以使用默认行为。

**接口层次结构**：
```
Agent（接口）→ AbstractAgent（抽象类）→ RouteAgent/KnowledgeAgent/RecommendAgent（实现类）
```

**代码实现**：

```java
// Agent.java - 智能体核心接口
public interface Agent {
    // 核心方法（子类必须实现）
    Flux<ChatEventVO> processStream(String question, String sessionId);
    String process(String question, String sessionId);
    AgentTypeEnum getAgentType();
    
    // 默认方法（子类可选覆盖）
    default String systemMessage() { return ""; }
    default Object[] tools() { return EMPTY_OBJECTS; }
    default List<Advisor> advisors() { return List.of(); }
    default Map<String, Object> toolContext(String sessionId, String requestId) { return Map.of(); }
}
```

## 4. 工具结果保持器（ToolResultHolder）

**设计思路**：

Q：为什么需要 ToolResultHolder？
> A：工具调用的结果需要与具体请求关联，以便后续使用。`ToolResultHolder` 使用 `ConcurrentHashMap` 存储工具调用结果，以 `requestId` 为键，支持多线程并发访问。

Q：为什么使用嵌套 Map 结构？
> A：外层 Map 以 `requestId` 为键，内层 Map 以字段名（如 `courseInfo_123`）为键。这种结构支持一个请求调用多个工具，每个工具结果存储在不同的字段中。

**代码实现**：

```java
// ToolResultHolder.java - 工具结果保持器
public class ToolResultHolder {
    // ConcurrentHashMap 保证线程安全
    private static final Map<String, Map<String, Object>> HANDLER_MAP = new ConcurrentHashMap<>();
    
    private ToolResultHolder() {
        // 工具类，禁止实例化
    }
    
    /**
     * 存储工具调用结果
     * @param key 请求ID
     * @param field 字段名
     * @param result 结果对象
     */
    public static void put(String key, String field, Object result) {
        Assert.notNull(key, "key is not null!");
        Assert.notNull(field, "field is not null!");
        HANDLER_MAP.computeIfAbsent(key, k -> new HashMap<>()).put(field, result);
    }
    
    /**
     * 获取指定请求的所有工具结果
     */
    public static Map<String, Object> get(String key) {
        return key == null ? null : HANDLER_MAP.get(key);
    }
    
    /**
     * 获取指定请求的指定字段结果
     */
    public static Object get(String key, String field) {
        Assert.notNull(key, "key is not null!");
        Assert.notNull(field, "field is not null!");
        return Optional.ofNullable(HANDLER_MAP.get(key))
                .map(map -> map.get(field))
                .orElse(null);
    }
    
    /**
     * 清理指定请求的工具结果
     */
    public static void remove(String key) {
        Assert.notNull(key, "key is not null!");
        HANDLER_MAP.remove(key);
    }
}
```

---

# 依赖说明

## 智能体路由模块（rag-chat）

| 依赖 | 版本 | 功能支撑 |
| :--- | :--- | :--- |
| Spring Boot | 3.3.8 | 应用框架，自动配置数据源、Redis等基础设施 |
| Spring AI | 1.1.0 | AI 能力集成框架，ChatClient、Agent 体系 |
| MyBatis Plus | 3.5.9 | 会话记录数据 CRUD；AutoMetaObjectHandler 自动填充时间字段 |
| Spring Boot Starter Data Redis | 3.3.8 | 会话记忆缓存、向量存储 |
| Hutool All | 5.8.36 | BeanUtil 对象转换；CollStreamUtil 集合处理 |

## 语音工作流模块（speech）

| 依赖 | 版本 | 功能支撑 |
| :--- | :--- | :--- |
| Spring AI | 1.1.0 | ChatClient 对话调用、TTS 语音合成 |
| DashScope Graph | 1.1.0.0 | 状态图编排引擎，节点化工作流设计 |

## 视觉识别模块（see）

| 依赖 | 版本 | 功能支撑 |
| :--- | :--- | :--- |
| Spring AI | 1.1.0 | 多模态模型调用、Media 内容嵌入 |
| SensitiveWord | - | 敏感词检测和过滤 |

## 语音合成模块（aliyun）

| 依赖 | 版本 | 功能支撑 |
| :--- | :--- | :--- |
| DashScope SDK | - | ASR 语音识别、TTS 语音合成 |
| Spring AI | 1.1.0 | 音频模型集成框架 |

---

# 功能架构总览

| 模块 | 核心功能 | 技术要点 |
| :--- | :--- | :--- |
| **rag-chat** | Agent 智能体路由、RAG 检索、工具调用、会话管理 | Spring AI, MyBatis Plus, Redis, MySQL |
| **speech** | 英语学习工作流、DashScope Graph 状态图 | Spring AI, DashScope Graph, SiliconFlow |
| **see** | 图像识别、敏感词过滤、工具调用 | Spring AI, ModelScope, OpenAI Vision |
| **aliyun** | 语音识别（ASR）、语音合成（TTS） | DashScope SDK, Spring AI Audio |
| **frontend-vue-ai** | 前端界面、流式对话展示、暗黑主题 | Vue 3, Element Plus |

---

# 前端功能演示

| 上传文件+输入问题 | ![封面](D:说明\原型设计\1.png) |
| ----------------- | ------------------------------ |
|                   |                                |



**技术实现**：

- Vue 3 Composition API（`<script setup>`）
- Element Plus 组件库
- 响应式状态管理（`ref`, `watch`, `nextTick`）
- 自定义打字机效果（`setInterval` 控制逐字显示）



------