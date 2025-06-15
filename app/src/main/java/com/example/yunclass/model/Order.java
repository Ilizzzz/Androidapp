package com.example.yunclass.model;

import java.util.Date;

public class Order {
    private int id;
    private int userId;
    private int courseId;
    private String courseTitle;
    private double price;
    private String status;
    private Date createdAt;

    public Order() {
    }

    public Order(int id, int userId, int courseId, String courseTitle, double price, String status, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
} 