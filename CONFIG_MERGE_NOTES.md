

## 合并详情

### 合并前的结构：
- `html/config.js` - 主配置文件，包含服务器和客户端配置
- `html/js/config.js` - 由update-config脚本自动生成的前端配置文件

### 合并后的结构：
- `html/config.js` - 统一配置文件，同时支持Node.js和浏览器环境

## 主要改进

1. **统一配置管理**：所有配置现在集中在一个文件中
2. **环境自适应**：同一个文件可以在Node.js和浏览器环境中使用
3. **自动清理**：update-config脚本会自动删除旧的js/config.js文件
4. **向后兼容**：保持了原有的API和功能

## 配置文件结构

```javascript
// 基础配置
const BaseConfig = {
    HOST: '192.168.43.230',
    PORT: 3000,
    get BASE_URL() { return `http://${this.HOST}:${this.PORT}`; }
};

// 客户端配置（浏览器环境）
const ClientConfig = { ...BaseConfig, /* 客户端特定配置 */ };

// 服务器配置（Node.js环境）
const ServerConfig = { ...BaseConfig, DB: {...}, SESSION: {...}, CORS: {...} };
```

## 环境检测机制

```javascript
if (typeof module !== 'undefined' && module.exports) {
    // Node.js环境，导出服务器配置
    module.exports = ServerConfig;
} else {
    // 浏览器环境，导出客户端配置
    window.AppConfig = ClientConfig;
}
```

## 使用方式

### 在Node.js中（服务器端）：
```javascript
const config = require('./config');
console.log(config.HOST); // 访问服务器配置
```

### 在浏览器中（客户端）：
```html
<script src="config.js"></script>
<script>
    console.log(AppConfig.HOST); // 访问客户端配置
</script>
```

## 更新脚本修改

- 修改了 `html/scripts/update-android-config.js` 中的 `updateJsConfig()` 函数
- 现在会自动删除旧的 `js/config.js` 文件
- 不再生成单独的前端配置文件

## 测试结果

✅ Node.js环境加载正常（服务器启动成功）
✅ 浏览器环境加载正常（HTTP访问正常）
✅ update-config脚本运行正常
✅ Android配置更新正常

## 清理的文件

- 删除了 `html/js/config.js`
- 删除了空的 `html/js/` 目录
- 更新了 `README.md` 中的相关说明

## 注意事项

- 所有HTML文件现在应该直接引用 `config.js` 而不是 `js/config.js`
- 配置修改后仍需运行 `npm run update-config` 来同步Android配置
- 合并后的配置文件保持了所有原有功能和API兼容性 