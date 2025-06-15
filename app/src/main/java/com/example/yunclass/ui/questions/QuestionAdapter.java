package com.example.yunclass.ui.questions;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yunclass.R;
import com.example.yunclass.model.Question;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {

    private Context context;
    private List<Question> questions;
    private SimpleDateFormat dateFormat;
    private static final String TAG = "QuestionAdapter";

    public QuestionAdapter(Context context, List<Question> questions) {
        this.context = context;
        this.questions = questions;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questions.get(position);
        
        holder.titleTextView.setText(question.getTitle());
        holder.contentTextView.setText(question.getContent());
        holder.userTextView.setText(question.getUserName());
        
        if (question.getCreatedAt() != null) {
            Log.d(TAG, "Question date: " + question.getCreatedAt());
            holder.timeTextView.setText(dateFormat.format(question.getCreatedAt()));
        } else {
            // 如果日期为空，使用当前时间
            Log.d(TAG, "Question date is null, using current time");
            Date currentDate = new Date();
            holder.timeTextView.setText(dateFormat.format(currentDate));
        }
        
        // 设置状态
        if ("answered".equals(question.getStatus())) {
            holder.statusTextView.setText("已回答");
            holder.statusTextView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
        } else {
            holder.statusTextView.setText("待回答");
            holder.statusTextView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
        }
        
        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            // 跳转到问题详情页
            Intent intent = new Intent(context, com.example.yunclass.QuestionDetailActivity.class);
            intent.putExtra("question_id", question.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return questions != null ? questions.size() : 0;
    }

    public void setQuestions(List<Question> questions) {
        if (questions == null) {
            this.questions = new ArrayList<>();
        } else {
            this.questions = new ArrayList<>(questions);
        }
        notifyDataSetChanged();
        
        // 记录问题数量，便于调试
        Log.d(TAG, "更新问题列表，数量: " + this.questions.size());
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        TextView userTextView;
        TextView timeTextView;
        TextView statusTextView;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.questionTitleTextView);
            contentTextView = itemView.findViewById(R.id.questionContentTextView);
            userTextView = itemView.findViewById(R.id.questionUserTextView);
            timeTextView = itemView.findViewById(R.id.questionTimeTextView);
            statusTextView = itemView.findViewById(R.id.questionStatusTextView);
        }
    }
} 