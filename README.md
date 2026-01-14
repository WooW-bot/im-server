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
- **核心框架**: Spring Boot 2.3.2
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
- **MySQL** (数据库 `im_core`)
- **Redis** (默认端口 6379)
- **Zookeeper** (默认端口 2181)
- **RabbitMQ** (默认端口 5672，需要创建 `admin/admin` 用户)

### 2. 数据库初始化

执行 SQL 脚本创建数据库和表：
```bash
mysql -u root -p < assert/sql/im_core.sql
```

### 3. 配置修改

主要配置文件：
- `service/src/main/resources/application.properties` - Service 模块配置
- `tcp/src/main/resources/config.yml` - TCP 模块配置

确保以下配置正确：
- MySQL 连接信息 (默认: `root/12345678`)
- Redis 地址 (默认: `127.0.0.1:6379`)
- RabbitMQ 用户 (默认: `admin/admin`)
- Zookeeper 地址 (默认: `127.0.0.1:2181`)

### 4. 启动服务

#### 方式一：使用管理脚本 (推荐) ⭐

项目提供了完整的管理脚本（位于 `scripts/` 目录），可以一键启动、停止和管理服务。

```bash
# 启动所有服务
./im-server.sh start

# 查看服务状态
./im-server.sh status

# 查看实时日志
./im-server.sh logs

# 重启服务
./im-server.sh restart

# 停止服务
./im-server.sh stop
```

**模块化启动**：
```bash
# 仅启动 Service 模块
./im-server.sh start service

# 仅启动 Message-Store 模块
./im-server.sh start message-store

# 仅启动 TCP 模块
./im-server.sh start tcp
```

**查看帮助**：
```bash
./im-server.sh --help
```

#### 方式二：使用独立脚本

```bash
# 启动服务
./scripts/start.sh

# 查看状态
./scripts/status.sh

# 停止服务
./scripts/stop.sh
```

#### 方式三：手动启动

**编译构建**：
```bash
mvn clean package -DskipTests
```

**启动 Service 层**：
```bash
cd service
mvn spring-boot:run
```

**启动 TCP 层**：
```bash
cd tcp
mvn spring-boot:run
```

## 📜 管理脚本说明

所有管理脚本位于 `scripts/` 目录中，同时在项目根目录提供了便捷的快捷方式。

| 脚本 | 位置 | 说明 |
|------|------|------|
| `im-server.sh` | 根目录/scripts/ | 统一管理脚本，提供 start/stop/restart/status/logs 功能 |
| `start.sh` | scripts/ | 启动脚本，自动检查中间件并按顺序启动服务 |
| `stop.sh` | scripts/ | 停止脚本，支持优雅停止和强制清理 |
| `status.sh` | scripts/ | 状态检查脚本，显示所有服务运行状态 |

**脚本特性**：
- ✅ 自动检查中间件服务状态
- ✅ 按正确顺序启动模块 (Service → Message-Store → TCP)
- ✅ 等待服务完全启动并验证端口
- ✅ 支持优雅停止和强制停止
- ✅ 彩色输出，清晰易读
- ✅ 自动管理日志和 PID 文件

**日志和 PID 文件**：
- 日志目录: `logs/` (自动创建)
  - `service.log` - Service 模块日志
  - `message-store.log` - Message-Store 模块日志
  - `tcp.log` - TCP 模块日志
- PID 目录: `pids/` (自动创建)
  - `service.pid` - Service 进程 PID
  - `message-store.pid` - Message-Store 进程 PID
  - `tcp.pid` - TCP 进程 PID

## 🔌 服务端口

启动成功后，服务监听以下端口：

| 服务 | 端口 | 说明 |
|------|------|------|
| **Service API** | 8000 | HTTP API 接口 |
| **TCP 网关** | 19001 | TCP 长连接 |
| **WebSocket** | 19002 | WebSocket 连接 |

## 🔌 接口说明

### 主要 API 接口

- **登录接口**: `POST http://localhost:8000/v1/user/login`
- **获取用户信息**: `POST http://localhost:8000/v1/user/data/getUserInfo`

### 测试数据

数据库已包含测试用户：
- 用户 ID: `10001`, `10002`, `10003`, `bantanger`, `admin`
- 测试群组: `27a35ff2f9be4cc9a8d3db1ad3322804`

## 🛠 常见问题

### 启动失败

1. **检查中间件服务**：
   ```bash
   ./scripts/status.sh
   ```

2. **查看详细日志**：
   ```bash
   ./im-server.sh logs
   ```

3. **检查端口占用**：
   ```bash
   lsof -i:8000
   lsof -i:19001
   lsof -i:19002
   ```

### 停止失败

使用强制清理模式：
```bash
./scripts/stop.sh cleanup
```

### RabbitMQ 用户配置

如果 RabbitMQ 没有 `admin/admin` 用户，执行：
```bash
rabbitmqctl add_user admin admin
rabbitmqctl set_user_tags admin administrator
rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```

## 🤝 贡献与支持

本项目仅供学习与交流使用。如有问题，欢迎提交 Issue。
