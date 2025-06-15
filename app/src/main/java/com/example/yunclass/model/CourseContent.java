package com.example.yunclass.model;

public class CourseContent {
    private String type; // "video" or "pdf"
    private String path; // Path to the content file
    private String label; // Display label like "视频讲解" or "课件资料"

    public CourseContent() {
    }

    public CourseContent(String type, String path, String label) {
        this.type = type;
        this.path = path;
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
} 