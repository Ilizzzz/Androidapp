package com.example.yunclass;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.databinding.ActivityAddQuestionBinding;
import com.example.yunclass.model.Question;
import com.example.yunclass.utils.FileUtils;
import com.example.yunclass.utils.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddQuestionActivity extends AppCompatActivity {

    private ActivityAddQuestionBinding binding;
    private Uri selectedImageUri = null;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private SessionManager sessionManager;
    
    // 定义需要的权限列表
    private String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    // 在Android 13及以上版本使用新的存储权限
    private String[] REQUIRED_PERMISSIONS_API_33 = {
        Manifest.permission.READ_MEDIA_IMAGES
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        sessionManager = new SessionManager(this);
        
        // 确保用户已登录
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 设置选择图片按钮点击事件
        binding.selectImageButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                openImagePicker();
            } else {
                requestPermissions();
            }
        });

        // 设置移除图片按钮点击事件
        binding.removeImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            binding.questionImageView.setVisibility(View.GONE);
            binding.removeImageButton.setVisibility(View.GONE);
        });

        // 设置提交按钮点击事件
        binding.submitButton.setOnClickListener(v -> {
            submitQuestion();
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS_API_33, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                openImagePicker();
            } else {
                showPermissionExplanationDialog();
            }
        }
    }
    
    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("需要存储权限")
            .setMessage("应用需要存储权限来选择图片。请在设置中授予权限。")
            .setPositiveButton("去设置", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
            .create()
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            binding.questionImageView.setVisibility(View.VISIBLE);
            binding.removeImageButton.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(selectedImageUri)
                    .into(binding.questionImageView);
        }
    }

    private void submitQuestion() {
        String title = binding.titleEditText.getText().toString().trim();
        String content = binding.contentEditText.getText().toString().trim();

        // 验证输入
        if (title.isEmpty()) {
            binding.titleEditText.setError("请输入标题");
            return;
        }

        if (content.isEmpty()) {
            binding.contentEditText.setError("请输入问题描述");
            return;
        }

        // 显示进度条
        binding.progressBar.setVisibility(View.VISIBLE);

        if (selectedImageUri != null) {
            // 有图片，使用Multipart表单提交
            submitQuestionWithImage(title, content);
        } else {
            // 无图片，直接提交文本
            submitQuestionWithoutImage(title, content);
        }
    }

    private void submitQuestionWithImage(String title, String content) {
        // 获取图片文件路径
        String imagePath = FileUtils.getPathFromUri(this, selectedImageUri);
        if (imagePath == null) {
            Toast.makeText(this, "无法获取图片路径", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        File imageFile = new File(imagePath);
        
        // 确保文件存在且可读
        if (!imageFile.exists() || !imageFile.canRead()) {
            Toast.makeText(this, "图片文件不可读", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            return;
        }
        
        // 创建文本请求体
        RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody contentPart = RequestBody.create(MediaType.parse("text/plain"), content);
        
        // 创建图片请求体 - 使用图片专用的 MediaType
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        // 改用带有正确参数的API调用
        Call<ApiResponse<Question>> call = ApiClient.getApiService().createQuestion(titlePart, contentPart, imagePart);
        call.enqueue(new Callback<ApiResponse<Question>>() {
            @Override
            public void onResponse(Call<ApiResponse<Question>> call, Response<ApiResponse<Question>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(AddQuestionActivity.this, "问题提交成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = "提交失败";
                    if (response.code() == 401) {
                        errorMsg = "未登录，请重新登录";
                        // 跳转到登录页面
                        sessionManager.logoutUser();
                        startActivity(new Intent(AddQuestionActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        errorMsg = "提交失败: " + (response.body() != null ? response.body().getMessage() : "未知错误") + 
                                 " (状态码: " + response.code() + ")";
                    }
                    Toast.makeText(AddQuestionActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Question>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AddQuestionActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitQuestionWithoutImage(String title, String content) {
        Map<String, String> questionData = new HashMap<>();
        questionData.put("title", title);
        questionData.put("content", content);

        Call<ApiResponse<Question>> call = ApiClient.getApiService().createQuestionWithoutImage(questionData);
        call.enqueue(new Callback<ApiResponse<Question>>() {
            @Override
            public void onResponse(Call<ApiResponse<Question>> call, Response<ApiResponse<Question>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(AddQuestionActivity.this, "问题提交成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = "提交失败";
                    if (response.code() == 401) {
                        errorMsg = "未登录，请重新登录";
                        // 跳转到登录页面
                        sessionManager.logoutUser();
                        startActivity(new Intent(AddQuestionActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        errorMsg = "提交失败: " + (response.body() != null ? response.body().getMessage() : "未知错误");
                    }
                    Toast.makeText(AddQuestionActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Question>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AddQuestionActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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