# AI Assistant - 智能 AI 助手

> 一个基于 Spring AI 的多功能 AI 助手后端服务平台，集成大语言模型对话、智能体路由、RAG 检索增强、工具调用、图像识别、工作流引擎等多种 AI 能力。

## 

### Situation（背景）

随着大语言模型的快速发展，企业和个人对 AI 助手的需求日益增长。然而，单一的对话模型往往难以满足多样化的业务场景需求——例如需要结合私有知识库回答专业问题、通过工具调用查询业务数据、识别图片内容、将文本转为语音等。传统的 AI 应用架构缺乏灵活性和可扩展性，难以快速集成多种 AI 能力并实现智能化的任务路由。

### Task（目标）

设计并实现一个**模块化、可扩展的 AI 助手后端平台**，核心目标包括：

- **多轮对话**：支持上下文感知的智能对话，具备会话记忆管理能力
- **智能路由**：根据用户意图自动路由到不同的专业智能体处理
- **RAG 增强**：结合向量检索技术，让 AI 能够基于私有知识库回答问题
- **工具调用**：支持 AI 自主调用外部工具（如课程查询、数据库操作）
- **视觉识别**：集成多模态模型，支持图片内容识别
- **工程化架构**：模块化拆分、可独立部署、易于扩展

### Action（行动）

围绕上述目标，基于 **Spring Boot 3.x + Spring AI 1.x** 技术栈，采用 **Maven 多模块架构** 构建后端服务，配套 **Vue 3 前端** 实现用户交互：

| 模块 | 技术要点 | 核心职责 |
|------|----------|----------|
| **rag-chat** | Spring AI, MyBatis Plus, Redis, MySQL | RAG 检索增强、工具调用、Agent 智能对话、会话管理 |
| **speech** | Spring AI, DashScope Graph, SiliconFlow | 语音识别、语音合成、英语学习工作流 |
| **see** | Spring AI, ModelScope, OpenAI Vision | 图像识别、敏感词过滤、工具调用 |
| **frontend-vue-ai** | Vue 3, Element Plus | 前端界面，流式对话展示、暗黑主题 |

#### 关键实现

1. **Agent 智能体体系**
   - 定义统一的 `Agent` 接口，支持 `processStream()` 流式处理和 `process()` 同步处理
   - `RouteAgent`：分析用户意图，返回意图标识（如 RECOMMEND、KNOWLEDGE）
   - `KnowledgeAgent`：基于 RAG 检索增强的知识问答
   - `RecommendAgent`：结合课程工具和 RAG 的课程推荐
   - 使用 `SpringUtil.getBeansOfType()` 实现 Agent 的动态查找和路由
2. **RAG 检索增强**
   - 基于 Redis 向量存储（VectorStore）实现语义检索
   - `QuestionAnswerAdvisor` 自动从知识库检索相关内容作为上下文
   - 支持相似度阈值（0.6）和 Top-K（6）的检索策略
3. **工具调用（Tool Calling）**
   - 通过 `@Tool` 注解实现 AI 自主调用：按 ID、名称、分类、价格等多维度查询课程
   - `ToolResultHolder` 管理工具调用结果与请求的关联
4. **会话记忆管理**
   - 支持 **MySQL 持久化** 和 **Redis 缓存** 两种记忆存储方式
   - 会话按时间分组：当天、最近30天、最近1年、1年以上
5. **多模态能力**
   - **Vision**：图片转 base64 嵌入多模态模型，支持本地文件和上传图片输出
6. **Graph 状态图**
   - 使用 DashScope Graph 定义英语学习流程图
   - 节点链：接收单词 → AI 造句 → AI 翻译 → 返回结果
   - 支持 PlantUML 可视化输出
7. **安全与过滤**
   - `SensitiveWordInterceptor`：敏感词拦截器，对 `/rag` 和 `/tool` 路径启用
   - 图片上传尺寸校验（最大 2048×2048）

### Result（成果）

成功构建了一个**功能完备、架构清晰**的 AI 助手平台：

- **7 个核心 API**：对话、Agent 路由、RAG 检索、向量嵌入、会话管理、工具调用、语音合成
- **3 种 Agent 类型**：路由、知识问答、课程推荐，可按需扩展
- **2 种记忆存储**：MySQL 持久化和 Redis 缓存，适应不同场景
- **1 套完整前端**：Vue 3 流式对话界面，支持暗黑主题、逐字打字机动画
- **工程化交付**：Maven 多模块管理，各模块可独立部署、独立演进

---

## 功能架构

