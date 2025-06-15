package com.example.yunclass.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Reply {
    @SerializedName("id")
    private int id;
    
    @SerializedName("question_id")
    private int questionId;
    
    @SerializedName("user_id")
    private int userId;
    
    @SerializedName("user_name")
    private String userName;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("created_at")
    private Date createdAt;

    public Reply() {
    }

    public Reply(int id, int questionId, int userId, String userName, String content, Date createdAt) {
        this.id = id;
        this.questionId = questionId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
} 