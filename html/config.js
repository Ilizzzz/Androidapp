/**
 * 统一配置文件
 * 集中管理所有配置项，便于统一修改
 * 此文件同时支持Node.js环境（服务器端）和浏览器环境（客户端）
 */

// 基础配置，被客户端和服务器共享
const BaseConfig = {
    // 服务器主机配置
    HOST: '10.163.118.129',
    PORT: 3000,
    
    // 获取完整的服务器URL
    get BASE_URL() {
        return `http://${this.HOST}:${this.PORT}`;
    }
};

// 客户端配置（用于前端JavaScript）
const ClientConfig = {
    ...BaseConfig,
    
    /**
     * API服务器配置
     * 修改这些配置请编辑html/config.js文件
     */
    HOST: BaseConfig.HOST,
    PORT: BaseConfig.PORT,
    
    /**
     * API服务器基础URL
     * 自动根据HOST和PORT生成
     */
    get BASE_URL() {
        return `http://${this.HOST}:${this.PORT}`;
    }
};

// 服务器配置（用于Node.js后端）
const ServerConfig = {
    ...BaseConfig,
    
    // 数据库配置
    DB: {
        HOST: 'localhost',
        USER: 'root',
        PASSWORD: '20031019Lsj',
        PORT: 3306,
        CONNECT_TIMEOUT: 10000
    },
    
    // 会话配置
    SESSION: {
        SECRET: 'edustage-secret-key',
        MAX_AGE: 24 * 60 * 60 * 1000 // 24小时
    },
    
    // CORS配置
    CORS: {
        ORIGINS: true, // 允许所有来源
        CREDENTIALS: true // 允许携带凭证
    }
};

// 防止配置被修改
Object.freeze(BaseConfig);
Object.freeze(ClientConfig);
Object.freeze(ServerConfig);
Object.freeze(ServerConfig.DB);
Object.freeze(ServerConfig.SESSION);
Object.freeze(ServerConfig.CORS);

// 根据环境导出不同的配置
if (typeof module !== 'undefined' && module.exports) {
    // Node.js环境，导出服务器配置
    module.exports = ServerConfig;
} else {
    // 浏览器环境，导出客户端配置
    window.AppConfig = ClientConfig;
} 