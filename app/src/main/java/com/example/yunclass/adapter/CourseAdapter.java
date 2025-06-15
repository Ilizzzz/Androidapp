package com.example.yunclass.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.yunclass.R;
import com.example.yunclass.api.ApiClient;
import com.example.yunclass.config.AppConfig;
import com.example.yunclass.model.Course;

import java.util.ArrayList;
import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private List<Course> courses;
    private List<Course> originalCourses; // 存储原始数据，用于搜索功能
    private Context context;
    private OnCourseClickListener listener;

    // 定义接口用于处理点击事件
    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    public CourseAdapter(Context context, List<Course> courses, OnCourseClickListener listener) {
        this.context = context;
        this.courses = courses;
        this.originalCourses = new ArrayList<>(courses); // 初始化原始数据
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.titleTextView.setText(course.getTitle());
        holder.descriptionTextView.setText(course.getDescription());
        holder.authorTextView.setText(course.getAuthor());
        holder.levelTextView.setText(course.getLevel());
        holder.ratingTextView.setText(course.getRating() + "分");

        // 加载课程图片
        String imageUrl = AppConfig.BASE_URL + "api/course-image/" + course.getImage();
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(holder.imageView);

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCourseClick(course);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courses != null ? courses.size() : 0;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
        this.originalCourses = new ArrayList<>(courses); // 保存原始数据副本
        notifyDataSetChanged();
    }
    
    /**
     * 获取所有原始课程数据，用于搜索功能
     * @return 原始课程列表
     */
    public List<Course> getAllCourses() {
        return originalCourses != null ? originalCourses : new ArrayList<>();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView authorTextView;
        TextView levelTextView;
        TextView ratingTextView;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.courseImageView);
            titleTextView = itemView.findViewById(R.id.courseTitleTextView);
            descriptionTextView = itemView.findViewById(R.id.courseDescriptionTextView);
            authorTextView = itemView.findViewById(R.id.courseAuthorTextView);
            levelTextView = itemView.findViewById(R.id.courseLevelTextView);
            ratingTextView = itemView.findViewById(R.id.courseRatingTextView);
        }
    }
} 