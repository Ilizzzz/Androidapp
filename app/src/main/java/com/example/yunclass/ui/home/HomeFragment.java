package com.example.yunclass.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.yunclass.CourseDetailActivity;
import com.example.yunclass.adapter.CourseAdapter;
import com.example.yunclass.adapter.WebsiteAdapter;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.databinding.FragmentHomeBinding;
import com.example.yunclass.model.Course;
import com.example.yunclass.model.Website;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private FragmentHomeBinding binding;
    private WebsiteAdapter websiteAdapter;
    private CourseAdapter courseAdapter;
    private CourseAdapter searchAdapter; // 搜索结果适配器
    private boolean isInSearchMode = false; // 是否处于搜索模式

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        
        // 添加点击监听器，点击空白区域退出搜索
        binding.getRoot().setOnClickListener(v -> {
            if (isInSearchMode) {
                exitSearchMode();
            }
        });
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化搜索功能
        setupSearch();
        
        // 初始化网站轮播
        setupWebsiteCarousel();

        // 初始化课程列表
        setupCourseList();

        // 加载数据
        loadWebsites();
        loadCourses();
    }

    private void setupSearch() {
        // 设置搜索按钮点击事件
        binding.searchButton.setOnClickListener(v -> performSearch());
        
        // 设置输入法搜索动作
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
        
        // 初始化搜索结果适配器
        searchAdapter = new CourseAdapter(requireContext(), new ArrayList<>(), this);
        binding.searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.searchResultsRecyclerView.setAdapter(searchAdapter);
        
        // 点击搜索输入框时，如果不是搜索模式则进入搜索模式
        binding.searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !isInSearchMode) {
                enterSearchMode();
            }
        });
        
        // 防止搜索结果区域的点击事件传递到根视图
        binding.searchResultsContainer.setOnClickListener(v -> {
            // 消费点击事件，防止传递
        });
    }
    
    private void performSearch() {
        String query = binding.searchEditText.getText().toString().trim();
        
        // 进入搜索模式
        if (!isInSearchMode) {
            enterSearchMode();
        }
        
        if (query.isEmpty()) {
            // 查询为空，显示所有课程
            searchAdapter.setCourses(courseAdapter.getAllCourses());
            binding.noResultsTextView.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "已显示所有课程", Toast.LENGTH_SHORT).show();
        } else {
            // 隐藏键盘
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(binding.searchEditText.getWindowToken(), 0);
            
            // 在当前加载的课程中进行过滤
            List<Course> allCourses = courseAdapter.getAllCourses();
            List<Course> filteredCourses = new ArrayList<>();
            
            // 不区分大小写的搜索
            String lowerQuery = query.toLowerCase();
            
            for (Course course : allCourses) {
                if (course.getTitle().toLowerCase().contains(lowerQuery) || 
                    course.getAuthor().toLowerCase().contains(lowerQuery) ||
                    (course.getDescription() != null && course.getDescription().toLowerCase().contains(lowerQuery))) {
                    filteredCourses.add(course);
                }
            }
            
            if (filteredCourses.isEmpty()) {
                // 没有结果，显示"暂无结果"提示
                binding.noResultsTextView.setVisibility(View.VISIBLE);
                searchAdapter.setCourses(new ArrayList<>());
            } else {
                // 有结果，更新列表
                binding.noResultsTextView.setVisibility(View.GONE);
                searchAdapter.setCourses(filteredCourses);
                Toast.makeText(requireContext(), "找到 " + filteredCourses.size() + " 个匹配课程", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enterSearchMode() {
        isInSearchMode = true;
        // 隐藏轮播和背景图区域
        binding.bannerContainer.setVisibility(View.GONE);
        binding.websiteContainer.setVisibility(View.GONE);
        binding.coursesContainer.setVisibility(View.GONE);
        
        // 显示搜索结果区域
        binding.searchResultsContainer.setVisibility(View.VISIBLE);
    }
    
    private void exitSearchMode() {
        isInSearchMode = false;
        // 显示轮播和背景图区域
        binding.bannerContainer.setVisibility(View.VISIBLE);
        binding.websiteContainer.setVisibility(View.VISIBLE);
        binding.coursesContainer.setVisibility(View.VISIBLE);
        
        // 隐藏搜索结果区域
        binding.searchResultsContainer.setVisibility(View.GONE);
        
        // 清空搜索框
        binding.searchEditText.setText("");
        
        // 隐藏键盘
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.searchEditText.getWindowToken(), 0);
        
        // 移除搜索框焦点
        binding.searchEditText.clearFocus();
    }

    private void setupWebsiteCarousel() {
        websiteAdapter = new WebsiteAdapter(requireContext(), new ArrayList<>());
        binding.websiteViewPager.setAdapter(websiteAdapter);
    }

    private void setupCourseList() {
        courseAdapter = new CourseAdapter(requireContext(), new ArrayList<>(), this);
        binding.coursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.coursesRecyclerView.setAdapter(courseAdapter);
    }

    private void loadWebsites() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        Call<ApiResponse<List<Website>>> call = ApiClient.getApiService().getWebsites();
        call.enqueue(new Callback<ApiResponse<List<Website>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Website>>> call, Response<ApiResponse<List<Website>>> response) {
                if (isAdded()) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Website>> apiResponse = response.body();
                        if (apiResponse.isSuccess() && apiResponse.getWebsites() != null) {
                            websiteAdapter.setWebsites(apiResponse.getWebsites());
                        }
                    } else {
                        Toast.makeText(requireContext(), "加载网站数据失败", Toast.LENGTH_SHORT).show();
                    }
                    
                    // 如果课程也加载完成，隐藏进度条
                    if (courseAdapter.getItemCount() > 0) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Website>>> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // 如果课程已加载完成，隐藏进度条
                    if (courseAdapter.getItemCount() > 0) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void loadCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        Call<ApiResponse<List<Course>>> call = ApiClient.getApiService().getCourses();
        call.enqueue(new Callback<ApiResponse<List<Course>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Course>>> call, Response<ApiResponse<List<Course>>> response) {
                if (isAdded()) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Course>> apiResponse = response.body();
                        if (apiResponse.isSuccess() && apiResponse.getCourses() != null) {
                            courseAdapter.setCourses(apiResponse.getCourses());
                        }
                    } else {
                        Toast.makeText(requireContext(), "加载课程数据失败", Toast.LENGTH_SHORT).show();
                    }
                    
                    // 如果网站也加载完成，隐藏进度条
                    if (websiteAdapter.getItemCount() > 0) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Course>>> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // 如果网站已加载完成，隐藏进度条
                    if (websiteAdapter.getItemCount() > 0) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    @Override
    public void onCourseClick(Course course) {
        // 跳转到课程详情页
        Intent intent = new Intent(requireContext(), CourseDetailActivity.class);
        intent.putExtra("course_id", course.getId());
        intent.putExtra("course_title", course.getTitle());
        intent.putExtra("course_description", course.getDescription());
        intent.putExtra("course_image", course.getImage());
        intent.putExtra("course_author", course.getAuthor());
        intent.putExtra("course_duration", course.getDuration());
        intent.putExtra("course_level", course.getLevel());
        intent.putExtra("course_rating", course.getRating());
        intent.putExtra("course_students", course.getStudents());
        intent.putExtra("course_price", course.getPrice());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 