### 1. 路由智能体工作流— rag模块

本系统采用 **路由-执行模式（Routing-Execution Pattern）** 构建智能体工作流引擎，通过统一的 `Agent` 接口体系实现意图识别、智能路由和专业化处理。

**智能体工作流总览**：

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

**智能体接口体系**：

| 层级 | 类名 | 职责 |
|------|------|------|
| **接口层** | `Agent` | 定义智能体核心能力：`processStream()`、`process()`、`getAgentType()` |
| **抽象层** | `AbstractAgent` | 实现通用逻辑：流式处理、请求ID生成、工具上下文管理、会话记忆 |
| **实现层** | `RouteAgent` | 意图分析与路由分发 |
| **实现层** | `KnowledgeAgent` | RAG 检索增强知识问答 |
| **实现层** | `RecommendAgent` | 课程工具调用 + RAG 推荐 |

**智能体类型枚举**：

```java
// AgentTypeEnum 定义了5种智能体类型，支持按需扩展
ROUTE("路由智能体"),        // 意图识别与路由
RECOMMEND("课程推荐智能体"), // 课程推荐 + 工具调用
CONSULT("课程咨询智能体"),   // 预留：课程咨询服务
BUY("课程购买智能体"),       // 预留：购买流程处理
KNOWLEDGE("知识讲解智能体")  // RAG 知识问答
```

#### 1.1 路由智能体（RouteAgent）

**职责**：分析用户意图，返回意图标识，作为工作流的入口网关。

```
用户问题 → [RouteAgent] → LLM 意图分析 → 返回意图标识(ROUTE/RECOMMEND/KNOWLEDGE)
```

**关键配置**：

- **ChatClient**：使用专用的 `routeChatClient`，避免记忆功能干扰路由决策
- **Advisor**：空列表，路由不需要会话记忆
- **Tools**：空数组，路由不需要工具调用
- **System Prompt**：定义意图分类规则，仅返回标准化的意图标识

#### 1.2 知识问答智能体（KnowledgeAgent）

**职责**：基于 RAG 检索增强，从私有知识库获取相关内容，生成精准回答。

```
用户问题 → [KnowledgeAgent] → [ChatClient] → AI 回答
              │
              └→ [ChatMemory] 会话上下文注入
```

**关键配置**：

- **ChatClient**：使用默认 `chatClient`（带记忆功能）
- **Advisor**：继承默认配置，支持会话记忆
- **Tools**：空数组，纯知识问答无需工具
- **System Prompt**：知识讲解专用提示词

#### 1.3 课程推荐智能体（RecommendAgent）

**职责**：结合课程工具调用和 RAG 检索，实现智能课程推荐。

```
用户问题 → [RecommendAgent] 
              │
              ├→ [QuestionAnswerAdvisor] → Redis VectorStore 检索 → 知识库上下文
              │
              ├→ [CourseTools] → @Tool 注解 → 数据库课程查询
              │
              └→ [ChatClient] → 整合工具结果 + 知识库 → AI 推荐回答
```

---

### 2. 语音工作流 — speech 模块

基于 **DashScope Graph 状态图引擎** 构建英语学习工作流，以有向图方式编排 AI 节点。

**工作流架构**：

