# AI Assistant 项目

一个基于 Spring Boot 的 AI 智能助理后端服务，提供智能对话、会话管理、工具调用等功能。

## 项目结构

```
backend-spring/
├── aigc/                     # AI 对话核心模块
│   ├── src/main/java/
│   │   ├── common/           # 通用组件
│   │   │   ├── constants/    # 常量定义
│   │   │   ├── properties/   # 配置属性类
│   │   │   └── result/       # 返回结果封装
│   │   ├── mapper/           # MyBatis Mapper 接口
│   │   ├── model/            # 数据模型
│   │   │   ├── dto/          # 数据传输对象
│   │   │   ├── entity/       # 数据库实体
│   │   │   ├── enums/        # 枚举类型
│   │   │   └── vo/           # 视图对象
│   │   ├── service/          # 服务层
│   │   │   ├── agent/        # Agent 抽象层
│   │   │   ├── chat/         # 对话服务
│   │   │   ├── memory/       # 会话记忆管理
│   │   │   │   ├── mysql/    # MySQL 记忆存储
│   │   │   │   └── redis/    # Redis 记忆存储
│   │   │   ├── session/      # 会话管理服务
│   │   │   └── tools/        # 工具服务
│   │   └── start/            # 启动配置
│   │       ├── config/       # 配置类
│   │       ├── controller/   # REST API 控制器
│   │       └── metaHandler/  # 元数据处理器
│   └── src/main/resources/   # 配置文件
├── audio/                    # 音频服务模块（TTS）
└── pom.xml                   # Maven 父工程配置
```

## 技术栈

- **Java**: 21
- **框架**: Spring Boot 3.2.x
- **ORM**: MyBatis Plus
- **数据库**: MySQL + Redis
- **AI 框架**: Spring AI
- **工具库**: Hutool（工具类）

## 核心功能

### 1. 智能对话服务
- 支持多轮对话
- 上下文记忆管理
- 系统消息配置

### 2. 会话管理
- 会话创建与销毁
- 会话历史查询
- 会话标题更新

### 3. 工具调用
- 课程查询工具
- RAG（检索增强生成）

### 4. 音频服务
- 文本转语音（TTS）

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

### 配置说明

1. 配置数据库连接：`aigc/src/main/resources/application.yml`
2. 配置 Redis 连接：`aigc/src/main/resources/application.yml`
3. 配置 AI 服务：`aigc/src/main/resources/application.yml`

### 启动方式

```bash
cd backend-spring
mvn spring-boot:run -pl aigc
```

## API 接口

### 会话管理

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/session` | POST | 创建会话 |
| `/api/session/{sessionId}` | GET | 查询会话详情 |
| `/api/session/title` | PUT | 更新会话标题 |
| `/api/session/history` | GET | 查询历史会话列表 |
| `/api/session/{sessionId}` | DELETE | 删除会话 |

### 对话服务

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/chat` | POST | 发送消息 |

## 目录职责说明

| 目录 | 职责 |
|------|------|
| `common/` | 通用工具类、常量、配置属性 |
| `mapper/` | MyBatis Plus Mapper 接口 |
| `model/` | 数据模型（DTO、Entity、Enum、VO） |
| `service/` | 业务逻辑层 |
| `start/` | Spring Boot 启动配置和控制器 |

## 许可证

MIT License