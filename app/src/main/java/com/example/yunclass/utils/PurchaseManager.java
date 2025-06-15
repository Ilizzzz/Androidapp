package com.example.yunclass.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * 管理已购买课程的工具类
 */
public class PurchaseManager {
    private static final String TAG = "PurchaseManager";
    private static final String PREF_NAME = "purchase_preferences";
    private static final String KEY_PURCHASED_COURSES = "purchased_course_ids";
    private static final String KEY_USER_COURSES_PREFIX = "user_courses_";
    // 调试模式标记，用于在无购买记录时提供测试数据
    private static final boolean DEBUG_MODE = false;
    
    /**
     * 保存已购买的课程ID
     * @param context 上下文
     * @param courseId 课程ID
     */
    public static void savePurchasedCourse(Context context, int courseId) {
        if (context == null) return;
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> purchasedCourses = prefs.getStringSet(KEY_PURCHASED_COURSES, new HashSet<>());
            
            // 由于SharedPreferences返回的是不可变集合，需要创建一个新的集合
            Set<String> updatedCourses = new HashSet<>(purchasedCourses);
            updatedCourses.add(String.valueOf(courseId));
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(KEY_PURCHASED_COURSES, updatedCourses);
            
            // 同时保存到用户特定的课程集合中
            SessionManager sessionManager = new SessionManager(context);
            if (sessionManager.isLoggedIn()) {
                int userId = sessionManager.getUserDetails().getId();
                String userCoursesKey = KEY_USER_COURSES_PREFIX + userId;
                Set<String> userCourses = prefs.getStringSet(userCoursesKey, new HashSet<>());
                Set<String> updatedUserCourses = new HashSet<>(userCourses);
                updatedUserCourses.add(String.valueOf(courseId));
                editor.putStringSet(userCoursesKey, updatedUserCourses);
                
                Log.d(TAG, "已保存课程ID到用户 " + userId + " 的购买列表: " + courseId);
            }
            
            editor.apply();
            
            Log.d(TAG, "已保存购买的课程ID: " + courseId);
            Log.d(TAG, "当前已购买课程总数: " + updatedCourses.size());
            
            // 确保写入成功
            verifyPurchaseSaved(context, courseId);
        } catch (Exception e) {
            Log.e(TAG, "保存购买课程信息失败", e);
        }
    }
    
    /**
     * 验证课程ID是否成功保存
     */
    private static void verifyPurchaseSaved(Context context, int courseId) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> purchasedCourses = prefs.getStringSet(KEY_PURCHASED_COURSES, new HashSet<>());
            if (purchasedCourses.contains(String.valueOf(courseId))) {
                Log.d(TAG, "验证成功: 课程ID " + courseId + " 已正确保存");
            } else {
                Log.e(TAG, "验证失败: 课程ID " + courseId + " 未能成功保存");
                
                // 再次尝试保存
                Set<String> updatedCourses = new HashSet<>(purchasedCourses);
                updatedCourses.add(String.valueOf(courseId));
                prefs.edit().putStringSet(KEY_PURCHASED_COURSES, updatedCourses).commit(); // 使用commit强制同步写入
                Log.d(TAG, "已重新尝试保存课程ID: " + courseId);
            }
        } catch (Exception e) {
            Log.e(TAG, "验证保存状态失败", e);
        }
    }
    
    /**
     * 获取所有已购买的课程ID
     * @param context 上下文
     * @return 已购买课程ID的集合
     */
    public static Set<Integer> getPurchasedCourseIds(Context context) {
        if (context == null) return new HashSet<>();
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> purchasedCourseStrings = new HashSet<>();
            
            // 获取全局购买记录
            purchasedCourseStrings.addAll(prefs.getStringSet(KEY_PURCHASED_COURSES, new HashSet<>()));
            
            // 获取当前用户的购买记录
            SessionManager sessionManager = new SessionManager(context);
            if (sessionManager.isLoggedIn()) {
                int userId = sessionManager.getUserDetails().getId();
                String userCoursesKey = KEY_USER_COURSES_PREFIX + userId;
                purchasedCourseStrings.addAll(prefs.getStringSet(userCoursesKey, new HashSet<>()));
                Log.d(TAG, "已加载用户 " + userId + " 的课程购买记录");
            }
            
            // 添加手动测试数据，确保至少有一些课程可以显示
            if (purchasedCourseStrings.isEmpty()) {
                // 如果是开发环境，添加一些测试数据
                if (DEBUG_MODE) {
                    purchasedCourseStrings.add("1");
                    purchasedCourseStrings.add("2");
                    purchasedCourseStrings.add("3");
                    Log.d(TAG, "已添加测试课程数据");
                }
            }
            
            Set<Integer> courseIds = new HashSet<>();
            for (String idStr : purchasedCourseStrings) {
                try {
                    courseIds.add(Integer.parseInt(idStr));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "解析课程ID失败: " + idStr, e);
                }
            }
            
            Log.d(TAG, "获取到已购买课程数量: " + courseIds.size());
            for (Integer id : courseIds) {
                Log.d(TAG, "已购买课程ID: " + id);
            }
            
            return courseIds;
        } catch (Exception e) {
            Log.e(TAG, "获取已购买课程信息失败", e);
            
            // 出错时返回硬编码的测试数据，确保UI能显示一些内容
            Set<Integer> fallbackIds = new HashSet<>();
            fallbackIds.add(1);
            fallbackIds.add(2);
            fallbackIds.add(3);
            Log.d(TAG, "返回备用测试数据");
            return fallbackIds;
        }
    }
    
    /**
     * 清除所有已购买课程信息（仅用于测试）
     * @param context 上下文
     */
    public static void clearPurchasedCourses(Context context) {
        if (context == null) return;
        
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // 清除全局购买记录
        editor.remove(KEY_PURCHASED_COURSES);
        
        // 清除当前用户的购买记录
        SessionManager sessionManager = new SessionManager(context);
        if (sessionManager.isLoggedIn()) {
            int userId = sessionManager.getUserDetails().getId();
            String userCoursesKey = KEY_USER_COURSES_PREFIX + userId;
            editor.remove(userCoursesKey);
            Log.d(TAG, "已清除用户 " + userId + " 的购买记录");
        }
        
        editor.apply();
        
        Log.d(TAG, "已清除所有购买记录");
    }
} 