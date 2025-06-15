# 服务器合并说明


## 合并内容

### 从根目录 server.js 合并的功能：
1. **静态文件服务**：`app.use('/html', express.static(path.join(__dirname)))`
2. **PDF/视频文件路由**：`/html/book/:filename` 路由处理
3. **课程API**：
   - `GET /api/courses` - 获取课程列表
   - `GET /api/courses/:id` - 获取单个课程详情
   - `GET /api/courses/:id/subcourses` - 获取子课程列表

### html/server.js 原有功能保持不变：
1. **用户认证系统**：注册、登录、登出
2. **数据库连接**：MySQL 数据库支持
3. **问答系统**：问题提交和回复
4. **账户系统**：用户账户和订单管理
5. **文件上传**：图片上传功能

## 启动方式
```bash
cd html
node server.js
```

## API 端点
- 用户相关：`/api/register`, `/api/login`, `/api/logout`
- 课程相关：`/api/courses`, `/api/courses/:id`, `/api/courses/:id/subcourses`
- 文件服务：`/html/book/python.pdf`, `/html/book/dui.mp4`
- 问答系统：`/api/questions`, `/api/questions/:id/replies`

## 测试文件
- PDF文件：`http://192.168.43.230:3000/html/book/python.pdf`
- 视频文件：`http://192.168.43.230:3000/html/book/dui.mp4`

## 注意事项
- 根目录的 `server.js` 已被删除
- 所有功能现在统一在 `html/server.js` 中
- Android 应用连接的服务器地址保持不变：`192.168.43.230:3000` 