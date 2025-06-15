# YunClass 配置管理系统

本项目实现了一个集中式的配置管理系统，使得修改服务器和客户端的配置只需要在一个地方进行。

## 配置文件结构

主要配置文件位于 `html/config.js`，包含以下部分：

- `BaseConfig`: 基础配置，包含服务器主机和端口信息
- `ClientConfig`: 客户端配置，继承自基础配置
- `ServerConfig`: 服务器配置，包含数据库、会话和CORS设置

## 如何修改配置

1. 打开 `html/config.js` 文件
2. 修改 `HOST` 和 `PORT` 等配置项
3. 运行更新命令，自动同步到Android配置文件

## 自动更新Android配置

当修改了 `html/config.js` 中的配置后，可以运行以下命令自动更新Android配置文件：

```bash
cd html
npm run update-config
```

这将自动更新以下文件：
- `app/src/main/java/com/example/yunclass/config/AppConfig.java`
- `app/src/main/res/xml/network_security_config.xml`

## 高级功能

更新脚本还提供以下高级功能：

1. **JSON文件URL更新**：自动检查并更新JSON文件中的URL，如果它们包含旧的IP地址
2. **硬编码IP检测**：扫描项目中所有文件，查找可能包含硬编码IP地址的文件，并提供报告
3. **跨平台支持**：在Windows、Linux和macOS上都能正常工作

## 配置项说明

- `HOST`: 服务器主机地址（IP或域名）
- `PORT`: 服务器端口号
- `DB`: 数据库配置
- `SESSION`: 会话配置
- `CORS`: 跨域资源共享配置

## 注意事项

- 修改配置后，需要重启服务器和重新编译Android应用才能生效
- 确保网络安全配置中的域名与服务器主机地址一致
- `html/config.js` 是统一配置文件，同时支持服务器端和客户端使用
- 如果脚本报告发现硬编码IP地址，请手动检查并更新这些文件 

# 云课堂 YunClass

云课堂是一个Android移动学习平台，支持PDF和视频课程内容。

## 项目结构

- `app/` - Android应用源代码
- `html/book/` - 课程内容文件（PDF和视频）
- `server.js` - 简易后端服务器

## 服务器设置

### 安装依赖

```bash
npm install
```

### 启动服务器

```bash
npm start
```

或者使用开发模式（自动重启）：

```bash
npm run dev
```

服务器将在 http://localhost:3000 上运行。

## API端点

- `GET /api/courses` - 获取所有课程
- `GET /api/courses/:id` - 获取单个课程详情
- `POST /api/purchase` - 购买课程

## 文件访问

- PDF文件: `http://localhost:3000/html/book/python.pdf`
- 视频文件: `http://localhost:3000/html/book/堆.mp4`

## Android应用

Android应用通过API与服务器通信，可以：

1. 浏览课程列表
2. 购买课程
3. 查看已购买的课程
4. 阅读PDF课程内容
5. 观看视频课程内容

## 配置

在应用中修改服务器地址：

1. 打开 `app/src/main/java/com/example/yunclass/config/AppConfig.java`
2. 修改 `HOST` 和 `PORT` 变量为您的服务器地址和端口 