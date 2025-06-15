package com.example.yunclass;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.databinding.ActivitySettingsBinding;
import com.example.yunclass.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置");
        }

        sessionManager = new SessionManager(this);

        // 设置各选项的点击事件
        setupClickListeners();
    }

    private void setupClickListeners() {
        // 修改密码
        binding.changePasswordLayout.setOnClickListener(v -> {
            Toast.makeText(this, "修改密码功能正在开发中", Toast.LENGTH_SHORT).show();
            // TODO: 实现修改密码功能
        });

        // 消息通知设置
        binding.notificationLayout.setOnClickListener(v -> {
            Toast.makeText(this, "消息通知设置功能正在开发中", Toast.LENGTH_SHORT).show();
            // TODO: 实现消息通知设置
        });

        // 隐私政策
        binding.privacyPolicyLayout.setOnClickListener(v -> {
            showPrivacyPolicyDialog();
        });

        // 关于我们
        binding.aboutUsLayout.setOnClickListener(v -> {
            showAboutUsDialog();
        });

        // 清除缓存
        binding.clearCacheLayout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("清除缓存")
                    .setMessage("确定要清除应用缓存吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 执行清除缓存操作
                        Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 退出登录
        binding.logoutLayout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("退出登录")
                    .setMessage("确定要退出登录吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 执行退出登录
                        performLogout();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }
    
    private void showAboutUsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("关于我们")
                .setMessage("云课堂应用\n\n开发者：22级大数据1班 李思进\n学号：20228131031\n\n版本：1.0.0\n\n本应用为Android课程设计作品，用于云课堂学习与交流。")
                .setPositiveButton("确定", null)
                .show();
    }
    
    private void showPrivacyPolicyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("隐私政策")
                .setMessage("云课堂隐私政策\n\n1. 信息收集\n我们收集的信息包括：\n· 账号信息（用户名、邮箱等）\n· 设备信息\n· 使用记录\n\n2. 权限说明\n本应用需要以下权限：\n· 网络访问：用于获取课程数据\n· 存储权限：用于保存课程资料\n· 相册读取：用于上传问题图片\n\n3. 信息安全\n我们采取合理措施保护您的个人信息安全。\n\n4. 第三方服务\n我们使用第三方服务来辅助应用功能的实现。\n\n如有任何疑问，请联系我们。")
                .setPositiveButton("我已阅读并同意", null)
                .setNegativeButton("不同意", null)
                .show();
    }

    private void performLogout() {
        Call<ApiResponse<Void>> call = ApiClient.getApiService().logout();
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                // 无论服务器响应如何，都清除本地会话
                sessionManager.logoutUser();
                
                // 跳转到登录页面
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // 即使网络请求失败，也清除本地会话
                sessionManager.logoutUser();
                
                // 跳转到登录页面
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 