package com.example.yunclass.config;

/**
 * 应用程序全局配置
 * 集中管理所有配置项，便于统一修改
 */
public class AppConfig {
    /**
     * API服务器配置
     * 修改这些配置即可全局更改API请求地址
     */
    private static final String HOST = "10.163.118.129";
    private static final int PORT = 3000;
    
    /**
     * API服务器基础URL
     * 自动根据HOST和PORT生成
     */
    public static final String BASE_URL = "http://" + HOST + ":" + PORT + "/";
} 