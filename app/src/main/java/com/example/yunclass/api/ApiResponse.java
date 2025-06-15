package com.example.yunclass.api;

import com.example.yunclass.model.Account;
import com.example.yunclass.model.Course;
import com.example.yunclass.model.Order;
import com.example.yunclass.model.Question;
import com.example.yunclass.model.Reply;
import com.example.yunclass.model.User;
import com.example.yunclass.model.Website;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private User user;
    private List<Course> courses;
    private List<Website> websites;
    private Account account;
    private List<Order> orders;
    private Order order;
    private List<Question> questions;
    private Question question;
    private Reply reply;

    @SerializedName("data")
    private T data;

    public ApiResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    public List<Website> getWebsites() {
        return websites;
    }

    public void setWebsites(List<Website> websites) {
        this.websites = websites;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Reply getReply() {
        return reply;
    }

    public void setReply(Reply reply) {
        this.reply = reply;
    }
} 