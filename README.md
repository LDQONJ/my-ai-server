# myai - AI 聊天应用后端服务器

一个基于 Spring Boot 3 + Spring AI + Ollama 的 AI 聊天后端服务，支持用户管理、对话管理、提示词管理、模型切换、SSE 流式输出等功能。
新增对阿里云模型、谷歌模型的适配。新增函数调用功能。

前端项目：[my-ai-chat](https://github.com/LDQONJ/my-ai-chat)

## 功能特性

- 聊天历史记录
- SSE 流式聊天输出
- 自动生成对话标题
- 自动压缩聊天上下文并生成摘要
- 全局/会话级提示词管理
- 模型在线切换
- 适配不同厂商的聊天模型 API
- 工具调用

## 技术栈

- Java 17
- Spring Boot 3.2.5
- Spring AI 1.0.0-M6（Ollama）
- MyBatis-Plus 3.5.6
- MySQL
- Redis
- MongoDB
- Knife4j

## 项目结构

```text
src/main/java/work/daqian/myai
├─ adapter      # 不同模型供应商请求和响应的处理
├─ controller   # API 接口层
├─ service      # 业务层
├─ mapper       # MyBatis-Plus Mapper
├─ domain       # DTO/PO/VO
├─ config       # 配置类
├─ interceptor  # 鉴权拦截器
├─ repository   # MongoDB Repository
├─ tool         # 模型可以调用的本地方法
└─ util         # 工具类
```

## 运行环境

请先准备以下依赖服务：

- JDK 17
- Maven 3.9+
- MySQL 8+
- Redis 6+
- MongoDB 6+
- Ollama 本地模型或者阿里云、谷歌等厂商API

## 快速开始

1. 克隆项目

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
  -d "{\"type\":\"thinking\",\"content\":\"嗯，用户发来了你好...\"}"
```
