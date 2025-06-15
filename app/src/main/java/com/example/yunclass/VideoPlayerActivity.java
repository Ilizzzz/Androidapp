package com.example.yunclass;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.yunclass.config.AppConfig;
import com.example.yunclass.databinding.ActivityVideoPlayerBinding;

import java.util.ArrayList;

public class VideoPlayerActivity extends AppCompatActivity {

    private ActivityVideoPlayerBinding binding;
    private static final String TAG = "VideoPlayerActivity";
    private String videoPath;
    private String courseTitle;
    private ExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 获取课程信息
        if (getIntent().getExtras() != null) {
            courseTitle = getIntent().getStringExtra("course_title");
            videoPath = getIntent().getStringExtra("video_path");
            
            // 设置标题
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(courseTitle);
            }
            
            // 显示视频
            if (videoPath != null && !videoPath.isEmpty()) {
                binding.progressBar.setVisibility(View.VISIBLE);
                
                // 测试多种URL格式
                testMultipleUrls();
                
            } else {
                showError("找不到视频文件路径");
            }
        } else {
            showError("未提供课程信息");
        }
    }
    
    // 测试多种URL格式
    private void testMultipleUrls() {
        new Thread(() -> {
            // 获取基础URL和视频路径
            String baseUrl = AppConfig.BASE_URL; // http://10.163.44.11:3000/
            Log.d(TAG, "BASE_URL: " + baseUrl);
            
            // 测试不同的URL组合
            ArrayList<String> testUrls = new ArrayList<>();
            
            // 根据Node.js静态文件托管的规则，正确的URL应该是:
            // http://10.163.44.11:3000/book/dui.mp4
            
            // 首先测试可能性最高的URL格式
            testUrls.add("http://10.163.44.11:3000/book/dui.mp4"); // 如果express.static('html')，应该是/book/dui.mp4
            
            // 其他可能的组合
            testUrls.add(baseUrl + "book/dui.mp4");
            testUrls.add("http://10.163.44.11:3000/html/book/dui.mp4"); // 如果express.static('.')
            testUrls.add(baseUrl + "html/book/dui.mp4"); 
            testUrls.add("http://10.163.44.11:3000/dui.mp4"); // 如果express.static('html/book')
            
            // 原始构建方式
            testUrls.add(buildVideoUrl());
            
            // 测试每个URL
            final StringBuilder resultBuilder = new StringBuilder("URL测试结果:\n");
            boolean anyUrlWorks = false;
            String workingUrl = null;
            
            for (String url : testUrls) {
                boolean isAccessible = checkUrlAccessible(url);
                resultBuilder.append(url).append(" -> ").append(isAccessible ? "可访问" : "不可访问").append("\n");
                if (isAccessible && !anyUrlWorks) {
                    anyUrlWorks = true;
                    workingUrl = url;
                }
            }
            
            // 保存结果
            final String results = resultBuilder.toString();
            final boolean foundWorkingUrl = anyUrlWorks;
            final String finalWorkingUrl = workingUrl;
            
            // 在UI线程显示结果和播放视频
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, results);
                    
                    if (foundWorkingUrl && finalWorkingUrl != null) {
                        // 如果找到可用URL，使用该URL播放视频
                        Toast.makeText(VideoPlayerActivity.this, "找到可访问的URL: " + finalWorkingUrl, Toast.LENGTH_LONG).show();
                        // 设置有效的URL并播放
                        playVideoWithUrl(finalWorkingUrl);
                    } else {
                        // 如果所有URL都不可用，显示错误
                        showError("所有测试的URL均无法访问。请检查服务器和网络连接。\n" + results + 
                            "\n\n可能原因:\n1. Node.js服务器未配置静态文件访问\n2. 视频文件路径不正确\n3. 防火墙阻止访问");
                    }
                }
            });
        }).start();
    }
    
    private String buildVideoUrl() {
        try {
            // 增加详细日志以便调试
            Log.d(TAG, "原始视频路径: " + videoPath);
            
            // 如果是完整的http/https URL，直接返回
            if (videoPath != null && (videoPath.startsWith("http://") || videoPath.startsWith("https://"))) {
                Log.d(TAG, "使用完整URL: " + videoPath);
                return videoPath;
            }
            
            // 获取基础URL
            String baseUrl = AppConfig.BASE_URL;
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            
            // 处理特殊情况 - 数据结构与算法课程的视频
            if (courseTitle != null && courseTitle.contains("数据结构与算法")) {
                Log.d(TAG, "数据结构与算法课程，使用特定视频路径");
                // 根据Node.js静态文件托管规则，正确的路径可能是:
                // 如果express.static('html')，则URL是: http://10.163.44.11:3000/book/dui.mp4
                return baseUrl + "book/dui.mp4";
            }
            
            // 处理路径，确保格式正确
            if (videoPath != null) {
                String processedPath = videoPath;
                
                // 处理路径，移除不必要的前缀
                if (processedPath.startsWith("/")) {
                    processedPath = processedPath.substring(1);
                }
                
                // 如果路径包含html/，移除它（因为express.static('html')会导致这部分路径冗余）
                if (processedPath.startsWith("html/")) {
                    processedPath = processedPath.substring(5); // 移除"html/"
                }
                
                // 构建完整的URL
                String fullUrl = baseUrl + processedPath;
                
                // 打印最终URL以便调试
                Log.d(TAG, "最终视频URL: " + fullUrl);
                return fullUrl;
            } else {
                Log.e(TAG, "视频路径为空");
                return baseUrl; // 返回基础URL，虽然这可能会导致404错误
            }
        } catch (Exception e) {
            Log.e(TAG, "构建URL时出错", e);
            return AppConfig.BASE_URL; // 出错时返回基础URL
        }
    }
    
    // 检查URL是否可访问
    private boolean checkUrlAccessible(String urlString) {
        try {
            Log.d(TAG, "正在检查URL: " + urlString);
            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // 设置一些请求头，避免某些服务器的限制
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "*/*");
            
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            String contentType = connection.getContentType();
            long contentLength = connection.getContentLength();
            
            Log.d(TAG, String.format("URL检查结果: %s -> 响应码: %d, 消息: %s, 内容类型: %s, 内容长度: %d",
                    urlString, responseCode, responseMessage, contentType, contentLength));
            
            // 如果响应码是2xx或3xx，都可能是有效的
            boolean success = (responseCode >= 200 && responseCode < 400);
            
            // 如果是视频内容类型，更可能是有效的
            if (success && contentType != null && contentType.startsWith("video/")) {
                Log.d(TAG, "找到有效的视频URL: " + urlString);
            }
            
            return success;
        } catch (Exception e) {
            Log.e(TAG, "检查URL可访问性时出错: " + urlString, e);
            return false;
        }
    }
    
    private void initializePlayer() {
        if (player == null) {
            try {
                // 创建播放器实例
                player = new ExoPlayer.Builder(this).build();
                
                // 设置播放器视图
                binding.playerView.setPlayer(player);
                
                // 设置播放器参数
                player.setPlayWhenReady(playWhenReady);
                
                // 构建视频URL
                String fullVideoUrl = buildVideoUrl();
                Log.d(TAG, "尝试加载视频URL: " + fullVideoUrl);
                
                // 显示视频URL以便调试
                Toast.makeText(this, "正在加载视频: " + fullVideoUrl, Toast.LENGTH_LONG).show();
                
                // 创建媒体项
                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(fullVideoUrl));
                player.setMediaItem(mediaItem);
                
                // 设置监听器
                player.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == Player.STATE_READY) {
                            binding.progressBar.setVisibility(View.GONE);
                            Log.d(TAG, "视频准备就绪，开始播放");
                        } else if (playbackState == Player.STATE_BUFFERING) {
                            binding.progressBar.setVisibility(View.VISIBLE);
                            Log.d(TAG, "视频缓冲中...");
                        } else if (playbackState == Player.STATE_ENDED) {
                            Log.d(TAG, "视频播放完成");
                            Toast.makeText(VideoPlayerActivity.this, "视频播放完成", Toast.LENGTH_SHORT).show();
                        } else if (playbackState == Player.STATE_IDLE) {
                            Log.d(TAG, "播放器处于空闲状态");
                        }
                    }
                    
                    @Override
                    public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                        binding.progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "播放器错误: " + error.getMessage(), error);
                        
                        String errorMessage = "视频播放出错: " + error.getMessage();
                        if (error.getCause() instanceof androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                            androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException httpError = 
                                (androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) error.getCause();
                            errorMessage += "\n\n错误代码: " + httpError.responseCode;
                            errorMessage += "\n请求URL: " + httpError.dataSpec.uri.toString();
                        }
                        
                        showError(errorMessage + "\n\n请检查网络连接或视频文件是否存在");
                        
                        // 显示更多错误信息
                        Toast.makeText(VideoPlayerActivity.this, 
                            "错误代码: " + error.getErrorCodeName() + 
                            "\n请尝试在浏览器中打开", Toast.LENGTH_LONG).show();
                    }
                });
                
                // 准备播放器
                player.seekTo(currentWindow, playbackPosition);
                player.prepare();
                
            } catch (Exception e) {
                Log.e(TAG, "初始化播放器错误", e);
                showError("初始化播放器时出错: " + e.getMessage() + "\n\n请检查视频路径是否正确");
            }
        }
    }
    
    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentMediaItemIndex();
            player.release();
            player = null;
        }
    }
    
    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.playerView.setVisibility(View.GONE);
        binding.errorTextView.setVisibility(View.VISIBLE);
        
        // 添加Node.js配置建议
        String fullMessage = message;
        if (message.contains("404") || message.contains("找不到")) {
            fullMessage += "\n\n配置建议:\n" +
                    "1. 确保Node.js服务器配置了静态文件:\n" +
                    "app.use(express.static('html'));\n" +
                    "2. 确保视频文件在服务器的正确路径下\n" +
                    "3. 检查防火墙是否允许访问3000端口";
        }
        
        binding.errorTextView.setText(fullMessage + "\n\n点击此处在浏览器中打开视频");
        
        // 添加点击事件，在浏览器中打开视频
        binding.errorTextView.setOnClickListener(v -> {
            try {
                // 尝试使用可能有效的URL格式
                String urlToOpen;
                if (courseTitle != null && courseTitle.contains("数据结构与算法")) {
                    urlToOpen = AppConfig.BASE_URL + "book/dui.mp4";
                } else {
                    urlToOpen = buildVideoUrl();
                }
                
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(urlToOpen));
                startActivity(intent);
                
                // 在浏览器中打开后显示提示
                Toast.makeText(this, "在浏览器中尝试打开: " + urlToOpen, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "无法在浏览器中打开视频", e);
                Toast.makeText(this, "无法在浏览器中打开视频: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT < 24 || player == null) && videoPath != null && !videoPath.isEmpty()) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }

    // 使用指定的URL播放视频
    private void playVideoWithUrl(String url) {
        // 设置视频路径
        videoPath = url;
        // 初始化播放器
        initializePlayer();
    }
} 