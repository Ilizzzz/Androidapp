package com.example.yunclass.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.yunclass.model.Order;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 调试工具类，用于调试和日志记录
 */
public class DebugUtils {
    private static final String TAG = "DebugUtils";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    /**
     * 记录订单信息到日志文件
     * @param context 上下文
     * @param order 订单对象
     */
    public static void logOrderInfo(Context context, Order order) {
        if (order == null) {
            Log.e(TAG, "订单对象为空");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("订单信息: \n");
        sb.append("ID: ").append(order.getId()).append("\n");
        sb.append("用户ID: ").append(order.getUserId()).append("\n");
        sb.append("课程ID: ").append(order.getCourseId()).append("\n");
        sb.append("课程标题: ").append(order.getCourseTitle()).append("\n");
        sb.append("价格: ").append(order.getPrice()).append("\n");
        sb.append("状态: ").append(order.getStatus()).append("\n");
        sb.append("创建时间: ").append(order.getCreatedAt() != null ? 
            DATE_FORMAT.format(order.getCreatedAt()) : "null").append("\n");
        
        Log.d(TAG, sb.toString());
        
        // 将信息写入文件
        writeToFile(context, "order_" + order.getId() + ".txt", sb.toString());
    }
    
    /**
     * 写入文本到文件
     * @param context 上下文
     * @param fileName 文件名
     * @param content 内容
     */
    public static void writeToFile(Context context, String fileName, String content) {
        try {
            File dir = new File(context.getExternalFilesDir(null), "debug");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File file = new File(dir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.append(content);
            writer.flush();
            writer.close();
            
            Log.d(TAG, "成功写入文件: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "写入文件失败", e);
        }
    }
    
    /**
     * 显示调试信息的 Toast
     * @param context 上下文
     * @param message 消息
     */
    public static void showDebugToast(Context context, String message) {
        Toast.makeText(context, "调试: " + message, Toast.LENGTH_LONG).show();
    }
} 