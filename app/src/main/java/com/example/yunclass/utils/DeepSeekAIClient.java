package com.example.yunclass.utils;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepSeekAIClient {
    private static final String TAG = "DeepSeekAIClient";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private OkHttpClient client;
    private JSONArray messages;
    
    // 系统初始提示信息
    private static final String SYSTEM_PROMPT = 
            "你是一个专业的云课程销售客服，具有以下特点：\n" +
            "1. 你熟悉各种在线教育课程和学习资源\n" +
            "2. 你能够根据用户的需求推荐合适的课程\n" +
            "3. 你擅长解答关于课程内容、价格、优惠等问题\n" +
            "4. 你态度亲切、专业，善于引导用户购买课程\n" +
            "5. 回答简明扼要，避免过长的回复\n" +
            "请记住，你的目标是帮助用户找到最适合他们的课程，并促成销售。";
    
    private String apiUrl;
    private String apiKey;
    private String apiModel;
    private int timeout;
    
    public DeepSeekAIClient() {
        // 设置OkHttpClient
        client = new OkHttpClient.Builder()
                .connectTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        
        // 初始化对话历史
        resetConversation();
        
        // 初始化API参数
        initApiParameters();
    }
    
    /**
     * 初始化API参数
     */
    private void initApiParameters() {
        try {
            apiUrl = AppConfig.getApiUrl();
            apiKey = AppConfig.getApiKey();
            apiModel = AppConfig.getAiModel();
            timeout = AppConfig.getApiTimeoutSeconds();
            
            Log.d(TAG, "API参数初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "API参数初始化失败", e);
        }
    }
    
    /**
     * 向AI发送消息并获取回复
     */
    public String sendMessage(String userMessage) {
        try {
            // 添加用户消息到历史
            addMessageToHistory("user", userMessage);
            
            // 创建请求体 - DeepSeek API格式
            JSONObject requestJson = new JSONObject();
            
            // 构建请求体
            try {
                // 设置模型
                requestJson.put("model", apiModel);
                
                // 标准格式的消息数组
                requestJson.put("messages", messages);
                
                // 其他可能需要的参数
                requestJson.put("temperature", 0.7);
                requestJson.put("max_tokens", 800);
            } catch (JSONException e) {
                Log.e(TAG, "构建请求体失败", e);
                return "发送请求时出错，请稍后重试。";
            }
            
            RequestBody body = RequestBody.create(requestJson.toString(), JSON);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            
            Log.d(TAG, "发送请求: " + requestJson.toString());
            
            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    logApiError(response);
                    return "连接DeepSeek服务失败，请检查网络后重试。";
                }
                
                String jsonResponse = response.body().string();
                Log.d(TAG, "收到响应: " + jsonResponse);
                
                JSONObject jsonObject = new JSONObject(jsonResponse);
                
                // 解析回复 - DeepSeek API响应格式
                String assistantReply = parseDeepSeekResponse(jsonObject);
                
                // 如果解析失败，返回错误信息
                if (assistantReply.isEmpty()) {
                    return "解析AI回复时出错，请重试。";
                }
                
                // 将助手回复添加到历史
                addMessageToHistory("assistant", assistantReply);
                
                return assistantReply;
            } catch (IOException e) {
                Log.e(TAG, "网络请求失败", e);
                return "网络连接异常，请检查您的网络设置后重试。";
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON处理失败", e);
            return "处理请求时出错，请稍后重试。";
        }
    }
    
    // 记录API错误
    private void logApiError(Response response) {
        Log.e(TAG, "API请求失败: 状态码=" + response.code() + ", 消息=" + response.message());
        
        if (response.body() != null) {
            try {
                String errorBody = response.body().string();
                Log.e(TAG, "错误响应: " + errorBody);
            } catch (IOException e) {
                Log.e(TAG, "读取错误响应失败", e);
            }
        }
    }
    
    // 解析DeepSeek API响应
    private String parseDeepSeekResponse(JSONObject jsonObject) {
        try {
            // DeepSeek API标准响应格式: choices[0].message.content
            if (jsonObject.has("choices") && !jsonObject.isNull("choices")) {
                JSONArray choices = jsonObject.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    if (choice.has("message") && !choice.isNull("message")) {
                        JSONObject message = choice.getJSONObject("message");
                        if (message.has("content") && !message.isNull("content")) {
                            return message.getString("content");
                        }
                    }
                }
            }
            
            // 备用解析逻辑
            Log.d(TAG, "标准解析失败，尝试备用解析");
            return extractTextFromJSON(jsonObject);
            
        } catch (JSONException e) {
            Log.e(TAG, "解析响应失败", e);
            return "";
        }
    }
    
    private void addMessageToHistory(String role, String content) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        messages.put(message);
        
        // 限制历史消息数量，防止请求过大
        if (messages.length() > 10) {
            // 保留系统消息和最近的9条消息
            JSONArray newMessages = new JSONArray();
            // 添加系统消息（如果存在）
            for (int i = 0; i < messages.length(); i++) {
                JSONObject msg = messages.getJSONObject(i);
                if ("system".equals(msg.getString("role"))) {
                    newMessages.put(msg);
                    break;
                }
            }
            // 添加最近的消息
            int startIndex = Math.max(1, messages.length() - 9); // 跳过系统消息
            for (int i = startIndex; i < messages.length(); i++) {
                newMessages.put(messages.getJSONObject(i));
            }
            messages = newMessages;
        }
    }
    
    private String extractTextFromJSON(JSONObject json) {
        try {
            // 递归查找任何名为"content"、"text"或"message"的字段
            for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                String key = it.next();
                if (("content".equals(key) || "text".equals(key) || "message".equals(key)) 
                        && json.get(key) instanceof String) {
                    return json.getString(key);
                } else if (json.get(key) instanceof JSONObject) {
                    String result = extractTextFromJSON(json.getJSONObject(key));
                    if (!result.isEmpty()) {
                        return result;
                    }
                } else if (json.get(key) instanceof JSONArray) {
                    JSONArray array = json.getJSONArray(key);
                    for (int i = 0; i < array.length(); i++) {
                        if (array.get(i) instanceof JSONObject) {
                            String result = extractTextFromJSON(array.getJSONObject(i));
                            if (!result.isEmpty()) {
                                return result;
                            }
                        }
                    }
                }
            }
            return "";
        } catch (JSONException e) {
            Log.e(TAG, "JSON递归提取失败", e);
            return "";
        }
    }
    
    public void resetConversation() {
        messages = new JSONArray();
        try {
            // 添加系统消息作为对话的引导
            addMessageToHistory("system", SYSTEM_PROMPT);
        } catch (JSONException e) {
            Log.e(TAG, "重置对话失败", e);
        }
    }
} 