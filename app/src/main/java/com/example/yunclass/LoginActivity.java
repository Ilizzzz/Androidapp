package com.example.yunclass;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.config.AppConfig;
import com.example.yunclass.databinding.ActivityLoginBinding;
import com.example.yunclass.model.User;
import com.example.yunclass.utils.NetworkUtils;
import com.example.yunclass.utils.SessionManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 显示当前配置的服务器地址
        Log.d(TAG, "当前服务器地址: " + AppConfig.BASE_URL);
        
        sessionManager = new SessionManager(this);

        // 如果用户已登录，直接进入主页
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "用户已登录，直接进入主页");
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // 注册按钮点击事件
        binding.registerTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // 登录按钮点击事件
        binding.loginButton.setOnClickListener(v -> {
            // 检查网络连接
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "网络连接不可用，请检查网络设置", Toast.LENGTH_LONG).show();
                return;
            }

            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            // 简单验证
            if (email.isEmpty()) {
                binding.emailEditText.setError("请输入邮箱");
                return;
            }

            if (password.isEmpty()) {
                binding.passwordEditText.setError("请输入密码");
                return;
            }

            // 显示进度条
            binding.progressBar.setVisibility(View.VISIBLE);

            // 调用登录API
            login(email, password);
        });
    }

    private void login(String email, String password) {
        Log.d(TAG, "尝试登录: " + email + " 到服务器: " + AppConfig.BASE_URL);
        
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        Call<ApiResponse<User>> call = ApiClient.getApiService().login(credentials);
        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getUser() != null) {
                        // 保存用户会话
                        User user = apiResponse.getUser();
                        Log.d(TAG, "登录成功: " + user.getEmail());
                        sessionManager.createLoginSession(user);
                        
                        Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                        
                        // 跳转到主页
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String message = apiResponse != null ? apiResponse.getMessage() : "未知错误";
                        Log.e(TAG, "登录失败: " + message);
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "登录失败: 响应不成功 " + response.code());
                    
                    // 尝试读取错误响应体
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                            Log.e(TAG, "错误响应体: " + errorBody);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "无法读取错误响应体", e);
                    }
                    
                    // 根据状态码提供更具体的错误信息
                    String errorMessage;
                    switch (response.code()) {
                        case 401:
                            errorMessage = "用户名或密码错误";
                            break;
                        case 404:
                            errorMessage = "服务器API路径不存在";
                            break;
                        case 500:
                            errorMessage = "服务器内部错误";
                            break;
                        default:
                            errorMessage = "登录失败，错误码: " + response.code();
                    }
                    
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "登录请求失败", t);
                
                // 提供更详细的错误信息
                String errorMessage = "网络错误: ";
                if (t.getMessage() != null) {
                    errorMessage += t.getMessage();
                } else {
                    errorMessage += t.getClass().getSimpleName();
                }
                
                Log.e(TAG, errorMessage);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
} 