```
用户输入单词 → [EnlishController] → [GraphConfig] 编译状态图
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

**节点设计**：

| 节点 | 输入 | 处理逻辑 | 输出 |
|------|------|----------|------|
| **Sentence** | 单词 | 调用 ChatClient 生成英文例句 | 英文句子 |
| **Translation** | 英文句子 | 调用 ChatClient 翻译成中文 | 中文翻译 |
| **Read** | 中文翻译 | 调用 TTS 模型生成语音文件 | MP3 文件路径 |

**状态管理**：

- 使用 `OverAllState` 存储工作流上下文
- 前一节点的输出通过 `state.value()` 传递给下一节点
- 支持 `Map<String, Object>` 格式的状态数据传递

---

### 3. 视觉识别工作流 — see 模块

基于 **多模态大模型（ModelScope Qwen）** 实现图像识别，采用节点化设计处理视觉任务。

**工作流架构**：

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

**节点化设计**：

| 节点 | 职责 |
|------|------|
| **VisualNode** | 处理图像识别任务，封装多模态模型调用 |
| **ToolNode** | 处理工具调用任务，如套餐查询（`SetmealTool`） |

**核心实现**：

- **灵活输入**：支持本地文件路径和 HTTP 上传字节两种方式
- **安全过滤**：`SensitiveWordInterceptor` 对 `/rag` 和 `/tool` 路径启用敏感词检测
- **多模态嵌入**：将图片转为 Base64 URI 嵌入模型请求，实现图文理解

## 项目结构

```
ai-assistant/
├── backend-ai/                      # 后端父工程（Maven 多模块）
│   ├── rag-chat/                    # RAG + 工具调用核心模块
│   │   ├── src/main/java/
│   │   │   ├── mapper/              # MyBatis Plus Mapper 接口
│   │   │   ├── model/
│   │   │   │   ├── dto/             # 数据传输对象（ChatDTO）
│   │   │   │   ├── entity/          # 数据库实体（ChatRecord, Session）
│   │   │   │   ├── enums/           # 枚举类型（AgentType, ChatEventType, MessageType）
│   │   │   │   └── vo/              # 视图对象（ChatEventVO, SessionVO 等）
│   │   │   ├── service/
│   │   │   │   ├── chat/            # 对话服务（ChatService, AgentService, RagService）
│   │   │   │   ├── flow/            # Agent 智能体（RouteAgent, KnowledgeAgent, RecommendAgent）
│   │   │   │   ├── memory/          # 会话记忆（MySQL 持久化 + Redis 缓存）
│   │   │   │   ├── rag/             # RAG 向量检索
│   │   │   │   ├── tools/           # 工具调用（课程查询工具）
│   │   │   │   └── SessionService   # 会话管理
│   │   │   └── start/               # 启动配置与控制器
│   │   ├── src/main/resources/      # 配置文件
│   │   └── src/test/java/           # 测试代码
│   ├── speech/                      # 语音识别 + 语音合成 + 工作流模块
│   │   ├── src/main/java/
│   │   │   ├── chat/
│   │   │   │   ├── config/          # Spring AI 配置、Graph 配置
│   │   │   │   ├── node/            # 图节点（Sentence, Translation, Read）
│   │   │   │   ├── start/           # 控制器（ASRController, ASR2, EnlishController）
│   │   │   │   └── LanguageApplication.java
│   │   │   └── config/              # 自定义配置（CustomTranscriptionConfig）
│   │   └── src/main/resources/      # 配置文件（SiliconFlow API）
│   ├── see/                         # 视觉识别 + 工具调用模块
│   │   ├── src/main/java/
│   │   │   ├── chat/
│   │   │   │   ├── Interceptor/     # 敏感词拦截器
│   │   │   │   ├── config/          # Spring AI 配置
│   │   │   │   ├── controller/      # 图像识别 API
│   │   │   │   └── node/            # 节点（ToolNode, VisualNode）
│   │   └── src/main/resources/      # 配置文件（ModelScope API）
│   └── pom.xml                      # Maven 父工程配置
└── README.md
```

---

## 技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java (OpenJDK) | 17 | 后端开发语言 |
| 框架 | Spring Boot | 3.3.8 | 后端应用框架 |
| AI 框架 | Spring AI | 1.1.0 | AI 能力集成框架 |
| ORM | MyBatis Plus | 3.5.9 | 数据库访问框架 |
| 数据库 | MySQL | 8.0+ | 会话记录持久化 |
| 缓存 | Redis | 7.0+ | 会话记忆缓存、向量存储 |
| 向量存储 | Redis Vector Store | - | RAG 检索向量数据库 |
| 大模型 | OpenAI / 阿里云 DashScope / ModelScope | - | 多厂商模型支持 |
| 语音识别 | SiliconFlow SenseVoiceSmall | - | 中英文语音转文字 |
| 语音合成 | DashScope CosyVoice2 | - | 文本转语音 |
| 工作流引擎 | DashScope Graph | 1.1.0.0 | 状态图编排引擎 |
| 工具库 | Hutool | 5.8.36 | Java 工具库 |
| 前端 | Vue 3 + Element Plus | - | 前端 UI 框架 |

---



## 环境要求

| 依赖 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | 后端运行环境 |
| Maven | 3.8+ | 项目构建工具 |
| MySQL | 8.0+ | 会话记录持久化（rag-chat 模块） |
| Redis | 7.0+ | 会话记忆缓存、向量存储（rag-chat 模块） |
| AI API Key | - | ModelScope / SiliconFlow |

### 配置说明

每个模块使用独立的配置文件，需在对应模块的 `src/main/resources/application.properties` 中配置：

| 模块 | 配置文件 | 所需 API Key |
|------|----------|-------------|
| rag-chat | `application-dev.properties` | ModelScope |
| speech | `application.properties` | SiliconFlow |
| see | `application.properties` | ModelScope |
