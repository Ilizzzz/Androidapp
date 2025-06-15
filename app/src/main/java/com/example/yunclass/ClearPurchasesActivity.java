package com.example.yunclass;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yunclass.utils.PurchaseManager;

/**
 * 清除购买记录的Activity
 * 此Activity会自动清除所有购买记录，然后关闭自己
 */
public class ClearPurchasesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_purchases);
        
        // 清除所有购买记录
        PurchaseManager.clearPurchasedCourses(this);
        Toast.makeText(this, "已清除所有购买记录", Toast.LENGTH_LONG).show();
        
        // 延迟1秒后关闭此Activity
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1000);
    }
} 