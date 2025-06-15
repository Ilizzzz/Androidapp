package com.example.yunclass.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * 清除购买记录的工具类
 */
public class ClearPurchasesUtil {
    
    /**
     * 清除所有购买记录并显示提示
     * @param context 上下文
     */
    public static void clearAllPurchases(Context context) {
        PurchaseManager.clearPurchasedCourses(context);
        Toast.makeText(context, "已清除所有购买记录", Toast.LENGTH_SHORT).show();
    }
}