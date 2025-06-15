const express = require('express');
const bodyParser = require('body-parser');
const bcrypt = require('bcryptjs');
const cors = require('cors');
const path = require('path');
const session = require('express-session');
const fs = require('fs');
const multer = require('multer');

// 导入服务器配置
const config = require('./config');

// 配置文件上传存储
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    const uploadDir = path.join(__dirname, 'uploads');
    // 确保上传目录存在
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: function (req, file, cb) {
    // 生成唯一文件名
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, uniqueSuffix + path.extname(file.originalname));
  }
});

// 创建multer实例
const upload = multer({ 
  storage: storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 限制5MB
  fileFilter: function (req, file, cb) {
    // 只接受图片文件
    if (file.mimetype.startsWith('image/')) {
      cb(null, true);
    } else {
      cb(new Error('只允许上传图片文件！'), false);
    }
  }
});

const app = express();

// 中间件设置
app.use(cors({
  origin: config.CORS.ORIGINS,
  credentials: config.CORS.CREDENTIALS
}));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname)));

// 添加session中间件
app.use(session({
  secret: config.SESSION.SECRET,
  resave: false,
  saveUninitialized: true,
  cookie: { 
    secure: false, // 在生产环境中应设置为true
    maxAge: config.SESSION.MAX_AGE
  }
}));

// 配置静态文件服务
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// 添加静态文件服务 - 从根目录server.js合并
app.use('/html', express.static(path.join(__dirname)));

// 添加特定路由处理PDF和视频文件 - 从根目录server.js合并
app.get('/html/book/:filename', (req, res) => {
  const filename = req.params.filename;
  const filePath = path.join(__dirname, 'book', filename);
  
  // 检查文件是否存在
  if (fs.existsSync(filePath)) {
    // 设置正确的MIME类型
    if (filename.endsWith('.pdf')) {
      res.setHeader('Content-Type', 'application/pdf');
    } else if (filename.endsWith('.mp4')) {
      res.setHeader('Content-Type', 'video/mp4');
    }
    
    // 发送文件
    res.sendFile(filePath);
  } else {
    res.status(404).send('File not found: ' + filename);
  }
});

// 添加Express JSON设置，确保日期正确格式化
app.set('json replacer', (key, value) => {
  // 如果值是Date类型，确保返回ISO格式的日期字符串
  if (value instanceof Date) {
    return value.toISOString();
  }
  return value;
});

// 增加中间件，在发送响应前确保所有Date类型都转换为ISO格式字符串
app.use((req, res, next) => {
  const originalJson = res.json;
  res.json = function(obj) {
    if (obj) {
      // 递归处理对象中的所有Date类型
      const processObject = (data) => {
        if (!data) return data;
        
        if (Array.isArray(data)) {
          return data.map(item => processObject(item));
        }
        
        if (typeof data === 'object') {
          Object.keys(data).forEach(key => {
            const value = data[key];
            if (value instanceof Date) {
              // 确保日期以ISO格式发送
              data[key] = value.toISOString();
            } else if (typeof value === 'object' && value !== null) {
              data[key] = processObject(value);
            }
          });
        }
        return data;
      };
      
      obj = processObject(obj);
    }
    return originalJson.call(this, obj);
  };
  next();
});

console.log('服务器初始化中...');
console.log('正在连接到MySQL数据库...');

// 数据库连接配置
const mysql = require('mysql2');

// 尝试连接数据库，如果失败则使用内存存储
let db;
let useMemoryStorage = false;
const users = []; // 内存存储备用

// 数据库配置
const dbConfig = {
  host: config.DB.HOST,
  user: config.DB.USER,
  password: config.DB.PASSWORD,
  port: config.DB.PORT,
  connectTimeout: config.DB.CONNECT_TIMEOUT,
  waitForConnections: true,
  queueLimit: 0
};

console.log('数据库配置:', JSON.stringify(dbConfig, null, 2));

// 定义连接尝试函数
async function tryConnect(config, configName) {
  return new Promise((resolve) => {
    try {
      console.log(`尝试使用${configName}配置连接到MySQL数据库...`);
      console.log('连接配置:', JSON.stringify(config, null, 2));
      
      const connection = mysql.createConnection(config);
      
      // 设置连接超时
      const timeout = setTimeout(() => {
        console.error(`${configName}连接超时`);
        connection.destroy();
        resolve(null);
      }, config.connectTimeout || 10000);
      
      connection.connect(err => {
        clearTimeout(timeout);
        
        if (err) {
          console.error(`${configName}连接失败:`, err);
          console.error('错误代码:', err.code);
          console.error('错误消息:', err.message);
          resolve(null);
        } else {
          console.log(`使用${configName}连接成功!`);
          resolve(connection);
        }
      });
      
      // 添加错误处理
      connection.on('error', (err) => {
        console.error(`${configName}连接错误:`, err);
        if (err.code === 'PROTOCOL_CONNECTION_LOST') {
          console.error('数据库连接丢失，将切换到内存存储');
          setupMemoryStorage();
        }
      });
    } catch (error) {
      console.error(`创建${configName}连接对象失败:`, error);
      resolve(null);
    }
  });
}

// 连接到数据库
async function connectToDatabase() {
  console.log('尝试连接到MySQL数据库...');
  
  // 尝试连接到本地数据库
  let connection = await tryConnect(dbConfig, 'localhost');
  if (connection) {
    console.log('成功连接到数据库');
    db = connection;
    setupDatabase(db);
    return;
  }
  
  // 连接失败，使用内存存储
  console.error('数据库连接失败，将切换到内存存储模式');
  setupMemoryStorage();
}

