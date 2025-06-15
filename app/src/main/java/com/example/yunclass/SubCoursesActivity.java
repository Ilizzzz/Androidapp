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

import com.bumptech.glide.Glide;
import com.example.yunclass.adapter.SubCourseAdapter;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.api.ApiResponse;
import com.example.yunclass.config.AppConfig;
import com.example.yunclass.databinding.ActivitySubCoursesBinding;
import com.example.yunclass.model.Course;
import com.example.yunclass.model.CourseContent;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubCoursesActivity extends AppCompatActivity implements SubCourseAdapter.OnSubCourseClickListener {
    private static final String TAG = "SubCoursesActivity";
    private ActivitySubCoursesBinding binding;
    private SubCourseAdapter adapter;
    private int courseId;
    private String courseTitle;
    private String courseDescription;
    private String courseImage;
    private String courseAuthor;
    private String courseDuration;
    private String courseLevel;
    private double courseRating;
    private int courseStudents;
    private double coursePrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySubCoursesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 获取传递的数据
        courseId = getIntent().getIntExtra("course_id", 0);
        courseTitle = getIntent().getStringExtra("course_title");
        courseDescription = getIntent().getStringExtra("course_description");
        courseImage = getIntent().getStringExtra("course_image");
        courseAuthor = getIntent().getStringExtra("course_author");
        courseDuration = getIntent().getStringExtra("course_duration");
        courseLevel = getIntent().getStringExtra("course_level");
        courseRating = getIntent().getDoubleExtra("course_rating", 0);
        courseStudents = getIntent().getIntExtra("course_students", 0);
        coursePrice = getIntent().getDoubleExtra("course_price", 0);
        
        if (courseTitle != null) {
            setTitle(courseTitle + " - 课程目录");
        }

        // 设置课程信息
        setupCourseInfo();

        // 设置RecyclerView
        binding.subCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubCourseAdapter(this, this);
        binding.subCoursesRecyclerView.setAdapter(adapter);

        // 加载子课程数据
        loadSubCourses();
    }

    private void setupCourseInfo() {
        // 设置课程标题
        if (courseTitle != null) {
            binding.courseTitleTextView.setText(courseTitle);
        }

        // 设置课程描述
        if (courseDescription != null) {
            binding.courseDescriptionTextView.setText(courseDescription);
        }

        // 设置作者
        if (courseAuthor != null) {
            binding.courseAuthorTextView.setText(courseAuthor);
        }

        // 设置时长
        if (courseDuration != null) {
            binding.courseDurationTextView.setText(courseDuration);
        }

        // 设置难度级别
        if (courseLevel != null) {
            binding.courseLevelTextView.setText(courseLevel);
        }

        // 设置评分
        if (courseRating > 0) {
            binding.courseRatingTextView.setText(String.format("%.1f分", courseRating));
        }

        // 设置学生数量
        if (courseStudents > 0) {
            binding.courseStudentsTextView.setText(courseStudents + "人");
        }

        // 设置价格
        if (coursePrice > 0) {
            binding.coursePriceTextView.setText(String.format("¥%.2f", coursePrice));
        }

        // 加载课程图片
        if (courseImage != null && !courseImage.isEmpty()) {
            String imageUrl = AppConfig.BASE_URL + "api/course-image/" + courseImage;
            Log.d(TAG, "加载课程图片: " + imageUrl);
            
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(binding.courseImageView);
        } else {
            // 如果没有图片，设置默认图片
            binding.courseImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void loadSubCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyView.setVisibility(View.GONE);

        Call<ApiResponse<List<Course>>> call = ApiClient.getApiService().getSubCourses(courseId);
        call.enqueue(new Callback<ApiResponse<List<Course>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Course>>> call, Response<ApiResponse<List<Course>>> response) {
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Course>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        List<Course> subCourses = apiResponse.getData();
                        if (!subCourses.isEmpty()) {
                            adapter.setSubCourses(subCourses);
                            Log.d(TAG, "加载了 " + subCourses.size() + " 个子课程");
                        } else {
                            showEmptyState();
                        }
                    } else {
                        Log.e(TAG, "API响应失败: " + apiResponse.getMessage());
                        showEmptyState();
                    }
                } else {
                    Log.e(TAG, "网络请求失败: " + response.code());
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Course>>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "网络请求异常", t);
                Toast.makeText(SubCoursesActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        binding.emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSubCourseClick(Course subCourse) {
        Log.d(TAG, "点击子课程: " + subCourse.getTitle() + ", 类型: " + subCourse.getContentType());
        
        if ("pdf".equalsIgnoreCase(subCourse.getContentType())) {
            // 打开PDF
            Intent intent = new Intent(this, PdfViewerActivity.class);
            intent.putExtra("course_title", subCourse.getTitle());
            intent.putExtra("pdf_path", subCourse.getContentPath());
            startActivity(intent);
        } else if ("video".equalsIgnoreCase(subCourse.getContentType())) {
            // 打开视频
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra("course_title", subCourse.getTitle());
            intent.putExtra("video_path", subCourse.getContentPath());
            startActivity(intent);
        } else {
            Toast.makeText(this, "不支持的内容类型: " + subCourse.getContentType(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onContentClick(Course subCourse, CourseContent content) {
        Log.d(TAG, "点击内容: " + subCourse.getTitle() + " - " + content.getLabel() + ", 类型: " + content.getType());
        
        if ("pdf".equalsIgnoreCase(content.getType())) {
            // 打开PDF
            Intent intent = new Intent(this, PdfViewerActivity.class);
            intent.putExtra("course_title", subCourse.getTitle() + " - " + content.getLabel());
            intent.putExtra("pdf_path", content.getPath());
            startActivity(intent);
        } else if ("video".equalsIgnoreCase(content.getType())) {
            // 打开视频
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra("course_title", subCourse.getTitle() + " - " + content.getLabel());
            intent.putExtra("video_path", content.getPath());
            startActivity(intent);
        } else {
            Toast.makeText(this, "不支持的内容类型: " + content.getType(), Toast.LENGTH_SHORT).show();
        }
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