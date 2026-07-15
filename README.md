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

## 功能

### 1. RAG + 工具调用 — rag-chat 模块

结合 **RAG 检索增强** 和 **Tool Calling 工具调用**，让 AI 既能从私有知识库获取信息，又能自主查询业务数据，实现精准回答。

**RAG 检索增强**：

```
用户提问 → [QuestionAnswerAdvisor] 向量检索 → 知识库上下文注入 → [ChatClient] AI 回答
```

- **向量存储**：基于 Redis VectorStore 存储文档向量，支持语义相似度检索
- **自动增强**：`QuestionAnswerAdvisor` 在每次对话中自动从知识库检索相关内容，注入 AI 上下文
- **可配置策略**：相似度阈值 0.6、Top-K 6 条

**工具调用（Tool Calling）**：

```
用户提问 → [ChatClient] 意图分析 → [@Tool 注解工具] 自主调用 → 返回结果
```

- **课程查询工具**：通过 `@Tool` 注解注册，支持按 ID、名称、分类、价格区间、状态等多维度查询课程
- **AI 自主决策**：AI 模型根据用户问题自动选择合适的工具方法查询数据库
- **结果关联**：`ToolResultHolder` 跟踪工具调用结果与请求 ID 的映射

**Agent 智能体体系**：

- **路由智能体（RouteAgent）**：分析用户意图，返回意图标识（RECOMMEND、KNOWLEDGE）
- **知识问答智能体（KnowledgeAgent）**：基于 RAG 检索增强的知识问答
- **推荐智能体（RecommendAgent）**：结合课程工具和 RAG 的课程推荐
- 使用 `SpringUtil.getBeansOfType()` 实现 Agent 的动态查找和路由

#### 架构设计

整体采用 **路由-执行** 模式（Routing-Execution Pattern）作为核心业务流程引擎：

```
用户输入 → [路由智能体] → 意图分类
                            ├→ 知识问答智能体（RAG 增强）
                            ├→ 课程推荐智能体（工具调用）
                            └→ 路由规则变更（界面 UI 控制）
```

#### 模块划分

**会话记忆管理**：

- 支持 **MySQL 持久化** 和 **Redis 缓存** 两种记忆存储方式
- 会话按时间分组：当天、最近30天、最近1年、1年以上
- `MessageWindowChatMemory` 支持最多保存 20 条对话上下文

### 2. 语音识别与合成 — speech 模块

基于 **SiliconFlow** 平台实现完整的语音能力，包括语音识别（ASR）和语音合成（TTS）。

**语音识别（ASR）**：

| 接口           | 路径    | 实现方式                                | 返回格式                                        |
| -------------- | ------- | --------------------------------------- | ----------------------------------------------- |
| 手动构建       | `/asr`  | HttpClient 手动构建 multipart/form-data | `{"success": boolean, "transcription": string}` |
| Spring AI 封装 | `/asr2` | `OpenAiAudioTranscriptionModel.call()`  | `{"success": boolean, "transcription": string}` |
| 简化返回       | `/asr3` | Spring AI 封装，仅返回文本              | 纯文本字符串                                    |

**核心实现要点**：

- 使用 Spring AI 的 `OpenAiAudioTranscriptionModel` 封装 SiliconFlow API 调用
- 配置 `response-format=json` 确保 API 返回 JSON 格式响应
- 自定义 `CustomTranscriptionConfig` 配置 `RestClient.Builder`，解决 SiliconFlow 返回 `application/octet-stream` 导致的反序列化问题
- `MappingJackson2HttpMessageConverter` 同时支持 `application/json` 和 `application/octet-stream` 媒体类型

**语音合成（TTS）**：

- **模型**：`FunAudioLLM/CosyVoice2-0.5B`
- **输出格式**：MP3
- **可配置参数**：语速（speed）、音色（voice）、响应格式（response-format）
- **工作流集成**：作为 `Read` 节点集成到英语学习工作流中，自动将翻译结果转为语音

**英语学习工作流**：

基于 **DashScope Graph 状态图引擎** 构建英语学习工作流，以有向图方式编排 AI 节点。

```
用户输入单词 → [Sentence 节点] AI 造句 → [Translation 节点] AI 翻译 → [Read 节点] 语音合成 → 返回结果
```

**工作流实现分析**：

- **节点设计**：
  - `Sentence`：接收单词，调用 `ChatClient` 生成英文例句
  - `Translation`：接收例句，调用 `ChatClient` 翻译成中文
  - `Read`：接收中文翻译，调用 `OpenAiAudioSpeechModel` 生成语音文件

- **状态管理**：
  - 使用 `OverAllState` 存储工作流上下文
  - 前一节点的输出通过 `state.value()` 传递给下一节点
  - 支持 `Map<String, Object>` 格式的状态数据传递

