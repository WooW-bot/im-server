# 登录接口测试指南

本指南提供了如何使用 `curl` 命令测试 `im-service` 模块中的登录接口 (`/v1/user/login`)。

## 1. 确保应用正在运行

在执行 `curl` 命令之前，请确保 `im-service` 应用已经成功构建并正在运行。

1.  **构建项目**: 在项目根目录 (`/Users/by/2026/im-server/`) 执行 Maven 命令：
    ```bash
    mvn clean install -DskipTests
    ```
    (如果你想运行测试，可以省略 `-DskipTests`)

2.  **运行应用**: 构建成功后，在 `im-service` 模块的 `target` 目录下会生成一个可执行的 JAR 文件。运行它：
    ```bash
    java -jar service/target/im-service-*.jar
    ```
    (请将 `im-service-*.jar` 替换为实际生成的 JAR 文件名，通常会包含版本号，例如 `im-service-0.0.1-SNAPSHOT.jar`)

确保应用启动后，你可以在控制台看到类似 "Started Application in X.XXX seconds" 的日志信息。

## 2. 使用 cURL 测试登录接口

当应用在默认端口 `8080` 成功启动后，你可以使用以下 `curl` 命令来测试登录接口。

```bash
curl -X POST \
  "http://localhost:8080/v1/user/login" \
  -H 'Content-Type: application/json' \
  -d '{
    "appId": 10000,
    "userId": "testUser",
    "clientType": 1
  }'
```

### 命令说明

*   `-X POST`: 指定 HTTP 请求方法为 POST。
*   `http://localhost:8080/v1/user/login?appId=1`: 登录接口的完整 URL。
    *   `localhost:8080`: 应用运行的主机和端口。
    *   `/v1/user/login`: 接口路径。
    *   `?appId=1`: URL 查询参数，`appId` 的值为 `1`。你可以根据实际情况修改此值。
*   `-H 'Content-Type: application/json'`: 设置请求头，告知服务器请求体是 JSON 格式。
*   `-d '{ "userId": "testUser", "clientType": 1 }'`: HTTP 请求体，一个 JSON 字符串。
    *   `userId`: 用户ID，这里使用 `testUser` 作为示例。
    *   `clientType`: 客户端类型，这里使用 `1` 作为示例。

### 预期响应

由于目前 `ImUserServiceImpl.java` 中的 `login` 方法尚未实现具体的业务逻辑，它会直接返回一个成功的响应。你执行上述 `curl` 命令后，应该会收到类似以下内容：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 注意事项

**当前 `ImUserServiceImpl.java` 的 `login` 方法仅返回成功，不包含实际的用户验证、数据库查询等登录逻辑。**

为了实现一个完整的登录功能，你需要在 `service/src/main/java/com/pd/im/service/user/service/impl/ImUserServiceImpl.java` 文件中补充具体的业务逻辑，例如：

*   查询用户数据库（需要先实现 DAO 层）。
*   验证用户ID和密码（如果 `LoginReq` 中包含密码）。
*   生成并返回会话 token 等。
```