try {
  // 开始连接数据库
  connectToDatabase();
} catch (error) {
  console.error('数据库连接过程中发生错误:', error);
  setupMemoryStorage();
}

// 设置内存存储
function setupMemoryStorage() {
  console.log('将使用内存存储作为备用方案');
  useMemoryStorage = true;
  
  // 创建内存存储的API处理函数
  console.log('内存存储初始化完成');
  
  // 添加一些示例用户数据（可选）
  if (users.length === 0) {
    console.log('添加示例用户数据到内存存储');
    // 添加一个示例用户
    const addDemoUser = async () => {
      try {
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash('123456', salt);
        
        users.push({
          id: 1,
          name: '测试用户',
          phone: '13800138000',
          email: 'test@example.com',
          password: hashedPassword,
          created_at: new Date()
        });
        
        console.log('已添加示例用户: test@example.com (密码: 123456)');
      } catch (error) {
        console.error('添加示例用户失败:', error);
      }
    };
    
    addDemoUser();
  }
  
  // 通知用户当前使用的是内存存储模式
  console.log('警告: 当前使用内存存储模式，所有数据将在服务器重启后丢失');
}

// 设置数据库和表
function setupDatabase(connection) {
  // 首先确保数据库存在
  const createDatabaseQuery = `CREATE DATABASE IF NOT EXISTS gadget_db`;
  
  connection.query(createDatabaseQuery, (err, result) => {
    if (err) {
      console.error('创建数据库失败:', err);
      setupMemoryStorage();
      return;
    }
    
    console.log('数据库检查/创建成功');
    
    // 使用数据库
    connection.query('USE gadget_db', (err) => {
      if (err) {
        console.error('切换到数据库失败:', err);
        setupMemoryStorage();
        return;
      }
      
      // 确保用户表存在
      const createUserTableQuery = `
        CREATE TABLE IF NOT EXISTS users (
          id INT AUTO_INCREMENT PRIMARY KEY,
          name VARCHAR(100) NOT NULL,
          phone VARCHAR(20) NOT NULL,
          email VARCHAR(100) UNIQUE NOT NULL,
          password VARCHAR(255) NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
      `;
      
      connection.query(createUserTableQuery, (err, result) => {
        if (err) {
          console.error('创建用户表失败:', err);
          setupMemoryStorage();
        } else {
          console.log('用户表检查/创建成功');
        }
      });
      
      // 创建账户表
      const createAccountTableQuery = `
        CREATE TABLE IF NOT EXISTS accounts (
          id INT AUTO_INCREMENT PRIMARY KEY,
          user_id INT NOT NULL,
          balance DECIMAL(10, 2) DEFAULT 1000.00,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (user_id) REFERENCES users(id)
        )
      `;
      
      connection.query(createAccountTableQuery, (err, result) => {
        if (err) {
          console.error('创建账户表失败:', err);
        } else {
          console.log('账户表检查/创建成功');
        }
      });
      
      // 创建订单表
      const createOrdersTableQuery = `
        CREATE TABLE IF NOT EXISTS orders (
          id INT AUTO_INCREMENT PRIMARY KEY,
          user_id INT NOT NULL,
          course_id INT NOT NULL,
          course_title VARCHAR(255) NOT NULL,
          price DECIMAL(10, 2) NOT NULL,
          status ENUM('pending', 'completed', 'cancelled') DEFAULT 'completed',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          FOREIGN KEY (user_id) REFERENCES users(id)
        )
      `;
      
      connection.query(createOrdersTableQuery, (err, result) => {
        if (err) {
          console.error('创建订单表失败:', err);
        } else {
          console.log('订单表检查/创建成功');
        }
      });
      
      // 创建答疑表
      const createQuestionsTableQuery = `
        CREATE TABLE IF NOT EXISTS questions (
          id INT AUTO_INCREMENT PRIMARY KEY,
          user_id INT NOT NULL,
          title VARCHAR(255) NOT NULL,
          content TEXT NOT NULL,
          image_path VARCHAR(255),
          status ENUM('pending', 'answered') DEFAULT 'pending',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          FOREIGN KEY (user_id) REFERENCES users(id)
        )
      `;
      
      connection.query(createQuestionsTableQuery, (err, result) => {
        if (err) {
          console.error('创建答疑表失败:', err);
        } else {
          console.log('答疑表检查/创建成功');
        }
      });
      
      // 创建回复表
      const createRepliesTableQuery = `
        CREATE TABLE IF NOT EXISTS replies (
          id INT AUTO_INCREMENT PRIMARY KEY,
          question_id INT NOT NULL,
          user_id INT NOT NULL,
          content TEXT NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          FOREIGN KEY (question_id) REFERENCES questions(id),
          FOREIGN KEY (user_id) REFERENCES users(id)
        )
      `;
      
      connection.query(createRepliesTableQuery, (err, result) => {
        if (err) {
          console.error('创建回复表失败:', err);
        } else {
          console.log('回复表检查/创建成功');
        }
      });
    });
  });
}

