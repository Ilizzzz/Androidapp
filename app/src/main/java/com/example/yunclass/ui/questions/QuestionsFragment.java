package com.example.yunclass.ui.questions;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.yunclass.AddQuestionActivity;
import com.example.yunclass.R;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.databinding.FragmentQuestionsBinding;
import com.example.yunclass.model.Question;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuestionsFragment extends Fragment {

    private FragmentQuestionsBinding binding;
    private QuestionAdapter adapter;
    private boolean showAllQuestions = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuestionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化列表
        setupQuestionsList();

        // 设置标签切换监听
        setupTabListener();

        // 设置添加问题按钮
        binding.addQuestionFab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddQuestionActivity.class);
            startActivity(intent);
        });

        // 加载问题数据
        loadQuestions();
    }

    private void setupQuestionsList() {
        adapter = new QuestionAdapter(requireContext(), new ArrayList<>());
        binding.questionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.questionsRecyclerView.setAdapter(adapter);
    }

    private void setupTabListener() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                showAllQuestions = (position == 0);
                
                // 清空当前显示的问题列表，以防止显示错误的数据
                adapter.setQuestions(new ArrayList<>());
                
                Log.d("QuestionsFragment", "标签切换: 位置=" + position + 
                        ", 标签文本=" + (position == 0 ? "全部问题" : "我的问题") + 
                        ", showAllQuestions=" + showAllQuestions);
                
                // 重新加载问题数据
                loadQuestions();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void loadQuestions() {
        // 显示加载中状态
        binding.progressBar.setVisibility(View.VISIBLE);
        
        boolean isAllQuestionsTab = binding.tabLayout.getSelectedTabPosition() == 0;
        Log.d("QuestionsFragment", "加载问题，选项卡位置: " + binding.tabLayout.getSelectedTabPosition() + 
                ", 是否为'全部问题'选项卡: " + isAllQuestionsTab);
        
        // 如果是"我的问题"选项卡，使用专用端点
        if (!isAllQuestionsTab) {
            com.example.yunclass.utils.SessionManager sessionManager = new com.example.yunclass.utils.SessionManager(requireContext());
            if (!sessionManager.isLoggedIn()) {
                Log.e("QuestionsFragment", "用户未登录，无法加载我的问题");
                binding.progressBar.setVisibility(View.GONE);
                binding.noQuestionsTextView.setText("请先登录");
                binding.noQuestionsTextView.setVisibility(View.VISIBLE);
                return;
            }
            
            Log.d("QuestionsFragment", "加载我的问题，使用专用API");
            
            Call<ApiResponse<List<Question>>> call = ApiClient.getApiService().getMyQuestions();
            enqueueQuestionCall(call);
        } else {
            // 加载全部问题
            Call<ApiResponse<List<Question>>> call = ApiClient.getApiService().getQuestions("true");
            enqueueQuestionCall(call);
        }
    }
    
    private void enqueueQuestionCall(Call<ApiResponse<List<Question>>> call) {
        call.enqueue(new Callback<ApiResponse<List<Question>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Question>>> call, Response<ApiResponse<List<Question>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                
                // Debug: 打印原始响应内容
                try {
                    if (response.errorBody() != null) {
                        String errorBody = response.errorBody().string();
                        Log.d("QuestionsFragment", "Error Body: " + errorBody);
                    } else if (response.body() != null) {
                        Log.d("QuestionsFragment", "Success Response: " + response.body());
                        if (response.body().getQuestions() != null) {
                            List<Question> questions = response.body().getQuestions();
                            Log.d("QuestionsFragment", "获取到问题数量: " + questions.size());
                            for (Question q : questions) {
                                Log.d("QuestionsFragment", "问题ID: " + q.getId() + 
                                      ", 用户ID: " + q.getUserId() +
                                      ", 用户名: " + q.getUserName() +
                                      ", 标题: " + q.getTitle() +
                                      ", 日期: " + q.getCreatedAt());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("QuestionsFragment", "Debug error: " + e.getMessage());
                }
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Question> questions = response.body().getQuestions();
                    
                    // 确保先隐藏空视图
                    binding.noQuestionsTextView.setVisibility(View.GONE);
                    
                    // 更新RecyclerView
                    if (questions != null && !questions.isEmpty()) {
                        adapter.setQuestions(questions);
                    } else {
                        binding.noQuestionsTextView.setVisibility(View.VISIBLE);
                    }
                } else {
                    // 显示错误信息
                    String errorMessage = response.body() != null ? 
                            response.body().getMessage() : getString(R.string.unknown_error);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    binding.noQuestionsTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Question>>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                
                // Debug: 打印错误
                Log.e("QuestionsFragment", "Network error: " + t.getMessage(), t);
                
                // 显示错误信息
                Toast.makeText(getContext(), getString(R.string.network_error) + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
                binding.noQuestionsTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时刷新数据
        loadQuestions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 