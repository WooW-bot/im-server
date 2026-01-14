# Instant Messaging Server (IM-Server)

这是一个基于 Java 开发的高性能分布式即时通讯服务端项目。采用 Netty 实现高性能 TCP 网关，Spring Boot 处理业务逻辑，并使用 Zookeeper、Redis、RabbitMQ 等组件构建高可用架构。

## 📂 项目结构

| 模块名 | 说明 |
| :--- | :--- |
| **tcp** | **长连接网关层**。基于 Netty 实现，负责维护客户端的长连接、消息协议解析 (Packet)、心跳保活以及消息的上下行投递。 |
| **service** | **业务逻辑层**。提供 HTTP API 接口（如登录、用户信息、好友关系等），处理具体的 IM 业务逻辑。 |
| **common** | **公共模块**。包含通用的工具类、常量定义、数据模型 (Model)、枚举 (Enum) 以及统一的错误码定义。 |
| **codec** | **编解码模块**。负责私有协议的序列化与反序列化，支持自定义的消息包结构。 |
| **message-store** | **消息存储模块**。负责消息的持久化存储逻辑（如写入 MySQL/MongoDB 等）。 |
| **assert** | 资源文件或测试断言相关目录。 |

## 🛠 技术栈

- **开发语言**: Java (JDK 1.8+)
- **核心框架**: Spring Boot
- **网络通信**: Netty 4.x
- **服务注册与发现**: Zookeeper
- **消息队列**: RabbitMQ (用于解耦消息投递与业务处理)
- **缓存**: Redis (用户 Session、路由信息缓存)
- **数据库**: MySQL / MyBatis-Plus

## 🚀 核心功能

1.  **用户体系**: 注册、登录、用户信息管理、在线状态维护。
2.  **长连接管理**: 支持 TCP 长连接，自定义私有协议，心跳检测。
3.  **消息路由**: 基于 Zookeeper 和 Redis 实现用户连接的路由查找，支持分布式集群部署。
4.  **单聊/群聊**: 支持点对点消息和群组消息的投递。
5.  **多端登录**: 支持多端互踢或共存策略（依赖 `appId` 和 `clientType`）。

## 🏁 快速开始

### 1. 环境准备
确保本地已安装并启动以下中间件：
- MySQL
- Redis
- Zookeeper
- RabbitMQ

### 2. 配置修改
在 `service` 和 `tcp` 模块的 `src/main/resources` 目录下修改配置文件（`application.yml` 或 `config.yml`），填入正确的中间件地址和账号密码。

### 3. 编译构建
在项目根目录下运行 Maven 命令：
```bash
mvn clean package -DskipTests
```

### 4. 启动服务
建议按以下顺序启动：
1.  **启动 Service 层**: 运行 `service` 模块的主启动类。
2.  **启动 TCP 层**: 运行 `tcp` 模块的主启动类 (Starter)。

## 🔌 接口说明
- **登录接口**: `POST /v1/user/login`
- **获取用户信息**: `POST /v1/user/data/getUserInfo`

## 🤝 贡献与支持
本项目仅供学习与交流使用。如有问题，欢迎提交 Issue。
