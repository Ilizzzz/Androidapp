package com.example.yunclass;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.config.AppConfig;
import com.example.yunclass.databinding.ActivityQuestionDetailBinding;
import com.example.yunclass.model.Question;
import com.example.yunclass.model.Reply;
import com.example.yunclass.ui.questions.ReplyAdapter;
import com.example.yunclass.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuestionDetailActivity extends AppCompatActivity {

    private ActivityQuestionDetailBinding binding;
    private ReplyAdapter replyAdapter;
    private int questionId;
    private SimpleDateFormat dateFormat;
    private static final String TAG = "QuestionDetailActivity";
    
    // 添加自动刷新的定时器和处理器
    private static final int REFRESH_INTERVAL = 30000; // 刷新间隔30秒
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private boolean autoRefreshEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 验证用户是否登录
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 获取问题ID
        questionId = getIntent().getIntExtra("question_id", -1);
        if (questionId == -1) {
            Toast.makeText(this, "无效的问题ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化日期格式化器
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("问题详情");
        }

        // 初始化回复列表
        setupRepliesList();
        
        // 设置下拉刷新
        setupSwipeRefresh();
        
        // 初始化自动刷新
        setupAutoRefresh();

        // 设置发送回复按钮点击事件
        binding.sendReplyButton.setOnClickListener(v -> {
            String replyContent = binding.replyEditText.getText().toString().trim();
            if (replyContent.isEmpty()) {
                Toast.makeText(this, "回复内容不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            
            sendReply(replyContent);
        });

        // 加载问题详情
        loadQuestionDetail();
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.accent,
                R.color.primary_dark
        );
        
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            // 手动刷新时重置自动刷新计时器
            resetAutoRefreshTimer();
            loadQuestionDetail();
        });
    }
    
    private void setupAutoRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = () -> {
            Log.d(TAG, "执行自动刷新");
            loadQuestionDetail();
            if (autoRefreshEnabled) {
                refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
            }
        };
        
        // 启动自动刷新
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }
    
    private void resetAutoRefreshTimer() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            if (autoRefreshEnabled) {
                refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
            }
        }
    }

    private void setupRepliesList() {
        replyAdapter = new ReplyAdapter(this, new ArrayList<>());
        binding.repliesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.repliesRecyclerView.setAdapter(replyAdapter);
    }

    private void loadQuestionDetail() {
        // 显示加载中状态（如果不是通过下拉刷新触发的）
        if (!binding.swipeRefreshLayout.isRefreshing()) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        Call<ApiResponse<Question>> call = ApiClient.getApiService().getQuestionDetail(questionId);
        call.enqueue(new Callback<ApiResponse<Question>>() {
            @Override
            public void onResponse(Call<ApiResponse<Question>> call, Response<ApiResponse<Question>> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                
                // 更新最后刷新时间
                updateLastRefreshTime();

                // 调试日志
                if (response.isSuccessful()) {
                    Log.d(TAG, "问题详情API调用成功");
                } else {
                    Log.e(TAG, "问题详情API调用失败: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "错误信息: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "读取错误信息异常", e);
                    }
                }

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Question question = response.body().getQuestion();
                    if (question != null) {
                        displayQuestionDetails(question);
                    } else {
                        Toast.makeText(QuestionDetailActivity.this, "问题详情获取失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "获取问题详情失败";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMessage = response.body().getMessage();
                    }
                    Toast.makeText(QuestionDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Question>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "获取问题详情网络错误", t);
                Toast.makeText(QuestionDetailActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayQuestionDetails(Question question) {
        // 设置问题标题
        binding.questionTitleTextView.setText(question.getTitle());

        // 设置问题内容
        binding.questionContentTextView.setText(question.getContent());

        // 设置提问者
        binding.questionUserTextView.setText(question.getUserName());

        // 设置提问时间
        if (question.getCreatedAt() != null) {
            binding.questionTimeTextView.setText(dateFormat.format(question.getCreatedAt()));
        }

        // 设置问题状态
        if ("answered".equals(question.getStatus())) {
            binding.questionStatusTextView.setText("已回答");
            binding.questionStatusTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            binding.questionStatusTextView.setText("待回答");
            binding.questionStatusTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        }

        // 如果有图片，显示图片
        if (question.getImagePath() != null && !question.getImagePath().isEmpty()) {
            binding.questionImageView.setVisibility(View.VISIBLE);
            String imageUrl = AppConfig.BASE_URL + question.getImagePath();
            Glide.with(this)
                    .load(imageUrl)
                    .centerInside()
                    .into(binding.questionImageView);
        } else {
            binding.questionImageView.setVisibility(View.GONE);
        }

        // 显示回复列表
        if (question.getReplies() != null && !question.getReplies().isEmpty()) {
            binding.noRepliesTextView.setVisibility(View.GONE);
            replyAdapter.setReplies(question.getReplies());
            binding.repliesTitleTextView.setText("全部回复 (" + question.getReplies().size() + ")");
        } else {
            binding.noRepliesTextView.setVisibility(View.VISIBLE);
            binding.repliesTitleTextView.setText("全部回复 (0)");
        }
    }

    private void sendReply(String content) {
        // 显示加载中状态
        binding.progressBar.setVisibility(View.VISIBLE);

        // 创建请求参数
        Map<String, String> replyData = new HashMap<>();
        replyData.put("content", content);

        Call<ApiResponse<Reply>> call = ApiClient.getApiService().replyToQuestion(questionId, replyData);
        call.enqueue(new Callback<ApiResponse<Reply>>() {
            @Override
            public void onResponse(Call<ApiResponse<Reply>> call, Response<ApiResponse<Reply>> response) {
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Reply newReply = response.body().getReply();
                    if (newReply != null) {
                        // 清空输入框
                        binding.replyEditText.setText("");

                        // 添加到回复列表的开头
                        replyAdapter.addReply(newReply);

                        // 隐藏"暂无回复"文本
                        binding.noRepliesTextView.setVisibility(View.GONE);

                        // 更新回复数量
                        int replyCount = replyAdapter.getItemCount();
                        binding.repliesTitleTextView.setText("全部回复 (" + replyCount + ")");

                        // 更新问题状态为已回答
                        binding.questionStatusTextView.setText("已回答");
                        binding.questionStatusTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));

                        // 显示成功消息
                        Toast.makeText(QuestionDetailActivity.this, "回复成功", Toast.LENGTH_SHORT).show();
                        
                        // 发送回复后立即刷新
                        resetAutoRefreshTimer();
                    } else {
                        Toast.makeText(QuestionDetailActivity.this, "回复添加失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "回复发送失败";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMessage = response.body().getMessage();
                    }
                    Toast.makeText(QuestionDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Reply>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "发送回复网络错误", t);
                Toast.makeText(QuestionDetailActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 恢复时启动自动刷新
        autoRefreshEnabled = true;
        resetAutoRefreshTimer();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停时停止自动刷新
        autoRefreshEnabled = false;
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            // 手动刷新
            binding.swipeRefreshLayout.setRefreshing(true);
            loadQuestionDetail();
            return true;
        } else if (item.getItemId() == R.id.action_auto_refresh) {
            // 切换自动刷新状态
            autoRefreshEnabled = !autoRefreshEnabled;
            item.setChecked(autoRefreshEnabled);
            
            if (autoRefreshEnabled) {
                Toast.makeText(this, "自动刷新已开启", Toast.LENGTH_SHORT).show();
                resetAutoRefreshTimer();
            } else {
                Toast.makeText(this, "自动刷新已关闭", Toast.LENGTH_SHORT).show();
                if (refreshHandler != null && refreshRunnable != null) {
                    refreshHandler.removeCallbacks(refreshRunnable);
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_question_detail, menu);
        return true;
    }

    private void updateLastRefreshTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = timeFormat.format(new java.util.Date());
        binding.lastRefreshTextView.setText("最后更新: " + currentTime);
    }
} 