// 注册API端点
app.post('/api/register', async (req, res) => {
  const { Name, Phone, email, password } = req.body;
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    try {
      // 检查邮箱是否已存在
      const existingUser = users.find(user => user.email === email);
      if (existingUser) {
        return res.status(400).json({ success: false, message: '该邮箱已被注册' });
      }
      
      // 密码加密
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(password, salt);
      
      // 创建新用户并存储到内存
      const newUser = {
        id: users.length + 1,
        name: Name,
        phone: Phone,
        email: email,
        password: hashedPassword,
        created_at: new Date()
      };
      
      users.push(newUser);
      console.log('新用户注册成功(内存存储):', email, '用户ID:', newUser.id);
      return res.status(201).json({ success: true, message: '注册成功！' });
    } catch (error) {
      console.error('内存存储注册错误:', error);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
  }
  
  // 使用数据库存储
  try {
    // 检查邮箱是否已存在
    const checkEmailQuery = 'SELECT * FROM users WHERE email = ?';
    db.query(checkEmailQuery, [email], async (err, results) => {
      if (err) {
        console.error('查询数据库失败:', err);
        return res.status(500).json({ success: false, message: '服务器错误' });
      }
      
      if (results.length > 0) {
        return res.status(400).json({ success: false, message: '该邮箱已被注册' });
      }
      
      // 密码加密
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(password, salt);
      
      // 创建新用户并存储到数据库
      const insertUserQuery = 'INSERT INTO users (name, phone, email, password) VALUES (?, ?, ?, ?)';
      db.query(insertUserQuery, [Name, Phone, email, hashedPassword], (err, result) => {
        if (err) {
          console.error('插入用户数据失败:', err);
          return res.status(500).json({ success: false, message: '服务器错误' });
        }
        
        const userId = result.insertId;
        
        // 为新用户创建账户
        const createAccountQuery = 'INSERT INTO accounts (user_id, balance) VALUES (?, 1000.00)';
        db.query(createAccountQuery, [userId], (err, accountResult) => {
          if (err) {
            console.error('创建用户账户失败:', err);
            // 继续注册流程，即使账户创建失败
          } else {
            console.log('用户账户创建成功, 用户ID:', userId);
          }
          
          console.log('新用户注册成功(数据库):', email, '用户ID:', userId);
        res.status(201).json({ success: true, message: '注册成功！' });
        });
      });
    });
  } catch (error) {
    console.error('服务器错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// 登录API端点
app.post('/api/login', async (req, res) => {
  const { email, password } = req.body;
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    try {
      // 查找用户
      const user = users.find(user => user.email === email);
      if (!user) {
        return res.status(401).json({ success: false, message: '邮箱或密码不正确' });
      }
      
      // 验证密码
      const isMatch = await bcrypt.compare(password, user.password);
      if (!isMatch) {
        return res.status(401).json({ success: false, message: '邮箱或密码不正确' });
      }
      
      // 登录成功，将用户信息存储在session中
      req.session.user = {
        id: user.id,
        name: user.name,
        email: user.email
      };
      
      console.log('用户登录成功(内存存储):', user.email);
      return res.status(200).json({ 
        success: true, 
        message: '登录成功！',
        user: {
          id: user.id,
          name: user.name,
          email: user.email
        }
      });
    } catch (error) {
      console.error('内存存储登录错误:', error);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
  }
  
  // 使用数据库存储
  try {
    // 查找用户
    const findUserQuery = 'SELECT * FROM users WHERE email = ?';
    db.query(findUserQuery, [email], async (err, results) => {
      if (err) {
        console.error('查询数据库失败:', err);
        return res.status(500).json({ success: false, message: '服务器错误' });
      }
      
      if (results.length === 0) {
        return res.status(401).json({ success: false, message: '邮箱或密码不正确' });
      }
      
      const user = results[0];
      
      // 验证密码
      const isMatch = await bcrypt.compare(password, user.password);
      
      if (!isMatch) {
        return res.status(401).json({ success: false, message: '邮箱或密码不正确' });
      }
      
      // 登录成功，将用户信息存储在session中
      req.session.user = {
        id: user.id,
        name: user.name,
        email: user.email
      };
      
      // 登录成功
      console.log('用户登录成功(数据库):', user.email);
      res.status(200).json({ 
        success: true, 
        message: '登录成功！',
        user: {
          id: user.id,
          name: user.name,
          email: user.email
        }
      });
    });
  } catch (error) {
    console.error('登录错误:', error);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// 获取所有用户（仅用于演示）
app.get('/api/users', (req, res) => {
  // 如果使用内存存储
  if (useMemoryStorage) {
    try {
      // 返回内存中的用户列表（不包含密码）
      const userList = users.map(user => ({
        id: user.id,
        name: user.name,
        email: user.email,
        phone: user.phone
      }));
      return res.status(200).json(userList);
    } catch (error) {
      console.error('获取内存用户列表失败:', error);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
  }
  
  // 使用数据库存储
  const getUsersQuery = 'SELECT id, name, email, phone FROM users';
  db.query(getUsersQuery, (err, results) => {
    if (err) {
      console.error('获取用户列表失败:', err);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
    
    res.status(200).json(results);
  });
});

// 检查用户登录状态
app.get('/api/check-auth', (req, res) => {
  if (req.session.user) {
    return res.status(200).json({
      isLoggedIn: true,
      user: req.session.user
    });
  } else {
    return res.status(200).json({
      isLoggedIn: false
    });
  }
});

// 用户注销
app.post('/api/logout', (req, res) => {
  if (req.session.user) {
    // 清除session中的用户信息
    req.session.destroy((err) => {
      if (err) {
        console.error('注销时发生错误:', err);
        return res.status(500).json({ success: false, message: '注销失败' });
      }
      
      res.clearCookie('connect.sid');
      return res.status(200).json({ success: true, message: '注销成功' });
    });
  } else {
    return res.status(200).json({ success: true, message: '用户未登录' });
  }
});

// 新增API端点 - 获取课程列表 (合并自根目录server.js)
app.get('/api/courses', (req, res) => {
  try {
    // 从courses.json文件读取课程数据
    const coursesData = fs.readFileSync(path.join(__dirname, 'doc', 'courses.json'), 'utf8');
    const coursesFromFile = JSON.parse(coursesData);
    
    // 为每个课程添加contentType和contentPath
    const courses = coursesFromFile.map(course => {
      let contentType, contentPath;
      
      // 根据课程ID或标题决定内容类型和路径
      if (course.id === 1 || course.title.includes("Java")) {
        contentType = "video";
        contentPath = "html/book/dui.mp4";
      } else if (course.id === 2 || course.title.includes("Android")) {
        contentType = "video";
        contentPath = "html/book/dui.mp4";
      } else if (course.id === 3 || course.title.includes("数据结构")) {
        contentType = "video";
        contentPath = "html/book/dui.mp4";
      } else if (course.id === 4 || course.title.includes("Web")) {
        contentType = "pdf";
        contentPath = "html/book/python.pdf";
      } else if (course.id === 5 || course.title.includes("Python")) {
        contentType = "pdf";
        contentPath = "html/book/python.pdf";
      } else {
        // 默认值
        contentType = "pdf";
        contentPath = "html/book/python.pdf";
      }
      
      return {
        ...course,
        contentType: contentType,
        contentPath: contentPath
      };
    });
    
    res.json({
      success: true,
      courses: courses
    });
  } catch (error) {
    console.error('读取课程数据失败:', error);
    // 如果读取文件失败，返回默认数据
    const defaultCourses = [
      {
        id: 1,
        title: "数据结构与算法",
        description: "本课程将介绍各种常见的数据结构（如堆、栈、队列等）以及算法设计与分析方法。",
        image: "数据结构.png",
        author: "张教授",
        duration: "8周",
        level: "中级",
        rating: 4.8,
        students: 1200,
        price: 199.00,
        contentType: "video",
        contentPath: "html/book/堆.mp4"
      }
    ];
    
    res.json({
      success: true,
      courses: defaultCourses
    });
  }
});

// 新增API端点 - 获取网站链接
app.get('/api/websites', (req, res) => {
  try {
    const websitesData = fs.readFileSync(path.join(__dirname, 'doc', 'websites.json'), 'utf8');
    const websites = JSON.parse(websitesData);
    res.json({ success: true, websites });
  } catch (error) {
    console.error('获取网站数据失败:', error);
    res.status(500).json({ success: false, message: '获取网站数据失败' });
  }
});

// 新增API端点 - 获取课程图片
app.get('/api/course-image/:imageName', (req, res) => {
  const imageName = req.params.imageName;
  const imagePath = path.join(__dirname, 'img', imageName);
  
  if (fs.existsSync(imagePath)) {
    res.sendFile(imagePath);
  } else {
    res.status(404).json({ success: false, message: '图片不存在' });
  }
});

// 获取单个课程详情 (合并自根目录server.js)
app.get('/api/courses/:id', (req, res) => {
  const courseId = parseInt(req.params.id);
  
  try {
    // 从courses.json文件读取课程数据
    const coursesData = fs.readFileSync(path.join(__dirname, 'doc', 'courses.json'), 'utf8');
    const coursesFromFile = JSON.parse(coursesData);
    
    // 查找指定ID的课程
    const courseFromFile = coursesFromFile.find(c => c.id === courseId);
    
    if (!courseFromFile) {
      return res.status(404).json({
        success: false,
        message: "课程不存在"
      });
    }
    
    // 为课程添加contentType和contentPath
    let contentType, contentPath;
    
    if (courseFromFile.id === 1 || courseFromFile.title.includes("Java")) {
      contentType = "video";
      contentPath = "html/book/dui.mp4";
    } else if (courseFromFile.id === 2 || courseFromFile.title.includes("Android")) {
      contentType = "video";
      contentPath = "html/book/dui.mp4";
    } else if (courseFromFile.id === 3 || courseFromFile.title.includes("数据结构")) {
      contentType = "video";
      contentPath = "html/book/dui.mp4";
    } else if (courseFromFile.id === 4 || courseFromFile.title.includes("Web")) {
      contentType = "pdf";
      contentPath = "html/book/python.pdf";
    } else if (courseFromFile.id === 5 || courseFromFile.title.includes("Python")) {
      contentType = "pdf";
      contentPath = "html/book/python.pdf";
    } else {
      contentType = "pdf";
      contentPath = "html/book/python.pdf";
    }
    
    const course = {
      ...courseFromFile,
      contentType: contentType,
      contentPath: contentPath
    };
    
         res.json({
       success: true,
       course: course
     });
   } catch (error) {
     console.error('读取课程数据失败:', error);
     return res.status(500).json({
       success: false,
       message: "服务器错误"
     });
   }
});

// 新增API端点 - 获取课程的子课程列表
app.get('/api/courses/:id/subcourses', (req, res) => {
  const courseId = parseInt(req.params.id);
  
  // 生成子课程的通用函数
  const generateSubCourse = (id, chapterNum, title, description, duration, parentId) => {
    // 根据课程ID选择不同的文件
    let videoPath, pdfPath;
    
    switch(parentId) {
      case 1: // Java编程基础
        videoPath = "html/book/dui.mp4";
        pdfPath = "html/book/java.pdf";
        break;
      case 2: // Android应用开发
        videoPath = "html/book/dui.mp4";
        pdfPath = "html/book/android.pdf";
        break;
      case 3: // 数据结构与算法
        videoPath = "html/book/dui.mp4";
        pdfPath = "html/book/algorithm.pdf";
        break;
      case 4: // Web前端开发
        videoPath = "html/book/dui.mp4";
        pdfPath = "html/book/web.pdf";
        break;
      case 5: // Python人工智能入门
        videoPath = "html/book/dui.mp4";
        pdfPath = "html/book/python.pdf";
        break;
      default:
        videoPath = "html/book/dui.mp4";
        pdfPath = "html/book/python.pdf";
    }
    
    return {
      id: id,
      title: `第${chapterNum}章：${title}`,
      description: description,
      duration: duration,
      contents: [
        {
          type: "video",
          path: videoPath,
          label: "视频讲解"
        },
        {
          type: "pdf",
          path: pdfPath,
          label: "课件资料"
        }
      ],
      isSubCourse: true,
      parentCourseId: parentId
    };
  };
  
  let subCourses = [];
  
  if (courseId === 1) {
    // Java编程基础课程的子课程
    subCourses = [
      generateSubCourse(101, 1, "Java开发环境搭建", "安装JDK和IDE，配置开发环境", "15分钟", 1),
      generateSubCourse(102, 2, "Java基础语法", "学习Java的基本语法和数据类型", "20分钟", 1),
      generateSubCourse(103, 3, "面向对象编程基础", "理解类、对象、封装等OOP概念", "25分钟", 1),
      generateSubCourse(104, 4, "继承与多态", "掌握Java继承机制和多态特性", "22分钟", 1),
      generateSubCourse(105, 5, "异常处理机制", "学习Java异常处理的最佳实践", "18分钟", 1),
      generateSubCourse(106, 6, "集合框架详解", "掌握List、Set、Map等集合的使用", "28分钟", 1),
      generateSubCourse(107, 7, "IO流操作", "学习文件读写和流操作", "24分钟", 1),
      generateSubCourse(108, 8, "多线程编程", "理解线程概念和并发编程", "30分钟", 1),
      generateSubCourse(109, 9, "网络编程基础", "学习Socket编程和HTTP通信", "26分钟", 1),
      generateSubCourse(110, 10, "数据库连接", "使用JDBC连接和操作数据库", "32分钟", 1)
    ];
  } else if (courseId === 2) {
    // Android应用开发课程的子课程
    subCourses = [
      generateSubCourse(201, 1, "Android开发环境", "安装Android Studio和SDK配置", "15分钟", 2),
      generateSubCourse(202, 2, "Activity生命周期", "理解Activity的生命周期管理", "20分钟", 2),
      generateSubCourse(203, 3, "UI界面设计", "学习布局管理器和控件使用", "25分钟", 2),
      generateSubCourse(204, 4, "Intent与组件通信", "掌握组件间的数据传递", "22分钟", 2),
      generateSubCourse(205, 5, "数据存储方案", "学习SharedPreferences、SQLite等", "28分钟", 2),
      generateSubCourse(206, 6, "网络请求处理", "使用Retrofit进行网络通信", "24分钟", 2),
      generateSubCourse(207, 7, "RecyclerView列表", "实现复杂列表和网格布局", "26分钟", 2),
      generateSubCourse(208, 8, "Fragment使用", "学习Fragment的生命周期和使用", "23分钟", 2),
      generateSubCourse(209, 9, "多媒体处理", "处理图片、音频和视频", "30分钟", 2),
      generateSubCourse(210, 10, "应用发布上线", "打包签名和应用商店发布", "18分钟", 2)
    ];
  } else if (courseId === 3) {
    // 数据结构与算法课程的子课程
    subCourses = [
      generateSubCourse(301, 1, "数据结构基础概念", "了解数据结构的基本概念和分类", "15分钟", 3),
      generateSubCourse(302, 2, "线性表的实现", "学习顺序表和链表的实现原理", "20分钟", 3),
      generateSubCourse(303, 3, "栈与队列详解", "掌握栈和队列的特点及应用", "18分钟", 3),
      generateSubCourse(304, 4, "树结构基础", "学习二叉树、平衡树等树形结构", "25分钟", 3),
      generateSubCourse(305, 5, "堆与优先队列", "深入理解堆的性质和实现", "22分钟", 3),
      generateSubCourse(306, 6, "图的基本概念", "学习图的表示方法和基本术语", "16分钟", 3),
      generateSubCourse(307, 7, "图的遍历算法", "掌握DFS和BFS遍历算法", "28分钟", 3),
      generateSubCourse(308, 8, "排序算法原理", "学习各种排序算法的实现", "30分钟", 3),
      generateSubCourse(309, 9, "查找算法详解", "掌握线性查找、二分查找等", "24分钟", 3),
      generateSubCourse(310, 10, "算法复杂度分析", "学习时间和空间复杂度分析", "26分钟", 3)
    ];
  } else if (courseId === 4) {
    // Web前端开发课程的子课程
    subCourses = [
      generateSubCourse(401, 1, "HTML基础标签", "学习HTML的基本标签和语义化", "12分钟", 4),
      generateSubCourse(402, 2, "CSS样式设计", "掌握CSS选择器和样式属性", "18分钟", 4),
      generateSubCourse(403, 3, "CSS布局技术", "学习Flexbox和Grid布局", "22分钟", 4),
      generateSubCourse(404, 4, "JavaScript基础", "掌握JS基本语法和数据类型", "20分钟", 4),
      generateSubCourse(405, 5, "DOM操作详解", "学习DOM元素的增删改查", "25分钟", 4),
      generateSubCourse(406, 6, "事件处理机制", "理解事件冒泡和事件委托", "16分钟", 4),
      generateSubCourse(407, 7, "AJAX异步请求", "学习异步数据获取和处理", "24分钟", 4),
      generateSubCourse(408, 8, "响应式设计", "实现移动端适配和响应式布局", "28分钟", 4),
      generateSubCourse(409, 9, "前端框架入门", "了解Vue.js或React基础", "30分钟", 4),
      generateSubCourse(410, 10, "项目实战练习", "完成一个完整的前端项目", "35分钟", 4)
    ];
  } else if (courseId === 5) {
    // Python人工智能入门课程的子课程
    subCourses = [
      generateSubCourse(501, 1, "Python环境搭建", "安装Python和开发环境配置", "12分钟", 5),
      generateSubCourse(502, 2, "Python基础语法", "掌握Python的基本语法", "25分钟", 5),
      generateSubCourse(503, 3, "函数与模块", "学习函数定义和模块导入", "20分钟", 5),
      generateSubCourse(504, 4, "面向对象编程", "理解Python中的类和对象", "28分钟", 5),
      generateSubCourse(505, 5, "NumPy数值计算", "学习NumPy进行高效数值计算", "22分钟", 5),
      generateSubCourse(506, 6, "Pandas数据处理", "掌握数据清洗和分析技术", "26分钟", 5),
      generateSubCourse(507, 7, "Matplotlib可视化", "学习数据可视化图表制作", "18分钟", 5),
      generateSubCourse(508, 8, "机器学习基础", "了解机器学习的基本概念", "30分钟", 5),
      generateSubCourse(509, 9, "Scikit-learn实战", "使用机器学习库构建模型", "35分钟", 5),
      generateSubCourse(510, 10, "深度学习入门", "初步了解神经网络概念", "32分钟", 5),
            generateSubCourse(511, 11, "TensorFlow基础", "学习深度学习框架使用", "28分钟", 5)
    ];
  } else {
    // 其他课程暂无子课程
    subCourses = [];
  }
  
  res.json({
    success: true,
    data: subCourses
  });
});

// 获取用户账户信息
app.get('/api/account', (req, res) => {
  if (!req.session.user) {
    return res.status(401).json({ success: false, message: '未登录' });
  }
  
  const userId = req.session.user.id;
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    // 内存存储中暂时没有账户系统，返回默认值
    return res.status(200).json({
      success: true,
      account: {
        balance: 1000.00,
        created_at: new Date()
      }
    });
  }
  
  // 使用数据库
  const getAccountQuery = 'SELECT * FROM accounts WHERE user_id = ?';
  db.query(getAccountQuery, [userId], (err, results) => {
    if (err) {
      console.error('获取账户信息失败:', err);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
    
    if (results.length === 0) {
      // 账户不存在，创建一个新账户
      const createAccountQuery = 'INSERT INTO accounts (user_id, balance) VALUES (?, 1000.00)';
      db.query(createAccountQuery, [userId], (err, result) => {
        if (err) {
          console.error('创建账户失败:', err);
          return res.status(500).json({ success: false, message: '服务器错误' });
        }
        
        return res.status(200).json({
          success: true,
          account: {
            id: result.insertId,
            user_id: userId,
            balance: 1000.00,
            created_at: new Date()
          }
        });
      });
    } else {
      // 返回现有账户
      res.status(200).json({
        success: true,
        account: results[0]
      });
    }
  });
});

// 获取用户订单
app.get('/api/orders', (req, res) => {
  if (!req.session.user) {
    return res.status(401).json({ success: false, message: '未登录' });
  }
  
  const userId = req.session.user.id;
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    // 内存存储中暂时没有订单系统，返回空数组
    return res.status(200).json({
      success: true,
      orders: []
    });
  }
  
  // 使用数据库
  const getOrdersQuery = 'SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC';
  db.query(getOrdersQuery, [userId], (err, results) => {
    if (err) {
      console.error('获取订单失败:', err);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
    
    res.status(200).json({
      success: true,
      orders: results
    });
  });
});

// 购买课程
app.post('/api/purchase', (req, res) => {
  if (!req.session.user) {
    return res.status(401).json({ success: false, message: '未登录' });
  }
  
  const userId = req.session.user.id;
  const { courseId, courseTitle, price } = req.body;
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    // 内存存储中暂时没有购买系统，直接返回成功
    return res.status(200).json({
      success: true,
      message: '购买成功！',
      order: {
        id: Date.now(),
        user_id: userId,
        course_id: courseId,
        course_title: courseTitle,
        price: price,
        status: 'completed',
        created_at: new Date()
      }
    });
  }
  
  // 使用数据库，开启事务
  db.beginTransaction(err => {
    if (err) {
      console.error('开启事务失败:', err);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
    
    // 检查账户余额
    const checkBalanceQuery = 'SELECT balance FROM accounts WHERE user_id = ?';
    db.query(checkBalanceQuery, [userId], (err, results) => {
      if (err) {
        return db.rollback(() => {
          console.error('检查余额失败:', err);
          res.status(500).json({ success: false, message: '服务器错误' });
        });
      }
      
      if (results.length === 0) {
        return db.rollback(() => {
          res.status(400).json({ success: false, message: '账户不存在' });
        });
      }
      
      const balance = results[0].balance;
      if (balance < price) {
        return db.rollback(() => {
          res.status(400).json({ success: false, message: '余额不足' });
        });
      }
      
      // 更新账户余额
      const updateBalanceQuery = 'UPDATE accounts SET balance = balance - ? WHERE user_id = ?';
      db.query(updateBalanceQuery, [price, userId], (err, result) => {
        if (err) {
          return db.rollback(() => {
            console.error('更新余额失败:', err);
            res.status(500).json({ success: false, message: '服务器错误' });
          });
        }
        
        // 创建订单
        const createOrderQuery = 'INSERT INTO orders (user_id, course_id, course_title, price) VALUES (?, ?, ?, ?)';
        db.query(createOrderQuery, [userId, courseId, courseTitle, price], (err, result) => {
          if (err) {
            return db.rollback(() => {
              console.error('创建订单失败:', err);
              res.status(500).json({ success: false, message: '服务器错误' });
            });
          }
          
          // 提交事务
          db.commit(err => {
            if (err) {
              return db.rollback(() => {
                console.error('提交事务失败:', err);
                res.status(500).json({ success: false, message: '服务器错误' });
              });
            }
            
            res.status(200).json({
              success: true,
              message: '购买成功！',
              order: {
                id: result.insertId,
                user_id: userId,
                course_id: courseId,
                course_title: courseTitle,
                price: price,
                status: 'completed',
                created_at: new Date()
              }
            });
          });
        });
      });
    });
  });
});

// 提交问题
app.post('/api/questions', upload.single('image'), (req, res) => {
  if (!req.session.user) {
    return res.status(401).json({ success: false, message: '未登录' });
  }
  
  const userId = req.session.user.id;
  const { title, content } = req.body;
  let imagePath = null;
  
  // 检查是否上传了图片
  if (req.file) {
    imagePath = '/uploads/' + req.file.filename;
  }
  
  // 创建当前时间
  const currentDate = new Date();
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    // 内存存储中暂时没有问题系统，直接返回成功
    return res.status(200).json({
      success: true,
      message: '提交成功！',
      question: {
        id: Date.now(),
        user_id: userId,
        title: title,
        content: content,
        image_path: imagePath,
        status: 'pending',
        created_at: currentDate
      }
    });
  }
  
  // 使用数据库
  const createQuestionQuery = 'INSERT INTO questions (user_id, title, content, image_path) VALUES (?, ?, ?, ?)';
  db.query(createQuestionQuery, [userId, title, content, imagePath], (err, result) => {
    if (err) {
      console.error('创建问题失败:', err);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
    
    res.status(200).json({
      success: true,
      message: '提交成功！',
      question: {
        id: result.insertId,
        user_id: userId,
        title: title,
        content: content,
        image_path: imagePath,
        status: 'pending',
        created_at: currentDate
      }
    });
  });
});

// 获取问题列表
app.get('/api/questions', (req, res) => {
  // 详细记录请求信息，用于调试
  console.log(`API调用: /api/questions`);
  console.log(`查询参数: `, req.query);
  console.log(`会话信息: `, req.session);
  
  if (!req.session.user) {
    console.log(`用户未登录，返回401`);
    return res.status(401).json({ success: false, message: '未登录' });
  }
  
  // 优先使用查询参数中的用户ID，如果没有则使用会话中的用户ID
  const userId = req.query.user_id ? parseInt(req.query.user_id) : req.session.user.id;
  const getAllQuestions = req.query.all === 'true';
  
  console.log(`获取问题列表: userId=${userId}, all=${getAllQuestions}`);
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    console.log(`使用内存存储模式，返回空问题列表`);
    // 内存存储中暂时没有问题系统，返回空数组
    return res.status(200).json({
      success: true,
      questions: []
    });
  }
  
  // 使用数据库
  let getQuestionsQuery;
  let queryParams = [];
  
  if (getAllQuestions) {
    // 获取所有问题
    getQuestionsQuery = `
      SELECT q.*, u.name as user_name 
      FROM questions q 
      JOIN users u ON q.user_id = u.id 
      ORDER BY q.created_at DESC
    `;
  } else {
    // 只获取当前用户的问题
    getQuestionsQuery = `
      SELECT q.*, u.name as user_name 
      FROM questions q 
      JOIN users u ON q.user_id = u.id 
      WHERE q.user_id = ? 
      ORDER BY q.created_at DESC
    `;
    queryParams.push(userId);
  }
  
  console.log(`执行SQL查询: ${getQuestionsQuery.replace(/\s+/g, ' ')}`);
  console.log(`参数: [${queryParams.join(', ')}]`);
  
  db.query(getQuestionsQuery, queryParams, (err, results) => {
    if (err) {
      console.error('获取问题列表失败:', err);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
    
    console.log(`查询结果: ${results.length}条问题记录`);
    if (results.length > 0) {
      console.log(`第一条问题示例: ${JSON.stringify(results[0])}`);
    }
    
    // 确保创建一个有效的响应
    const response = {
      success: true,
      questions: results || []
    };
    
    console.log(`返回响应: success=${response.success}, questions.length=${response.questions.length}`);
    res.status(200).json(response);
  });
});

// 获取我的问题列表
app.get('/api/my-questions', (req, res) => {
  // 详细记录请求信息，用于调试
  console.log(`API调用: /api/my-questions`);
  console.log(`会话信息: `, req.session);
  
  if (!req.session.user) {
    console.log(`用户未登录，返回401`);
    return res.status(401).json({ success: false, message: '未登录' });
  }
  
  const userId = req.session.user.id;
  console.log(`获取我的问题列表: userId=${userId}`);
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    console.log(`使用内存存储模式，返回空问题列表`);
    // 内存存储中暂时没有问题系统，返回空数组
    return res.status(200).json({
      success: true,
      questions: []
    });
  }
  
  // 使用数据库
  const getQuestionsQuery = `
    SELECT q.*, u.name as user_name 
    FROM questions q 
    JOIN users u ON q.user_id = u.id 
    WHERE q.user_id = ? 
    ORDER BY q.created_at DESC
  `;
  
  console.log(`执行SQL查询: ${getQuestionsQuery.replace(/\s+/g, ' ')}`);
  console.log(`参数: [${userId}]`);
  
  db.query(getQuestionsQuery, [userId], (err, results) => {
    if (err) {
      console.error('获取我的问题列表失败:', err);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
    
    console.log(`我的问题查询结果: ${results.length}条问题记录`);
    if (results.length > 0) {
      console.log(`第一条问题示例: ${JSON.stringify(results[0])}`);
    }
    
    // 确保创建一个有效的响应
    const response = {
      success: true,
      questions: results || []
    };
    
    console.log(`返回响应: success=${response.success}, questions.length=${response.questions.length}`);
    res.status(200).json(response);
  });
});

// 获取问题详情
app.get('/api/questions/:id', (req, res) => {
  if (!req.session.user) {
    return res.status(401).json({ success: false, message: '未登录' });
  }
  
  const questionId = req.params.id;
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    // 内存存储中暂时没有问题系统，返回错误
    return res.status(404).json({
      success: false,
      message: '问题不存在'
    });
  }
  
  // 使用数据库
  const getQuestionQuery = `
    SELECT q.*, u.name as user_name 
    FROM questions q 
    JOIN users u ON q.user_id = u.id 
    WHERE q.id = ?
  `;
  
  db.query(getQuestionQuery, [questionId], (err, questionResults) => {
    if (err) {
      console.error('获取问题详情失败:', err);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
    
    if (questionResults.length === 0) {
      return res.status(404).json({ success: false, message: '问题不存在' });
    }
    
    // 获取问题的回复
    const getRepliesQuery = `
      SELECT r.*, u.name as user_name 
      FROM replies r 
      JOIN users u ON r.user_id = u.id 
      WHERE r.question_id = ? 
      ORDER BY r.created_at ASC
    `;
    
    db.query(getRepliesQuery, [questionId], (err, replyResults) => {
      if (err) {
        console.error('获取回复失败:', err);
        return res.status(500).json({ success: false, message: '服务器错误' });
      }
      
      const question = questionResults[0];
      question.replies = replyResults;
      
      res.status(200).json({
        success: true,
        question: question
      });
    });
  });
});

// 回复问题
app.post('/api/questions/:id/replies', (req, res) => {
  if (!req.session.user) {
    return res.status(401).json({ success: false, message: '未登录' });
  }
  
  const userId = req.session.user.id;
  const questionId = req.params.id;
  const { content } = req.body;
  
  // 如果使用内存存储
  if (useMemoryStorage) {
    // 内存存储中暂时没有回复系统，直接返回成功
    return res.status(200).json({
      success: true,
      message: '回复成功！',
      reply: {
        id: Date.now(),
        question_id: questionId,
        user_id: userId,
        content: content,
        created_at: new Date()
      }
    });
  }
  
  // 使用数据库
  const createReplyQuery = 'INSERT INTO replies (question_id, user_id, content) VALUES (?, ?, ?)';
  db.query(createReplyQuery, [questionId, userId, content], (err, result) => {
    if (err) {
      console.error('创建回复失败:', err);
      return res.status(500).json({ success: false, message: '服务器错误' });
    }
    
    // 更新问题状态为已回答
    const updateQuestionQuery = 'UPDATE questions SET status = "answered" WHERE id = ?';
    db.query(updateQuestionQuery, [questionId], (err) => {
      if (err) {
        console.error('更新问题状态失败:', err);
        // 继续流程，即使状态更新失败
      }
      
      // 获取用户名
      const getUserQuery = 'SELECT name FROM users WHERE id = ?';
      db.query(getUserQuery, [userId], (err, userResults) => {
        if (err || userResults.length === 0) {
          console.error('获取用户名失败:', err);
          // 继续流程，即使获取用户名失败
          
          res.status(200).json({
            success: true,
            message: '回复成功！',
            reply: {
              id: result.insertId,
              question_id: questionId,
              user_id: userId,
              content: content,
              created_at: new Date()
            }
          });
        } else {
          res.status(200).json({
            success: true,
            message: '回复成功！',
            reply: {
              id: result.insertId,
              question_id: questionId,
              user_id: userId,
              user_name: userResults[0].name,
              content: content,
              created_at: new Date()
            }
          });
        }
      });
    });
  });
});

// 启动服务器
app.listen(config.PORT, () => {
  console.log(`服务器运行在 ${config.BASE_URL}`);
  console.log(`API端点:`);
  console.log(`- 注册: ${config.BASE_URL}/api/register`);
  console.log(`- 登录: ${config.BASE_URL}/api/login`);
  console.log(`- 用户列表: ${config.BASE_URL}/api/users`);
  console.log(`- 课程列表: ${config.BASE_URL}/api/courses`);
  console.log(`- 子课程列表: ${config.BASE_URL}/api/courses/{id}/subcourses`);
  console.log(`文件服务:`);
  console.log(`- PDF文件可通过 ${config.BASE_URL}/html/book/python.pdf 访问`);
  console.log(`- 视频文件可通过 ${config.BASE_URL}/html/book/dui.mp4 访问`);
  
  if (useMemoryStorage) {
    console.log('\n警告: 当前使用内存存储模式，所有数据将在服务器重启后丢失');
    console.log('示例用户: test@example.com (密码: 123456)');
  } else {
    console.log('\n数据库连接成功，使用MySQL存储模式');
  }
});