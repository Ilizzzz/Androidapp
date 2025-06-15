package com.example.yunclass.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.example.yunclass.config.AppConfig;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.Context;
import android.util.Log;

public class ApiClient {
    
    // 从AppConfig获取BASE_URL，便于统一管理
    private static Retrofit retrofit = null;
    private static final Map<String, List<Cookie>> cookieStore = new HashMap<>();
    
    public static ApiService getApiService() {
        if (retrofit == null) {
            // 创建日志拦截器
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                Log.d("OkHttp", message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // 创建OkHttpClient，添加Cookie管理
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .cookieJar(new CookieJar() {
                        @Override
                        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                            Log.d("ApiClient", "Saving cookies for host: " + url.host());
                            cookieStore.put(url.host(), cookies);
                        }

                        @Override
                        public List<Cookie> loadForRequest(HttpUrl url) {
                            List<Cookie> cookies = cookieStore.get(url.host());
                            Log.d("ApiClient", "Loading cookies for host: " + url.host() + 
                                    ", found: " + (cookies != null ? cookies.size() : 0));
                            return cookies != null ? cookies : new ArrayList<>();
                        }
                    })
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();
            
            // 创建自定义Gson实例以正确处理日期格式
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                        @Override
                        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            try {
                                // 打印接收到的日期字符串，用于调试
                                String jsonString = json.toString();
                                System.out.println("Date JSON: " + jsonString);
                                
                                // 优先使用当前时间而不是硬编码的时间
                                Date currentDate = new Date();
                                System.out.println("Current system date: " + currentDate);
                                
                                // 尝试作为时间戳处理
                                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                                    return new Date(json.getAsLong());
                                }
                                
                                // 尝试作为字符串处理
                                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                                    String dateStr = json.getAsString();
                                    System.out.println("Parsing date string: " + dateStr);
                                    
                                    // 处理ISO格式 (2023-05-29T10:07:18.000Z)
                                    if (dateStr.contains("T") && (dateStr.contains("Z") || dateStr.contains("+"))) {
                                        try {
                                            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                                            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                            Date parsedDate = isoFormat.parse(dateStr);
                                            System.out.println("Successfully parsed ISO date: " + parsedDate);
                                            return parsedDate;
                                        } catch (Exception e) {
                                            System.out.println("ISO date parse error: " + e.getMessage());
                                            // 尝试不同的ISO格式
                                            try {
                                                SimpleDateFormat altIsoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                                                Date parsedDate = altIsoFormat.parse(dateStr);
                                                System.out.println("Successfully parsed alternative ISO date: " + parsedDate);
                                                return parsedDate;
                                            } catch (Exception e2) {
                                                System.out.println("Alternative ISO date parse error: " + e2.getMessage());
                                            }
                                        }
                                    }
                                    
                                    // 处理MySQL格式 (2023-05-29 10:07:18)
                                    if (dateStr.contains("-") && dateStr.contains(":")) {
                                        try {
                                            SimpleDateFormat mysqlFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                            Date parsedDate = mysqlFormat.parse(dateStr);
                                            System.out.println("Successfully parsed MySQL date: " + parsedDate);
                                            return parsedDate;
                                        } catch (Exception e) {
                                            System.out.println("MySQL date parse error: " + e.getMessage());
                                        }
                                    }
                                    
                                    // 处理简单日期格式 (2023-05-29)
                                    if (dateStr.contains("-") && dateStr.length() == 10) {
                                        try {
                                            SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                            Date parsedDate = simpleFormat.parse(dateStr);
                                            System.out.println("Successfully parsed simple date: " + parsedDate);
                                            return parsedDate;
                                        } catch (Exception e) {
                                            System.out.println("Simple date parse error: " + e.getMessage());
                                        }
                                    }
                                }
                                
                                // 如果无法解析，返回当前时间
                                System.out.println("Returning current date as fallback");
                                return currentDate;
                            } catch (Exception e) {
                                e.printStackTrace();
                                return new Date(); // 出错时返回当前时间
                            }
                        }
                    })
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .create();
            
            // 创建Retrofit实例
            retrofit = new Retrofit.Builder()
                    .baseUrl(AppConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        
        return retrofit.create(ApiService.class);
    }
} 