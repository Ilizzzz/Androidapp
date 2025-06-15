package com.example.yunclass.model;

import java.util.List;

public class Course {
    private int id;
    private String title;
    private String description;
    private String image;
    private String author;
    private String duration;
    private String level;
    private double rating;
    private int students;
    private double price;
    
    // 兼容旧版本的字段
    private String contentType; // "pdf" or "video"
    private String contentPath; // Path to the content file
    
    // 新版本的多内容支持
    private List<CourseContent> contents; // 多个内容选项（视频+PDF）
    
    // 新增字段：子课程支持
    private List<Course> subCourses; // 子课程列表
    private boolean isSubCourse; // 是否为子课程
    private int parentCourseId; // 父课程ID（仅子课程使用）

    public Course() {
    }

    public Course(int id, String title, String description, String image, String author, String duration, String level, double rating, int students, double price, String contentType, String contentPath) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.image = image;
        this.author = author;
        this.duration = duration;
        this.level = level;
        this.rating = rating;
        this.students = students;
        this.price = price;
        this.contentType = contentType;
        this.contentPath = contentPath;
        this.isSubCourse = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getStudents() {
        return students;
    }

    public void setStudents(int students) {
        this.students = students;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getContentPath() {
        return contentPath;
    }
    
    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }
    
    // 新版本多内容支持的getter和setter
    public List<CourseContent> getContents() {
        return contents;
    }
    
    public void setContents(List<CourseContent> contents) {
        this.contents = contents;
    }
    
    // 辅助方法：检查是否有多个内容选项
    public boolean hasMultipleContents() {
        return contents != null && !contents.isEmpty();
    }
    
    // 新增的getter和setter方法
    public List<Course> getSubCourses() {
        return subCourses;
    }
    
    public void setSubCourses(List<Course> subCourses) {
        this.subCourses = subCourses;
    }
    
    public boolean isSubCourse() {
        return isSubCourse;
    }
    
    public void setSubCourse(boolean subCourse) {
        isSubCourse = subCourse;
    }
    
    public int getParentCourseId() {
        return parentCourseId;
    }
    
    public void setParentCourseId(int parentCourseId) {
        this.parentCourseId = parentCourseId;
    }
    
    // 辅助方法：检查是否有子课程
    public boolean hasSubCourses() {
        return subCourses != null && !subCourses.isEmpty();
    }
} 