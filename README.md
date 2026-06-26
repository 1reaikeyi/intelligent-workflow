# AI Assistant - 智能 AI 助手

> 一个基于 Spring AI 的多功能 AI 助手后端服务平台，集成大语言模型对话、智能体路由、RAG 检索增强、工具调用、图像识别、工作流引擎等多种 AI 能力。

---

## STAR

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

#### 架构设计

整体采用 **路由-执行** 模式（Routing-Execution Pattern）作为核心业务流程引擎：

```
用户输入 → [路由智能体] → 意图分类
                            ├→ 知识问答智能体（RAG 增强）
                            ├→ 课程推荐智能体（工具调用）
                            └→ 路由规则变更（界面 UI 控制）
```

#### 模块划分

| 模块 | 技术要点 | 核心职责 |
|------|----------|----------|
| **rag-chat** | Spring AI, MyBatis Plus, Redis, MySQL | RAG 检索增强、工具调用、Agent 智能对话、会话管理 |
| **graph** | DashScope Graph 状态图引擎 | 英语学习工作流：输入单词 → AI 造句 → AI 翻译 |
| **tool-vision** | Spring AI, MyBatis Plus, OpenAI Vision | 图像识别、敏感词过滤 |
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

## 项目结构

```
ai-assistant/
├── backend-spring-ai/               # 后端父工程（Maven 多模块）
│   ├── rag-chat/                    # AI 对话核心模块
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
│   ├── media/                       # 文本转语音（TTS）模块
│   │   ├── tts/                     # TTS 服务（DashScope 语音合成）
│   │   └── start/                   # 控制器与启动类
│   ├── graph/                       # 英语学习流程图模块
│   │   ├── node/                    # 图节点（造句、翻译）
│   │   ├── config/                  # 图配置
│   │   └── start/                   # 控制器与启动类
│   ├── tool-vision/                 # 工具调用 + 视觉识别模块（独立工程）
│   │   ├── controller/              # 工具对话 & 视觉识别 API
│   │   ├── service/                 # 套餐工具 & 视觉服务
│   │   ├── springai/                # Spring AI 配置
│   │   └── Interceptor/             # 敏感词拦截器
│   └── pom.xml                      # Maven 父工程配置
├── frontend-vue-ai/                 # Vue 3 前端
│   └── src/
│       ├── views/                   # 页面（tool.vue, vision.vue）
│       ├── api/                     # API 请求封装
│       ├── router/                  # 路由（/tool, /see）
│       └── components/              # 公共组件
└── README.md
```

---

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Java (OpenJDK) | 17 |
| 框架 | Spring Boot | 3.3.8 |
| AI 框架 | Spring AI | 1.1.0 |
| ORM | MyBatis Plus | 3.5.9 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7.0+ |
| 向量存储 | Redis Vector Store | - |
| 大模型 | OpenAI / 阿里云 DashScope | - |
| 工具库 | Hutool | 5.8.36 |
| 前端 | Vue 3 + Element Plus | - |

---

## 核心功能

### 1. 工作流 — graph 模块

基于 **DashScope Graph 状态图引擎** 构建英语学习工作流，以有向图方式编排 AI 节点，实现可追溯的多步骤任务流程。

**英语学习工作流**：

```
用户输入单词 → [Sentence 节点] AI造句 → [Translation 节点] AI翻译 → 返回双语结果
```

- **节点化编排**：每个图节点（Sentence / Translation）独立调用 AI `ChatClient`，职责单一、可复用
- **自动流转**：状态图编译后自动执行节点链，前一节点的输出作为后一节点的上下文
- **可视化调试**：编译时自动生成 PlantUML 图，便于理解和调试工作流拓扑

### 2. RAG + 工具调用 — rag-chat 模块

结合 **RAG 检索增强** 和 **Tool Calling 工具调用**，让 AI 既能从私有知识库获取信息，又能自主查询业务数据，实现精准回答。

**RAG 检索增强**：

- **向量存储**：基于 Redis VectorStore 存储文档向量，支持语义相似度检索
- **自动增强**：`QuestionAnswerAdvisor` 在每次对话中自动从知识库检索相关内容，注入 AI 上下文
- **可配置策略**：相似度阈值 0.6、Top-K 6 条

**工具调用（Tool Calling）**：

- **课程查询工具**：通过 `@Tool` 注解注册，支持按 ID、名称、分类、价格区间、状态等多维度查询
- **AI 自主决策**：AI 模型根据用户问题自动选择合适的工具方法查询数据库
- **结果关联**：`ToolResultHolder` 跟踪工具调用结果与请求 ID 的映射

**Agent 路由引擎**：

用户输入 → 路由智能体分析意图 → 分发到专业智能体处理

- **路由智能体（RouteAgent）**：判断用户意图（知识问答 / 课程推荐 / UI 变更）
- **知识问答智能体（KnowledgeAgent）**：基于 RAG 检索增强回答专业问题
- **课程推荐智能体（RecommendAgent）**：结合工具调用和 RAG，推荐课程信息

**会话管理**：

- **记忆存储**：MySQL 持久化 + Redis 缓存，支持 20 条上下文记忆
- **会话列表**：按时间分组（当天、30天、1年、更早）
- **标题管理**：自动 / 手动更新会话标题

### 3. 视觉识别 — tool-vision 模块

基于 **多模态大模型** 实现图像识别，支持用户上传图片并由 AI 分析识别内容。

- **图片上传识别**：接收上传的图片文件，自动校验尺寸（最大 2048×2048）
- **Base64 嵌入**：将图片转为 `data:image/jpeg;base64,...` URI 嵌入多模态模型请求
- **灵活输入**：支持本地文件路径和 HTTP 上传字节两种方式
- **安全过滤**：`SensitiveWordInterceptor` 对对话接口启用敏感词检测，命中则返回 400

---


---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- OpenAI API Key 或阿里云 DashScope API Key

### 启动后端

```bash
# 1. 配置数据库和 AI 模型密钥
cd backend-spring-ai/rag-chat/src/main/resources
# 编辑 application-dev.properties 和 application.yml

# 2. 编译启动核心对话模块
cd backend-spring-ai/rag-chat
mvn spring-boot:run
cd ../graph    # 英语学习模块
mvn spring-boot:run

cd ../tool-vision  # 工具视觉模块（独立工程）
mvn spring-boot:run
```

### 启动前端

```bash
cd frontend-vue-ai
npm install
npm run dev
```

---

## API 概览

| 模块 | 方法 | 端点 | 说明 |
|------|------|------|------|
| Agent | POST | `/agent` | 智能路由对话（SSE 流式） |
| Chat | POST | `/chat` | 对话 |
| Embedding | POST | `/embedding` | 向量嵌入 |
| Session | GET/POST/DELETE | `/session/**` | 会话管理 |
| Tool | GET | `/tool` | 工具对话 |
| Vision | POST | `/vision` | 图像识别 |
| Graph | POST | `/Enlish` | 英语学习工作流 |
