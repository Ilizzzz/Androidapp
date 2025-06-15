package com.example.yunclass;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.config.AppConfig;
import com.example.yunclass.databinding.ActivityCourseDetailBinding;
import com.example.yunclass.model.Course;
import com.example.yunclass.model.Order;
import com.example.yunclass.utils.DebugUtils;
import com.example.yunclass.utils.PurchaseManager;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseDetailActivity extends AppCompatActivity {

    private ActivityCourseDetailBinding binding;
    private int courseId;
    private String courseTitle;
    private double coursePrice;
    private String contentType; // "pdf" or "video"
    private String contentPath; // Path to the content file
    private static final String TAG = "CourseDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 获取课程数据
        if (getIntent().getExtras() != null) {
            courseId = getIntent().getIntExtra("course_id", 0);
            courseTitle = getIntent().getStringExtra("course_title");
            String courseDescription = getIntent().getStringExtra("course_description");
            String courseImage = getIntent().getStringExtra("course_image");
            String courseAuthor = getIntent().getStringExtra("course_author");
            String courseDuration = getIntent().getStringExtra("course_duration");
            String courseLevel = getIntent().getStringExtra("course_level");
            double courseRating = getIntent().getDoubleExtra("course_rating", 0);
            int courseStudents = getIntent().getIntExtra("course_students", 0);
            coursePrice = getIntent().getDoubleExtra("course_price", 0);
            contentType = getIntent().getStringExtra("content_type");
            contentPath = getIntent().getStringExtra("content_path");

            // 显示课程信息
            binding.collapsingToolbar.setTitle(courseTitle);
            binding.courseTitleTextView.setText(courseTitle);
            binding.courseDescriptionTextView.setText(courseDescription);
            binding.courseAuthorTextView.setText(courseAuthor);
            binding.courseDurationTextView.setText(courseDuration);
            binding.courseLevelTextView.setText(courseLevel);
            binding.courseRatingTextView.setText(String.format(Locale.getDefault(), "%.1f分", courseRating));
            binding.courseStudentsTextView.setText(String.format(Locale.getDefault(), "%d人", courseStudents));
            
            // 格式化价格
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.CHINA);
            binding.coursePriceTextView.setText(format.format(coursePrice));

            // 加载课程图片
            String imageUrl = AppConfig.BASE_URL + "api/course-image/" + courseImage;
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(binding.courseImageView);
            
            // 检查课程是否已购买
            if (PurchaseManager.getPurchasedCourseIds(this).contains(courseId)) {
                updateUIForPurchasedCourse();
            }
        }

        // 设置购买按钮点击事件
        binding.enrollButton.setOnClickListener(v -> {
            if (PurchaseManager.getPurchasedCourseIds(this).contains(courseId)) {
                // 如果已购买，则跳转到内容查看器
                startLearning();
            } else {
                // 否则显示购买确认对话框
                showPurchaseConfirmDialog();
            }
        });
    }
    
    private void updateUIForPurchasedCourse() {
        binding.enrollButton.setText("开始学习");
        binding.enrollButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
    }

    private void showPurchaseConfirmDialog() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.CHINA);
        new AlertDialog.Builder(this)
                .setTitle("确认购买")
                .setMessage("您确定要购买「" + courseTitle + "」课程吗？\n价格：" + format.format(coursePrice))
                .setPositiveButton("确认购买", (dialog, which) -> {
                    purchaseCourse();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void purchaseCourse() {
        // 显示进度条
        binding.progressBar.setVisibility(View.VISIBLE);

        // 准备购买数据
        Map<String, Object> purchaseData = new HashMap<>();
        purchaseData.put("courseId", courseId);
        purchaseData.put("courseTitle", courseTitle);
        purchaseData.put("price", coursePrice);

        Log.d(TAG, "发送购买请求：课程ID=" + courseId + ", 标题=" + courseTitle + ", 价格=" + coursePrice);
        DebugUtils.writeToFile(this, "purchase_request.txt", 
                "购买请求：\n课程ID=" + courseId + "\n标题=" + courseTitle + "\n价格=" + coursePrice);

        // 发送购买请求
        Call<ApiResponse<Order>> call = ApiClient.getApiService().purchaseCourse(purchaseData);
        call.enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Order> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Order order = apiResponse.getOrder();
                        if (order != null) {
                            Log.d(TAG, "购买成功：订单ID=" + order.getId() + 
                                   ", 课程ID=" + order.getCourseId() + 
                                   ", 课程标题=" + order.getCourseTitle());
                            
                            // 记录订单信息到文件
                            DebugUtils.logOrderInfo(CourseDetailActivity.this, order);
                            
                            // 保存课程ID以确保能在"我的课程"中显示
                            DebugUtils.writeToFile(CourseDetailActivity.this, "purchased_course_" + order.getCourseId() + ".txt",
                                    "课程ID: " + order.getCourseId() + "\n" +
                                    "课程标题: " + order.getCourseTitle() + "\n" +
                                    "购买时间: " + (order.getCreatedAt() != null ? order.getCreatedAt().toString() : "未知"));
                            
                            // 使用 PurchaseManager 保存购买记录
                            PurchaseManager.savePurchasedCourse(CourseDetailActivity.this, order.getCourseId());
                        } else {
                            Log.w(TAG, "购买成功但订单对象为null");
                            // 即使订单对象为null，也保存当前课程ID
                            PurchaseManager.savePurchasedCourse(CourseDetailActivity.this, courseId);
                        }
                        
                        showPurchaseSuccessDialog();
                    } else {
                        Log.e(TAG, "购买失败：" + apiResponse.getMessage());
                        Toast.makeText(CourseDetailActivity.this, "购买失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "购买请求失败：" + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "错误详情：" + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "无法读取错误详情", e);
                        }
                    }
                    Toast.makeText(CourseDetailActivity.this, "购买失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Order>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "购买请求网络错误", t);
                Toast.makeText(CourseDetailActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPurchaseSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("购买成功")
                .setMessage("您已成功购买「" + courseTitle + "」课程！")
                .setPositiveButton("查看我的课程", (dialog, which) -> {
                    // 跳转到我的课程页面
                    Intent intent = new Intent(CourseDetailActivity.this, MyCoursesActivity.class);
                    intent.putExtra("from_purchase", true);  // 标记来自购买成功页面
                    intent.putExtra("purchased_course_id", courseId);  // 传递刚购买的课程ID
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("继续浏览", (dialog, which) -> {
                    // 更新UI，显示已购买状态
                    updateUIForPurchasedCourse();
                })
                .setCancelable(false) // 防止用户通过返回键关闭对话框
                .show();
    }

    private void startLearning() {
        Log.d(TAG, "开始学习课程: " + courseTitle + ", 类型: " + contentType + ", 路径: " + contentPath);
        
        // 首先检查课程是否有子课程
        checkForSubCourses();
    }
    
    private void checkForSubCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        Call<ApiResponse<List<Course>>> call = ApiClient.getApiService().getSubCourses(courseId);
        call.enqueue(new Callback<ApiResponse<List<Course>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Course>>> call, Response<ApiResponse<List<Course>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Course>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
                        // 有子课程，跳转到子课程列表
                        Intent intent = new Intent(CourseDetailActivity.this, SubCoursesActivity.class);
                        intent.putExtra("course_id", courseId);
                        intent.putExtra("course_title", courseTitle);
                        intent.putExtra("course_description", getIntent().getStringExtra("course_description"));
                        intent.putExtra("course_image", getIntent().getStringExtra("course_image"));
                        intent.putExtra("course_author", getIntent().getStringExtra("course_author"));
                        intent.putExtra("course_duration", getIntent().getStringExtra("course_duration"));
                        intent.putExtra("course_level", getIntent().getStringExtra("course_level"));
                        intent.putExtra("course_rating", getIntent().getDoubleExtra("course_rating", 0));
                        intent.putExtra("course_students", getIntent().getIntExtra("course_students", 0));
                        intent.putExtra("course_price", getIntent().getDoubleExtra("course_price", 0));
                        startActivity(intent);
                    } else {
                        // 没有子课程，直接播放内容
                        playDirectContent();
                    }
                } else {
                    // 请求失败，直接播放内容
                    Log.w(TAG, "获取子课程失败，直接播放内容");
                    playDirectContent();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Course>>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.w(TAG, "获取子课程网络错误，直接播放内容", t);
                playDirectContent();
            }
        });
    }
    
    private void playDirectContent() {
        // 根据课程ID和内容类型决定打开PDF还是视频
        if (courseTitle != null && courseTitle.contains("数据结构与算法")) {
            // 数据结构与算法课程，始终使用视频
            Log.d(TAG, "检测到数据结构与算法课程，使用视频播放");
            openVideo();
        } else if (courseId == 1) {
            // ID为1的课程，使用视频
            openVideo();
        } else if (courseId == 2) {
            // Python人工智能入门课程，使用PDF
            openPdf();
        } else if (contentType != null) {
            // 其他课程根据内容类型决定
            if (contentType.equalsIgnoreCase("pdf")) {
                openPdf();
            } else if (contentType.equalsIgnoreCase("video")) {
                openVideo();
            } else {
                Toast.makeText(this, "不支持的内容类型: " + contentType, Toast.LENGTH_SHORT).show();
            }
        } else {
            // 默认为PDF，兼容旧版本
            openPdf();
        }
    }
    
    private void openPdf() {
        String pdfPath;
        
        // 如果有指定的内容路径，则使用它
        if (contentPath != null && !contentPath.isEmpty()) {
            pdfPath = contentPath;
        } else {
            // 否则使用默认路径（兼容旧版本）
            pdfPath = "html/book/python.pdf"; // 默认使用python.pdf
        }
        
        Log.d(TAG, "开始学习课程: " + courseId + ", PDF文件: " + pdfPath);
        
        // 跳转到PDF查看器
        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra("course_title", courseTitle);
        intent.putExtra("pdf_path", pdfPath);
        startActivity(intent);
    }
    
    private void openVideo() {
        String videoPath;
        
        // 如果有指定的内容路径，则使用它
        if (contentPath != null && !contentPath.isEmpty()) {
            videoPath = contentPath;
        } else {
            // 否则使用默认路径（兼容旧版本）
            videoPath = "html/book/堆.mp4"; // 默认使用堆.mp4
        }
        
        Log.d(TAG, "开始学习课程: " + courseId + ", 视频文件: " + videoPath);
        
        // 跳转到视频播放器
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("course_title", courseTitle);
        intent.putExtra("video_path", videoPath);
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