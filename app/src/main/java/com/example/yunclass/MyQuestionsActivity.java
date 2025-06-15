package com.example.yunclass;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.yunclass.ui.questions.QuestionAdapter;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.databinding.ActivityMyQuestionsBinding;
import com.example.yunclass.model.Question;
import com.example.yunclass.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyQuestionsActivity extends AppCompatActivity {

    private ActivityMyQuestionsBinding binding;
    private QuestionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 验证用户是否已登录
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 获取当前登录用户ID
        int currentUserId = sessionManager.getUserDetails().getId();
        Log.d("MyQuestionsActivity", "当前登录用户ID: " + currentUserId);

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("我的问题");
        }

        // 初始化问题列表
        setupQuestionsList();

        // 设置添加问题按钮
        binding.addQuestionFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddQuestionActivity.class);
            startActivity(intent);
        });

        // 加载问题数据
        loadQuestions();
    }

    private void setupQuestionsList() {
        adapter = new QuestionAdapter(this, new ArrayList<>());
        binding.questionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.questionsRecyclerView.setAdapter(adapter);
    }

    private void loadQuestions() {
        // 显示加载中状态
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.noQuestionsTextView.setVisibility(View.GONE);
        
        // 获取当前用户ID
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Log.e("MyQuestionsActivity", "用户未登录，无法加载问题");
            binding.progressBar.setVisibility(View.GONE);
            binding.noQuestionsTextView.setText("请先登录");
            binding.noQuestionsTextView.setVisibility(View.VISIBLE);
            return;
        }
        
        // 使用专用端点获取我的问题
        Log.d("MyQuestionsActivity", "开始获取我的问题，使用专用API");
        
        Call<ApiResponse<List<Question>>> call = ApiClient.getApiService().getMyQuestions();
        call.enqueue(new Callback<ApiResponse<List<Question>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Question>>> call, Response<ApiResponse<List<Question>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                
                // Debug: 打印原始响应内容
                try {
                    Log.d("MyQuestionsActivity", "响应代码: " + response.code());
                    Log.d("MyQuestionsActivity", "响应消息: " + response.message());
                    Log.d("MyQuestionsActivity", "是否成功: " + response.isSuccessful());
                    
                    if (response.errorBody() != null) {
                        String errorBody = response.errorBody().string();
                        Log.e("MyQuestionsActivity", "Error Body: " + errorBody);
                    } 
                    
                    if (response.body() != null) {
                        Log.d("MyQuestionsActivity", "响应体不为空");
                        Log.d("MyQuestionsActivity", "success值: " + response.body().isSuccess());
                        
                        if (response.body().getMessage() != null) {
                            Log.d("MyQuestionsActivity", "消息: " + response.body().getMessage());
                        }
                        
                        if (response.body().getQuestions() != null) {
                            List<Question> questions = response.body().getQuestions();
                            Log.d("MyQuestionsActivity", "获取到问题数量: " + questions.size());
                            
                            for (Question q : questions) {
                                Log.d("MyQuestionsActivity", "问题ID: " + q.getId() + 
                                    ", 用户ID: " + q.getUserId() +
                                    ", 用户名: " + q.getUserName() +
                                    ", 标题: " + q.getTitle());
                            }
                        } else {
                            Log.e("MyQuestionsActivity", "问题列表为null");
                        }
                    } else {
                        Log.e("MyQuestionsActivity", "响应体为null");
                    }
                } catch (Exception e) {
                    Log.e("MyQuestionsActivity", "解析响应时出错: " + e.getMessage(), e);
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    boolean isSuccess = response.body().isSuccess();
                    Log.d("MyQuestionsActivity", "API调用成功，success值: " + isSuccess);
                    
                    if (isSuccess) {
                        List<Question> questions = response.body().getQuestions();
                        
                        // 确保先隐藏空视图
                        binding.noQuestionsTextView.setVisibility(View.GONE);
                        
                        // 更新RecyclerView
                        if (questions != null && !questions.isEmpty()) {
                            adapter.setQuestions(questions);
                            Log.d("MyQuestionsActivity", "更新问题列表，显示 " + questions.size() + " 个问题");
                        } else {
                            binding.noQuestionsTextView.setText("暂无问题");
                            binding.noQuestionsTextView.setVisibility(View.VISIBLE);
                            Log.d("MyQuestionsActivity", "问题列表为空，显示空视图");
                        }
                    } else {
                        // API调用成功但操作失败
                        String errorMessage = response.body().getMessage() != null ? 
                                response.body().getMessage() : getString(R.string.unknown_error);
                        binding.noQuestionsTextView.setText(errorMessage);
                        binding.noQuestionsTextView.setVisibility(View.VISIBLE);
                        Log.e("MyQuestionsActivity", "API操作失败: " + errorMessage);
                    }
                } else {
                    // 响应错误
                    String errorMessage = "";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e("MyQuestionsActivity", "读取错误响应失败", e);
                    }
                    
                    if (errorMessage.isEmpty()) {
                        errorMessage = getString(R.string.unknown_error);
                    }
                    
                    binding.noQuestionsTextView.setText(errorMessage);
                    binding.noQuestionsTextView.setVisibility(View.VISIBLE);
                    Log.e("MyQuestionsActivity", "API调用失败: " + errorMessage);
                    Toast.makeText(MyQuestionsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Question>>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                
                // Debug: 打印错误
                Log.e("MyQuestionsActivity", "Network error: " + t.getMessage(), t);
                
                // 显示错误信息
                Toast.makeText(MyQuestionsActivity.this, getString(R.string.network_error) + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
                binding.noQuestionsTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次恢复时刷新数据
        loadQuestions();
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