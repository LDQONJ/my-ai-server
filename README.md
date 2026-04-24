# myai - AI 聊天应用后端服务器

一个基于 Spring Boot 3 + Spring AI + Ollama 的 AI 聊天后端服务，支持用户管理、对话管理、提示词管理、模型切换、SSE 流式输出等功能。

前端项目：[my-ai-chat](https://github.com/LDQONJ/my-ai-chat)

## 功能特性

- 聊天历史记录
- SSE 流式聊天输出
- 自动生成对话标题
- 自动压缩聊天上下文并生成摘要
- 全局/会话级提示词管理
- 模型在线切换

## 技术栈

- Java 17
- Spring Boot 3.2.5
- Spring AI 1.0.0-M6（Ollama）
- MyBatis-Plus 3.5.6
- MySQL
- Redis
- MongoDB
- Knife4j (OpenAPI 3)

## 项目结构

```text
src/main/java/work/daqian/myai
├─ controller   # API 接口层
├─ service      # 业务层
├─ mapper       # MyBatis-Plus Mapper
├─ domain       # DTO/PO/VO
├─ config       # 配置类
├─ interceptor  # 鉴权拦截器
├─ repository   # MongoDB Repository
└─ util         # 工具类
```

## 运行环境

请先准备以下依赖服务：

- JDK 17
- Maven 3.9+
- MySQL 8+
- Redis 6+
- MongoDB 6+
- Ollama（并已拉取可用模型）

## 快速开始

1. 克隆项目

```bash
git clone <your-repo-url>
cd myai
```

2. 配置密钥与连接信息  
配置 `src/main/resources/application-secret.yaml` 。

3. 启动应用

默认端口为 `5000`，SSL 没有可以关闭。

4. 打开接口文档

- [https://localhost:5000/doc.html](https://localhost:5000/doc.html)

## 主要接口

> 返回结构统一为 `R<T>`：`code`、`msg`、`data`、`requestId`。

- `POST /users/register` 用户注册
- `POST /users/login` 用户登录
- `GET /users/me` 当前用户信息
- `GET /verifyCode?target=...` 发送验证码
- `POST /chat` 流式聊天（`text/event-stream`）
- `GET /session/create` 创建会话
- `GET /session/list` 会话列表
- `PUT /session?id=...&title=...` 重命名会话
- `DELETE /session/{id}` 删除会话
- `GET /prompt` 查询全局提示词
- `POST /prompt` 设置全局提示词
- `GET /model/list` 模型列表
- `GET /model/change?id=...` 切换模型
- `POST /files` 上传文件
- `GET /files/{fileName}` 下载文件

## SSE 调用示例

```bash
curl -N -X POST "https://localhost:5000/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: <your_access_token>" \
  -d "{\"sessionId\":\"<session_id>\",\"message\":\"你好\"}"
```

## 打包与测试

```bash
mvn clean test
mvn clean package
```
