# AI Assistant 

## 项目结构

```
backend-spring/
├── aigc/                     # AI 对话核心模块
│   ├── src/main/java/
│   │   ├── common/           # 通用组件
│   │   │   ├── constants/    # 常量定义（Constant, FileErrorInfo）
│   │   │   ├── properties/   # 配置属性类（SessionProperties, SystemMessageProperties）
│   │   │   └── result/       # 返回结果封装（Result）
│   │   ├── mapper/           # MyBatis Plus Mapper 接口
│   │   │   ├── ChatRecordMapper.java   # 聊天记录数据访问
│   │   │   ├── CourseMapper.java       # 课程数据访问
│   │   │   └── SessionMapper.java      # 会话数据访问
│   │   ├── model/            # 数据模型
│   │   │   ├── dto/          # 数据传输对象（ChatDTO）
│   │   │   ├── entity/       # 数据库实体（ChatRecord, Session）
│   │   │   ├── enums/        # 枚举类型（AgentTypeEnum, ChatEventTypeEnum, MessageTypeEnum）
│   │   │   └── vo/           # 视图对象（ChatEventVO, ChatSessionVO, MessageVO, SessionVO, TemplateVO）
│   │   ├── service/          # 服务层
│   │   │   ├── agent/        # Agent 抽象层（AbstractAgent, Agent）
│   │   │   ├── chat/         # 对话服务
│   │   │   │   ├── ChatService/ChatServiceImpl      # 核心对话逻辑
│   │   │   │   ├── RagService/RagServiceImpl        # 检索增强生成
│   │   │   │   └── ToolService/ToolServiceImpl      # 工具调用服务
│   │   │   ├── memory/       # 会话记忆管理
│   │   │   │   ├── mysql/    # MySQL 记忆存储（ChatRecordService, MysqlChatMemoryReposity）
│   │   │   │   ├── redis/    # Redis 记忆存储（RedisChatMemoryReposity）
│   │   │   │   └── AssistantMessageUtil, MessageModel, MessageUtil  # 消息工具类
│   │   │   ├── rag/          # RAG 服务（CreateVector）
│   │   │   ├── tools/        # 工具服务
│   │   │   │   ├── result/   # 课程工具实现（Course, CourseService, CourseServiceImpl）
│   │   │   │   └── CourseInfo, CourseTools, ToolResultHolder  # 工具定义
│   │   │   ├── SessionService.java       # 会话管理服务接口
│   │   │   └── SessionServiceImpl.java   # 会话管理服务实现
│   │   └── start/            # 启动配置
│   │       ├── config/       # 配置类（ChatConfiguration, RagConfiguration, SystemPromptConfig, ToolConfiguration）
│   │       ├── controller/   # REST API 控制器
│   │       │   ├── ChatController.java      # 对话接口
│   │       │   ├── EmbeddingController.java # 向量嵌入接口
│   │       │   ├── SessionController.java   # 会话管理接口
│   │       │   └── ToolController.java      # 工具接口
│   │       ├── metaHandler/  # 元数据处理器（AutoMetaObjectHandler）
│   │       └── AIGCApplication.java         # 启动类
│   └── src/main/resources/   # 配置文件（application.yml, application-dev.properties, session.yml, system-message.txt）
├── audio/                    # 音频服务模块（TTS）
│   └── src/main/java/
│       ├── audio/            # 音频服务（AudioService, AudioServiceImpl）
│       └── start/            # 控制器（TTSController）和启动类（YuApplication）
└── pom.xml                   # Maven 父工程配置
```

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| Java | OpenJDK | 17 |
| 框架 | Spring Boot | 3.2.x |
| ORM | MyBatis Plus | 3.5.9 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7.0+ |
| AI 框架 | Spring AI | 1.0.x |
| 工具库 | Hutool | 5.8.26 |

## 核心功能

### 1. 智能对话服务

**功能描述：**
- **多轮对话支持**：基于 Spring AI 实现的智能对话，支持上下文理解和多轮交互
- **会话记忆管理**：支持 MySQL 和 Redis 两种记忆存储方式，实现对话历史的持久化和快速检索
- **系统消息配置**：支持自定义系统提示词，通过 `system-message.txt` 配置 AI 助手的行为模式

---

### 2. 会话管理

**功能描述：**
- **会话创建**：生成唯一会话 ID，初始化会话状态
- **会话查询**：支持单个会话详情查询和历史会话列表查询
- **会话分组**：历史会话按时间分组（当天、最近30天、最近1年、1年以上）
- **标题更新**：自动或手动更新会话标题
- **会话删除**：支持单个会话删除，清理关联的数据库记录和缓存

---

### 3. 工具调用服务

**功能描述：**
- **课程查询工具**：通过 `CourseTools` 实现课程信息查询功能
- **RAG 检索增强**：基于 Redis 向量存储实现检索增强生成，提升回答准确性
- **动态工具调用**：支持 Agent 根据对话内容自动选择调用合适的工具

**工具列表：**
- `CourseInfo`：课程信息查询工具
- `RAG`：检索增强生成，从知识库中检索相关信息辅助回答

---

### 4. 向量嵌入服务

**功能描述：**
- **文本向量化**：将文本转换为向量表示
- **向量存储**：使用 Redis 作为向量数据库，支持高效的相似度检索
- **知识库管理**：支持将文档内容向量化后存入向量库

---

### 5. 音频服务

**功能描述：**
- **文本转语音（TTS）**：将文本内容转换为语音文件
- **语音输出**：支持 MP3 格式音频输出

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

## 目录职责说明

| 目录 | 职责描述 |
|------|----------|
| `common/` | 通用工具类、常量定义、配置属性类、统一返回结果封装 |
| `mapper/` | MyBatis Plus Mapper 接口，定义数据库访问方法 |
| `model/dto/` | 数据传输对象，用于接收和传递请求参数 |
| `model/entity/` | 数据库实体类，与数据库表结构对应 |
| `model/enums/` | 枚举类型，定义业务常量 |
| `model/vo/` | 视图对象，用于封装响应数据 |
| `service/` | 业务逻辑层，实现核心业务功能 |
| `service/agent/` | Agent 抽象层，定义智能代理行为 |
| `service/chat/` | 对话服务，处理消息发送和响应 |
| `service/memory/` | 会话记忆管理，支持多种存储方式 |
| `service/tools/` | 工具服务，提供外部工具调用能力 |
| `start/config/` | Spring 配置类，初始化 Bean 和第三方服务 |
| `start/controller/` | REST API 控制器，处理 HTTP 请求 |

## 数据模型

### 核心实体

**ChatRecord（聊天记录）**：
- `id`：主键 ID
- `conversation_id`：会话 ID
- `title`：会话标题
- `data`：消息数据（JSON 格式）
- `create_time`：创建时间
- `update_time`：更新时间

**Session（会话）**：
- `id`：主键 ID
- `title`：会话标题
- `create_time`：创建时间
- `update_time`：更新时间

### 视图对象

**MessageVO（消息视图）**：
- `id`：消息 ID
- `role`：角色（user/assistant/system）
- `content`：消息内容
- `timestamp`：时间戳

**ChatSessionVO（会话视图）**：
- `sessionId`：会话 ID
- `title`：会话标题
- `updateTime`：更新时间

**SessionVO（会话视图）**：
- `sessionId`：会话 ID
- `title`：会话标题

## 许可证

MIT License