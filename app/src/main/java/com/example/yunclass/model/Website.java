package com.example.yunclass.model;

public class Website {
    private int id;
    private String name;
    private String url;
    private String description;
    private String image;

    public Website() {
    }

    public Website(int id, String name, String url, String description, String image) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.description = description;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
} 