- **图配置**：
  - `GraphConfig` 定义节点链和连接关系
  - 编译时自动生成 PlantUML 图，便于可视化调试

- **自动执行**：
  - 状态图编译后自动执行节点链
  - 无需手动编排调用顺序

### 3. 视觉识别 — see 模块

基于 **多模态大模型（ModelScope Qwen）** 实现图像识别，支持用户上传图片并由 AI 分析识别内容。

**图像识别流程**：

```
用户上传图片 → [ImgComperhendController] 尺寸校验 → Base64 编码 → [VisionService] 多模态模型调用 → 返回识别结果
```

**核心实现**：

- **图片上传识别**：接收上传的图片文件，自动校验尺寸（最大 2048×2048）
- **Base64 嵌入**：将图片转为 `data:image/jpeg;base64,...` URI 嵌入多模态模型请求
- **灵活输入**：支持本地文件路径和 HTTP 上传字节两种方式
- **安全过滤**：`SensitiveWordInterceptor` 对对话接口启用敏感词检测，命中则返回 400

**工具调用集成**：

- 套餐工具查询：通过 `SetmealTool` 实现数据库查询
- 节点化设计：`ToolNode` 和 `VisualNode` 分别处理工具调用和视觉识别任务

## 工作流分析

### rag-chat 模块工作流

```
用户输入 → [AgentController] → [AgentServiceImpl] → [RouteAgent] 意图识别
                                                           ├→ [KnowledgeAgent] → [RagServiceImpl] → Redis VectorStore 检索 → ChatClient 回答
                                                           └→ [RecommendAgent] → [CourseTools] 工具调用 → ChatClient 回答
```

- **路由决策**：`RouteAgent` 根据用户问题判断意图，分发到对应的专业智能体
- **RAG 增强**：`KnowledgeAgent` 通过 `QuestionAnswerAdvisor` 自动从知识库检索相关内容
- **工具调用**：`RecommendAgent` 通过 `@Tool` 注解的 `CourseTools` 查询课程数据库
- **会话记忆**：所有对话通过 `ChatMemory` 管理上下文，支持 MySQL 和 Redis 两种存储方式

### speech 模块工作流

```
┌─────────────────────────────────────────────────────────────────────┐
│                        英语学习工作流                              │
├─────────────────────────────────────────────────────────────────────┤
│ 用户输入单词 → [EnlishController] → [GraphConfig] 编译状态图        │
│                                      ↓                            │
│                          [Sentence 节点] 生成英文例句               │
│                                      ↓                            │
│                          [Translation 节点] 翻译成中文             │
│                                      ↓                            │
│                          [Read 节点] 语音合成生成 MP3              │
│                                      ↓                            │
│                          返回 {sentence, translation, audioPath}   │
└─────────────────────────────────────────────────────────────────────┘
```

- **状态图编排**：使用 DashScope Graph 定义节点链，编译后自动执行
- **节点通信**：通过 `OverAllState` 传递数据，前一节点输出作为后一节点输入
- **可视化调试**：编译时生成 PlantUML 图，便于理解工作流拓扑

### see 模块工作流

```
用户上传图片 → [ImgComperhendController] 尺寸校验 → Base64 编码
                    ↓
            [VisionService] 构建多模态请求 → [OpenAiChatModel] 调用
                  									  ↓
                								返回识别结果
```

- **安全过滤**：`SensitiveWordInterceptor` 对请求进行敏感词检测
- **图片处理**：自动校验尺寸，支持本地文件和上传文件两种输入方式
- **多模态调用**：将图片转为 Base64 嵌入模型请求，实现图文理解

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

## 使用示例

### RAG 对话

```bash
# 调用智能路由对话接口（SSE 流式）
curl -X POST http://localhost:8080/agent \
  -H "Content-Type: application/json" \
  -d '{"question": "推荐一门Java课程", "sessionId": "test-123"}'

# 调用普通对话接口
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "什么是RAG?", "sessionId": "test-123"}'
```

### 工具调用

```bash
# 调用课程查询工具
curl http://localhost:8080/tool
```

### 语音识别

```bash
# 调用 /asr3 接口（直接返回文本）
curl -X POST http://localhost:8080/asr3 \
  -F "file=@test.mp3"

# 调用 /asr2 接口（完整结果）
curl -X POST http://localhost:8080/asr2 \
  -F "file=@test.wav"
```

### 英语学习工作流

```bash
# 输入单词，获取造句 + 翻译 + 语音合成
curl -X POST http://localhost:8080/Enlish \
  -H "Content-Type: application/json" \
  -d '{"word": "serendipity"}'
```

### 图像识别

```bash
# 调用图像识别接口
curl -X POST http://localhost:8080/vision \
  -F "file=@test.jpg"
```

---

