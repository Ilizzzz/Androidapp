package com.example.yunclass;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.yunclass.adapter.CourseAdapter;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.databinding.ActivityMyCoursesBinding;
import com.example.yunclass.model.Course;
import com.example.yunclass.model.Order;
import com.example.yunclass.model.User;
import com.example.yunclass.utils.DebugUtils;
import com.example.yunclass.utils.PurchaseManager;
import com.example.yunclass.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyCoursesActivity extends AppCompatActivity implements CourseAdapter.OnCourseClickListener {

    private ActivityMyCoursesBinding binding;
    private CourseAdapter adapter;
    private List<Course> allCourses = new ArrayList<>();
    private List<Course> myCourses = new ArrayList<>();
    private Set<Integer> purchasedCourseIds = new HashSet<>();
    private static final String TAG = "MyCoursesActivity";
    private boolean fromPurchase = false;
    private int purchasedCourseId = -1;
    private SessionManager sessionManager;
    private int currentUserId = 0;
    
    // 开发模式标志，开启后显示测试按钮
    private static final boolean DEV_MODE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyCoursesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化SessionManager
        sessionManager = new SessionManager(this);
        
        // 获取当前登录用户ID
        User currentUser = sessionManager.getUserDetails();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
            Log.d(TAG, "当前登录用户ID: " + currentUserId);
        } else {
            Log.e(TAG, "未获取到用户信息");
            Toast.makeText(this, "未获取到用户信息，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("我的课程");
        }

        // 检查是否来自购买页面
        if (getIntent().getExtras() != null) {
            fromPurchase = getIntent().getBooleanExtra("from_purchase", false);
            purchasedCourseId = getIntent().getIntExtra("purchased_course_id", -1);
            
            if (fromPurchase && purchasedCourseId != -1) {
                Log.d(TAG, "来自购买页面，购买的课程ID: " + purchasedCourseId);
                DebugUtils.showDebugToast(this, "从购买页面跳转，课程ID: " + purchasedCourseId);
                
                // 确保将购买的课程ID添加到集合中
                purchasedCourseIds.add(purchasedCourseId);
                
                // 同时保存到持久化存储中
                PurchaseManager.savePurchasedCourse(this, purchasedCourseId);
            }
        }

        // 初始化课程列表
        setupCoursesList();
        
        // 设置测试按钮
        setupTestButtons();

        // 获取已购买的课程
        loadPurchasedCourses();
    }
    
    private void setupTestButtons() {
        // 仅在开发模式下显示测试按钮
        if (DEV_MODE) {
            binding.testButtonsLayout.setVisibility(View.VISIBLE);
            
            // 检查购买按钮
            binding.checkPurchasesButton.setOnClickListener(v -> {
                Set<Integer> storedIds = PurchaseManager.getPurchasedCourseIds(this);
                StringBuilder sb = new StringBuilder();
                sb.append("已购买课程ID (").append(storedIds.size()).append("个):\n");
                for (Integer id : storedIds) {
                    sb.append("- ").append(id).append("\n");
                }
                
                new AlertDialog.Builder(this)
                        .setTitle("购买记录")
                        .setMessage(sb.toString())
                        .setPositiveButton("确定", null)
                        .show();
            });
            
            // 清除购买记录按钮
            binding.clearPurchasesButton.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("确认清除")
                        .setMessage("确定要清除所有购买记录吗？此操作不可恢复。")
                        .setPositiveButton("确认清除", (dialog, which) -> {
                            PurchaseManager.clearPurchasedCourses(this);
                            Toast.makeText(this, "已清除所有购买记录", Toast.LENGTH_SHORT).show();
                            loadPurchasedCourses();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
            
            // 添加测试课程按钮
            binding.checkPurchasesButton.setOnLongClickListener(v -> {
                // 添加测试课程
                addTestCourses();
                return true;
            });
        } else {
            binding.testButtonsLayout.setVisibility(View.GONE);
        }
    }

    // 添加测试课程功能
    private void addTestCourses() {
        // 添加一些测试课程ID到本地存储
        int[] testCourseIds = {1, 2, 3}; // 假设这些是有效的课程ID
        for (int courseId : testCourseIds) {
            PurchaseManager.savePurchasedCourse(this, courseId);
        }
        Toast.makeText(this, "已添加测试课程", Toast.LENGTH_SHORT).show();
        loadPurchasedCourses();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 如果不是从购买页面跳转来的，则刷新数据
        if (!fromPurchase) {
            loadPurchasedCourses();
        } else {
            // 重置标记，以便下次返回时能正常刷新
            fromPurchase = false;
        }
    }

    private void setupCoursesList() {
        adapter = new CourseAdapter(this, new ArrayList<>(), this);
        binding.coursesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.coursesRecyclerView.setAdapter(adapter);
    }

    private void loadPurchasedCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyView.setVisibility(View.GONE);

        Log.d(TAG, "开始加载已购买的课程");
        
        // 初始化 purchasedCourseIds，从本地存储加载所有已购买课程ID
        purchasedCourseIds.clear();
        purchasedCourseIds.addAll(PurchaseManager.getPurchasedCourseIds(this));
        
        if (!purchasedCourseIds.isEmpty()) {
            Log.d(TAG, "从本地存储获取到已购买课程，数量: " + purchasedCourseIds.size());
            for (Integer id : purchasedCourseIds) {
                Log.d(TAG, "本地存储的已购买课程ID: " + id);
            }
        } else {
            Log.d(TAG, "本地存储中没有已购买课程记录，尝试从服务器获取");
        }
        
        // 无论如何都从服务器获取最新订单，以保持数据同步
        loadOrdersFromServer();
    }
    
    private void loadOrdersFromServer() {
        Log.d(TAG, "从服务器获取订单数据，用户ID: " + currentUserId);
        
        // 从API获取所有订单
        Call<ApiResponse<List<Order>>> orderCall = ApiClient.getApiService().getOrders();
        orderCall.enqueue(new Callback<ApiResponse<List<Order>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Order>>> call, Response<ApiResponse<List<Order>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "订单API调用成功: " + response.code());
                    
                    if (response.body().isSuccess()) {
                        List<Order> orders = response.body().getOrders();
                        Log.d(TAG, "获取到订单数量: " + (orders != null ? orders.size() : "null"));
                        
                        if (orders != null && !orders.isEmpty()) {
                            // 只处理当前用户的订单
                            int filteredOrderCount = 0;
                            
                            // 保留本地存储的课程ID，使用服务器数据进行补充而不是替换
                            // purchasedCourseIds.clear(); // 不再清除本地缓存
                            
                            for (Order order : orders) {
                                // 只处理当前登录用户的订单
                                if (order.getUserId() == currentUserId) {
                                    int courseId = order.getCourseId();
                                    purchasedCourseIds.add(courseId);
                                    PurchaseManager.savePurchasedCourse(MyCoursesActivity.this, courseId);
                                    Log.d(TAG, "添加当前用户的课程ID: " + courseId + ", 课程标题: " + order.getCourseTitle());
                                    filteredOrderCount++;
                                    
                                    // 记录订单信息到文件，便于调试
                                    DebugUtils.logOrderInfo(MyCoursesActivity.this, order);
                                }
                            }
                            
                            Log.d(TAG, "当前用户的订单数量: " + filteredOrderCount);
                        } else {
                            Log.w(TAG, "服务器返回的订单列表为空");
                        }
                    } else {
                        if ("未登录".equals(response.body().getMessage())) {
                            Log.w(TAG, "用户未登录或会话已过期，仅使用本地存储的课程数据");
                            // 会话可能已过期，但我们仍然可以使用本地存储的课程ID
                        } else {
                            Log.e(TAG, "API返回成功但状态为失败: " + response.body().getMessage());
                        }
                    }
                } else {
                    Log.e(TAG, "获取订单API返回错误: " + response.code());
                    
                    if (response.code() == 401) {
                        Log.w(TAG, "用户未授权，仅使用本地存储的课程数据");
                        // 会话可能已过期，但我们仍然可以使用本地存储的课程ID
                    }
                }
                
                Log.d(TAG, "综合订单信息后的课程ID总数: " + purchasedCourseIds.size());
                
                // 无论服务器响应如何，都继续加载所有课程
                loadAllCourses();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Order>>> call, Throwable t) {
                Log.e(TAG, "获取订单网络请求失败", t);
                
                // 即使网络请求失败，仍然使用本地存储的课程ID加载课程
                loadAllCourses();
            }
        });
    }

    private void loadAllCourses() {
        Log.d(TAG, "开始加载所有课程");
        
        // 检查是否有购买的课程ID
        if (purchasedCourseIds.isEmpty()) {
            Log.w(TAG, "没有找到已购买的课程ID");
            binding.progressBar.setVisibility(View.GONE);
            binding.emptyView.setVisibility(View.VISIBLE);
            return;
        }
        
        Call<ApiResponse<List<Course>>> courseCall = ApiClient.getApiService().getCourses();
        courseCall.enqueue(new Callback<ApiResponse<List<Course>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Course>>> call, Response<ApiResponse<List<Course>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "课程API调用成功: " + response.code());
                    
                    if (response.body().isSuccess()) {
                        allCourses = response.body().getCourses();
                        Log.d(TAG, "获取到总课程数量: " + (allCourses != null ? allCourses.size() : "null"));
                        
                        if (allCourses != null && !allCourses.isEmpty()) {
                            // 筛选出已购买的课程
                            myCourses.clear();
                            for (Course course : allCourses) {
                                int courseId = course.getId();
                                if (purchasedCourseIds.contains(courseId)) {
                                    Log.d(TAG, "匹配到已购买课程: ID=" + courseId + ", 标题=" + course.getTitle());
                                    myCourses.add(course);
                                }
                            }
                            
                            Log.d(TAG, "筛选后的已购买课程数量: " + myCourses.size());
                            
                            // 更新UI
                            adapter.setCourses(myCourses);
                            
                            // 处理空视图
                            if (myCourses.isEmpty()) {
                                Log.w(TAG, "没有匹配到任何已购买课程");
                                binding.emptyView.setVisibility(View.VISIBLE);
                            } else {
                                binding.emptyView.setVisibility(View.GONE);
                            }
                        } else {
                            Log.w(TAG, "获取到的课程列表为空");
                            binding.emptyView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.e(TAG, "API返回成功但状态为失败: " + response.body().getMessage());
                        binding.emptyView.setVisibility(View.VISIBLE);
                        Toast.makeText(MyCoursesActivity.this, "获取课程失败: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "获取课程API返回错误: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ", " + response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "读取错误响应失败", e);
                        }
                    }
                    Log.e(TAG, errorMsg);
                    
                    binding.emptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(MyCoursesActivity.this, "获取课程失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Course>>> call, Throwable t) {
                Log.e(TAG, "获取课程网络请求失败", t);
                binding.progressBar.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.VISIBLE);
                Toast.makeText(MyCoursesActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCourseClick(Course course) {
        // 跳转到课程详情页
        Intent intent = new Intent(this, CourseDetailActivity.class);
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
        intent.putExtra("content_type", course.getContentType());
        intent.putExtra("content_path", course.getContentPath());
        startActivity(intent);
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