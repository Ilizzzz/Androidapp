package com.example.yunclass;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.databinding.ActivityRegisterBinding;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 登录按钮点击事件
        binding.loginTextView.setOnClickListener(v -> {
            finish(); // 返回登录页面
        });

        // 注册按钮点击事件
        binding.registerButton.setOnClickListener(v -> {
            String name = binding.nameEditText.getText().toString().trim();
            String phone = binding.phoneEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            // 简单验证
            if (name.isEmpty()) {
                binding.nameEditText.setError("请输入姓名");
                return;
            }

            if (phone.isEmpty()) {
                binding.phoneEditText.setError("请输入电话");
                return;
            }

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

            // 调用注册API
            register(name, phone, email, password);
        });
    }

    private void register(String name, String phone, String email, String password) {
        Map<String, String> userData = new HashMap<>();
        userData.put("Name", name);
        userData.put("Phone", phone);
        userData.put("email", email);
        userData.put("password", password);

        Call<ApiResponse<Void>> call = ApiClient.getApiService().register(userData);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                        
                        // 返回登录页面
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "注册失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, "注册失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
} 