package com.example.yunclass.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 应用程序全局配置
 */
public class AppConfig {
    private static final String TAG = "AppConfig";
    
    // 客服系统配置
    public static final int API_TIMEOUT_SECONDS = 10;        // API请求超时时间
    public static final int MAX_RETRY_COUNT = 3;             // 最大重试次数
    
    // API相关配置 - DeepSeek API
    private static final String DEFAULT_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEFAULT_API_KEY = "sk-8a7d0073020c4603afecd567129727d2";
    private static final String DEFAULT_AI_MODEL = "deepseek-chat";
    
    // 静态实例
    private static AppConfig instance;
    private Context context;
    private SharedPreferences preferences;
    
    private AppConfig(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getApplicationContext()
                .getSharedPreferences("app_config", Context.MODE_PRIVATE);
    }
    
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new AppConfig(context);
        }
    }
    
    public static AppConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppConfig未初始化，请先调用init()方法");
        }
        return instance;
    }
    
    // API相关方法
    public static String getApiUrl() {
        return getInstance().preferences.getString("api_url", DEFAULT_API_URL);
    }
    
    public static String getApiKey() {
        return getInstance().preferences.getString("api_key", DEFAULT_API_KEY);
    }
    
    public static String getAiModel() {
        return getInstance().preferences.getString("ai_model", DEFAULT_AI_MODEL);
    }
    
    public static int getApiTimeoutSeconds() {
        return getInstance().preferences.getInt("api_timeout_seconds", API_TIMEOUT_SECONDS);
    }
} 