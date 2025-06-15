package com.example.yunclass.api;

import com.example.yunclass.model.Account;
import com.example.yunclass.model.Course;
import com.example.yunclass.model.Order;
import com.example.yunclass.model.Question;
import com.example.yunclass.model.Reply;
import com.example.yunclass.model.User;
import com.example.yunclass.model.Website;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface ApiService {
    
    @POST("api/login")
    Call<ApiResponse<User>> login(@Body Map<String, String> credentials);
    
    @POST("api/register")
    Call<ApiResponse<Void>> register(@Body Map<String, String> userData);
    
    @POST("api/logout")
    Call<ApiResponse<Void>> logout();
    
    @GET("api/courses")
    Call<ApiResponse<List<Course>>> getCourses();
    
    @GET("api/websites")
    Call<ApiResponse<List<Website>>> getWebsites();
    
    @GET("api/account")
    Call<ApiResponse<Account>> getAccount();
    
    @GET("api/orders")
    Call<ApiResponse<List<Order>>> getOrders();
    
    @POST("api/purchase")
    Call<ApiResponse<Order>> purchaseCourse(@Body Map<String, Object> purchaseData);
    
    @GET("api/questions")
    Call<ApiResponse<List<Question>>> getQuestions(@Query("all") String all, @Query("user_id") Integer userId);
    
    @GET("api/questions")
    Call<ApiResponse<List<Question>>> getQuestions(@Query("all") String all);
    
    @GET("api/my-questions")
    Call<ApiResponse<List<Question>>> getMyQuestions();
    
    @GET("api/questions/{id}")
    Call<ApiResponse<Question>> getQuestionDetail(@Path("id") int questionId);
    
    @Multipart
    @POST("api/questions")
    Call<ApiResponse<Question>> createQuestion(
            @Part("title") RequestBody title,
            @Part("content") RequestBody content,
            @Part MultipartBody.Part image);
    
    @POST("api/questions")
    Call<ApiResponse<Question>> createQuestionWithoutImage(@Body Map<String, String> questionData);
    
    @POST("api/questions/{id}/replies")
    Call<ApiResponse<Reply>> replyToQuestion(
            @Path("id") int questionId,
            @Body Map<String, String> replyData);
            
    /**
     * 获取课程内容文件
     * @param url 文件URL
     * @return 文件响应体
     */
    @Streaming
    @GET
    Call<ResponseBody> downloadFile(@Url String url);
    
    /**
     * 获取课程详情，包括内容类型和路径
     * @param courseId 课程ID
     * @return 课程详情
     */
    @GET("api/courses/{id}")
    Call<ApiResponse<Course>> getCourseDetail(@Path("id") int courseId);
    
    /**
     * 获取课程的子课程列表
     * @param courseId 课程ID
     * @return 子课程列表
     */
    @GET("api/courses/{id}/subcourses")
    Call<ApiResponse<List<Course>>> getSubCourses(@Path("id") int courseId);
} 