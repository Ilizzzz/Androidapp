package com.example.yunclass.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yunclass.R;
import com.example.yunclass.model.Course;
import com.example.yunclass.model.CourseContent;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SubCourseAdapter extends RecyclerView.Adapter<SubCourseAdapter.SubCourseViewHolder> {

    private List<Course> subCourses;
    private Context context;
    private OnSubCourseClickListener listener;

    // 定义接口用于处理点击事件
    public interface OnSubCourseClickListener {
        void onSubCourseClick(Course subCourse);
        void onContentClick(Course subCourse, CourseContent content);
    }

    public SubCourseAdapter(Context context, OnSubCourseClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.subCourses = new ArrayList<>();
    }

    @NonNull
    @Override
    public SubCourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_course, parent, false);
        return new SubCourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubCourseViewHolder holder, int position) {
        Course subCourse = subCourses.get(position);
        
        holder.titleTextView.setText(subCourse.getTitle());
        holder.descriptionTextView.setText(subCourse.getDescription());
        holder.durationTextView.setText(subCourse.getDuration());
        
        // 检查是否有新版本的多内容支持
        if (subCourse.hasMultipleContents()) {
            // 新版本：显示多个内容选项按钮
            holder.contentOptionsLayout.setVisibility(View.VISIBLE);
            
            // 设置图标为通用的课程图标
            holder.typeIconImageView.setImageResource(R.drawable.ic_school);
            
            // 设置按钮点击事件
            List<CourseContent> contents = subCourse.getContents();
            CourseContent videoContent = null;
            CourseContent pdfContent = null;
            
            // 查找视频和PDF内容
            for (CourseContent content : contents) {
                if ("video".equals(content.getType())) {
                    videoContent = content;
                } else if ("pdf".equals(content.getType())) {
                    pdfContent = content;
                }
            }
            
            // 设置视频按钮
            if (videoContent != null) {
                final CourseContent finalVideoContent = videoContent;
                holder.videoButton.setVisibility(View.VISIBLE);
                holder.videoButton.setText(videoContent.getLabel());
                holder.videoButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onContentClick(subCourse, finalVideoContent);
                    }
                });
            } else {
                holder.videoButton.setVisibility(View.GONE);
            }
            
            // 设置PDF按钮
            if (pdfContent != null) {
                final CourseContent finalPdfContent = pdfContent;
                holder.pdfButton.setVisibility(View.VISIBLE);
                holder.pdfButton.setText(pdfContent.getLabel());
                holder.pdfButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onContentClick(subCourse, finalPdfContent);
                    }
                });
            } else {
                holder.pdfButton.setVisibility(View.GONE);
            }
            
        } else {
            // 兼容旧版本：单一内容类型
            holder.contentOptionsLayout.setVisibility(View.GONE);
            
            // 根据内容类型设置图标
            if ("video".equals(subCourse.getContentType())) {
                holder.typeIconImageView.setImageResource(R.drawable.ic_play_circle);
                holder.typeTextView.setText("视频课程");
            } else if ("pdf".equals(subCourse.getContentType())) {
                holder.typeIconImageView.setImageResource(R.drawable.ic_description);
                holder.typeTextView.setText("PDF文档");
            } else {
                holder.typeIconImageView.setImageResource(R.drawable.ic_help);
                holder.typeTextView.setText("未知类型");
            }

            // 点击事件（兼容旧版本）
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubCourseClick(subCourse);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return subCourses != null ? subCourses.size() : 0;
    }

    public void setSubCourses(List<Course> subCourses) {
        this.subCourses = subCourses != null ? subCourses : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class SubCourseViewHolder extends RecyclerView.ViewHolder {
        ImageView typeIconImageView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView durationTextView;
        TextView typeTextView;
        View contentOptionsLayout;
        MaterialButton videoButton;
        MaterialButton pdfButton;

        public SubCourseViewHolder(@NonNull View itemView) {
            super(itemView);
            typeIconImageView = itemView.findViewById(R.id.typeIconImageView);
            titleTextView = itemView.findViewById(R.id.subCourseTitleTextView);
            descriptionTextView = itemView.findViewById(R.id.subCourseDescriptionTextView);
            durationTextView = itemView.findViewById(R.id.subCourseDurationTextView);
            typeTextView = itemView.findViewById(R.id.subCourseTypeTextView);
            contentOptionsLayout = itemView.findViewById(R.id.contentOptionsLayout);
            videoButton = itemView.findViewById(R.id.videoButton);
            pdfButton = itemView.findViewById(R.id.pdfButton);
        }
    }
} 