package com.example.yunclass.ui.questions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yunclass.R;
import com.example.yunclass.model.Reply;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {
    
    private final Context context;
    private List<Reply> replies;
    private final SimpleDateFormat dateFormat;
    
    public ReplyAdapter(Context context, List<Reply> replies) {
        this.context = context;
        this.replies = replies != null ? new ArrayList<>(replies) : new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Reply reply = replies.get(position);
        
        // 设置用户名
        holder.userTextView.setText(reply.getUserName());
        
        // 设置回复内容
        holder.contentTextView.setText(reply.getContent());
        
        // 设置时间
        if (reply.getCreatedAt() != null) {
            holder.timeTextView.setText(dateFormat.format(reply.getCreatedAt()));
        } else {
            holder.timeTextView.setText(dateFormat.format(new Date()));
        }
    }
    
    @Override
    public int getItemCount() {
        return replies.size();
    }
    
    public void setReplies(List<Reply> replies) {
        this.replies = replies != null ? new ArrayList<>(replies) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addReply(Reply reply) {
        if (reply != null) {
            this.replies.add(0, reply);
            notifyItemInserted(0);
        }
    }
    
    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView userTextView;
        TextView contentTextView;
        TextView timeTextView;
        
        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            userTextView = itemView.findViewById(R.id.replyUserTextView);
            contentTextView = itemView.findViewById(R.id.replyContentTextView);
            timeTextView = itemView.findViewById(R.id.replyTimeTextView);
        }
    }
} 