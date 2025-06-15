package com.example.yunclass;

import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.yunclass.api.ApiClient;
import com.example.yunclass.config.AppConfig;
import com.example.yunclass.databinding.ActivityPdfViewerBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfViewerActivity extends AppCompatActivity {

    private ActivityPdfViewerBinding binding;
    private static final String TAG = "PdfViewerActivity";
    private String pdfPath;
    private String courseTitle;
    private String fullPdfUrl;
    private File tempPdfFile;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isDestroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化异步处理组件
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        isDestroyed = false;

        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 获取课程信息
        if (getIntent().getExtras() != null) {
            courseTitle = getIntent().getStringExtra("course_title");
            pdfPath = getIntent().getStringExtra("pdf_path");
            
            // 设置标题
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(courseTitle);
            }
            
            // 显示PDF
            if (pdfPath != null && !pdfPath.isEmpty()) {
                setupPdfUrl(pdfPath);
                loadPdfFromAssets();
            } else {
                showError("找不到PDF文件路径");
            }
        } else {
            showError("未提供课程信息");
        }
        
        // 添加点击事件到错误视图
        binding.errorTextView.setOnClickListener(v -> {
            showOpenOptions();
        });
        
        // 添加重试按钮点击事件
        binding.retryButton.setOnClickListener(v -> {
            loadPdfFromAssets();
        });
        
        // 添加在浏览器中打开按钮点击事件
        binding.openInBrowserButton.setOnClickListener(v -> {
            openExternalApp();
        });
        
        // 添加下载PDF按钮点击事件
        binding.downloadButton.setOnClickListener(v -> {
            downloadPdf();
        });
    }
    
    private void setupPdfUrl(String pdfPath) {
        try {
            // 构建完整的PDF URL
            String baseUrl = AppConfig.BASE_URL;
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            
            // 如果是相对路径，则添加基础URL
            if (pdfPath.startsWith("http")) {
                fullPdfUrl = pdfPath;
            } else {
                if (pdfPath.startsWith("/")) {
                    pdfPath = pdfPath.substring(1);
                }
                fullPdfUrl = baseUrl + pdfPath;
            }
            
            Log.d(TAG, "PDF URL: " + fullPdfUrl);
            
            // 提取PDF文件名
            String pdfFileName;
            if (pdfPath.contains("/")) {
                pdfFileName = pdfPath.substring(pdfPath.lastIndexOf("/") + 1);
            } else {
                pdfFileName = pdfPath;
            }
            Log.d(TAG, "PDF文件名: " + pdfFileName);
        } catch (Exception e) {
            Log.e(TAG, "设置PDF URL出错", e);
            showError("设置PDF URL时出错: " + e.getMessage());
        }
    }

    private void loadPdfFromAssets() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.errorTextView.setVisibility(View.GONE);
        binding.optionsLayout.setVisibility(View.GONE);
        
        try {
            // 直接尝试用外部应用打开PDF，不再使用WebView
            Log.d(TAG, "直接尝试外部应用打开PDF: " + fullPdfUrl);
            
            // 显示选择对话框
            showPdfOpenDialog();
            
        } catch (Exception e) {
            Log.e(TAG, "初始化PDF查看器时出错", e);
            showError("初始化PDF查看器时出错: " + e.getMessage());
            showOpenOptions();
        }
    }
    
    private void showPdfOpenDialog() {
        binding.progressBar.setVisibility(View.GONE);
        
        // 创建选择对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("PDF打开方式");
        builder.setMessage("请选择PDF文件的打开方式：\n\n推荐使用外部应用打开，体验更佳");
        
        // 使用按钮方式，更可靠
        builder.setPositiveButton("外部应用打开", (dialog, which) -> {
            openWithExternalPdfApp();
        });
        
        builder.setNeutralButton("下载文件", (dialog, which) -> {
            downloadPdf();
        });
        
        builder.setNegativeButton("取消", (dialog, which) -> {
            finish(); // 用户取消则关闭Activity
        });
        
        // 创建并显示对话框
        android.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // 防止意外关闭
        dialog.show();
        
        // 添加更多选项按钮
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnLongClickListener(v -> {
            // 长按显示更多选项
            showMoreOptions();
            dialog.dismiss();
            return true;
        });
    }
    
    private void showMoreOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("更多打开方式");
        
        String[] options = {
            "外部应用打开 (推荐)",
            "下载到本地",
            "在浏览器中打开",
            "尝试应用内显示"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    openWithExternalPdfApp();
                    break;
                case 1:
                    downloadPdf();
                    break;
                case 2:
                    openInBrowser();
                    break;
                case 3:
                    tryWebViewDisplay();
                    break;
            }
        });
        
        builder.setNegativeButton("返回", (dialog, which) -> {
            showPdfOpenDialog(); // 返回主选择对话框
        });
        
        builder.show();
    }
    
    private void openWithExternalPdfApp() {
        try {
            // 创建Intent打开PDF
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fullPdfUrl));
            intent.setType("application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            
            // 检查是否有应用可以处理PDF
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                finish(); // 成功打开外部应用后关闭当前Activity
            } else {
                // 没有PDF查看器，提示用户
                showNoPdfAppDialog();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "外部应用打开PDF失败", e);
            Toast.makeText(this, "外部应用打开失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showPdfOpenDialog(); // 重新显示选择对话框
        }
    }
    
    private void openInBrowser() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fullPdfUrl));
            startActivity(intent);
            finish(); // 成功打开浏览器后关闭当前Activity
        } catch (Exception e) {
            Log.e(TAG, "浏览器打开PDF失败", e);
            Toast.makeText(this, "浏览器打开失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showPdfOpenDialog(); // 重新显示选择对话框
        }
    }
    
    private void tryWebViewDisplay() {
        // 这是原来的WebView显示方法
        try {
            // 配置WebView设置
            WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadsImagesAutomatically(true);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            
            // 添加JavaScript接口，允许网页调用Android方法
            binding.webView.addJavascriptInterface(new WebAppInterface(), "Android");
            
            // 设置WebChromeClient
            binding.webView.setWebChromeClient(new WebChromeClient());
            
            binding.progressBar.setVisibility(View.VISIBLE);
            
            // 使用异步加载避免主线程阻塞
            loadPdfAsync();
            
        } catch (Exception e) {
            Log.e(TAG, "WebView显示PDF出错", e);
            showError("WebView显示PDF时出错: " + e.getMessage());
            showOpenOptions();
        }
    }
    
    private void showNoPdfAppDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("未找到PDF阅读器");
        builder.setMessage("您的设备上没有安装PDF阅读器应用。\n\n建议：\n1. 下载PDF文件后手动打开\n2. 安装PDF阅读器应用（如Adobe Reader）\n3. 在浏览器中查看");
        
        builder.setPositiveButton("下载文件", (dialog, which) -> {
            downloadPdf();
        });
        
        builder.setNeutralButton("浏览器打开", (dialog, which) -> {
            openInBrowser();
        });
        
        builder.setNegativeButton("取消", (dialog, which) -> {
            finish();
        });
        
        builder.show();
    }
    
    private void tryWebViewPdfViewers() {
        if (isDestroyed) return;
        
        // 使用简化PDF查看器
        String localPdfUrl = "file:///android_asset/simple-pdf-viewer.html?" + Uri.encode(fullPdfUrl);
        Log.d(TAG, "尝试简化PDF查看器: " + localPdfUrl);
        
        // 设置超时机制
        final boolean[] hasLoaded = {false};
        mainHandler.postDelayed(() -> {
            if (!hasLoaded[0] && !isDestroyed) {
                Log.w(TAG, "WebView PDF查看器超时，显示错误选项");
                showError("无法在应用内显示PDF文件。");
                showOpenOptions();
            }
        }, 15000); // 15秒超时
        
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isDestroyed && !hasLoaded[0]) {
                    hasLoaded[0] = true;
                    binding.progressBar.setVisibility(View.GONE);
                    binding.webView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "WebView PDF查看器加载成功");
                }
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (!isDestroyed && !hasLoaded[0]) {
                    hasLoaded[0] = true;
                    Log.e(TAG, "WebView PDF查看器失败: " + description + " (错误代码: " + errorCode + ")");
                    showError("WebView无法显示PDF文件: " + description);
                    showOpenOptions();
                }
            }
        });
        
        binding.webView.loadUrl(localPdfUrl);
    }
    
    // 检查文件是否为真正的PDF格式
    private boolean isRealPdf(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (file.length() < 4) {
                return false;
            }
            byte[] buffer = new byte[4];
            raf.read(buffer);
            String header = new String(buffer);
            return "%PDF".equals(header);
        } catch (Exception e) {
            Log.e(TAG, "检查PDF格式时出错", e);
            return false;
        }
    }
    
    // 异步加载PDF文件
    private void loadPdfAsync() {
        if (isDestroyed) return;
        
        // 在后台线程执行PDF加载
        executorService.execute(() -> {
            try {
                Log.d(TAG, "开始异步加载PDF: " + fullPdfUrl);
                
                // 模拟检查网络连接和文件可用性
                Thread.sleep(100); // 避免主线程阻塞
                
                // 在主线程更新UI
                if (!isDestroyed) {
                    mainHandler.post(() -> {
                        if (!isDestroyed) {
                            tryWebViewPdfViewers();
                        }
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "异步加载PDF失败", e);
                if (!isDestroyed) {
                    mainHandler.post(() -> {
                        if (!isDestroyed) {
                            showError("加载PDF时出错: " + e.getMessage());
                            showOpenOptions();
                        }
                    });
                }
            }
        });
    }
    
    // 不再需要从assets复制文件，直接使用网络URL
    
    private void loadPdfInWebView(File pdfFile) {
        // 设置WebView
        WebSettings webSettings = binding.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                binding.progressBar.setVisibility(View.GONE);
                binding.webView.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showError("无法加载PDF: " + description);
                showOpenOptions();
            }
        });
        
        binding.webView.setWebChromeClient(new WebChromeClient());
        
        try {
            // 使用file:///android_asset/方式加载assets中的PDF
            String url = "file:///android_asset/" + pdfPath;
            binding.webView.loadUrl(url);
        } catch (Exception e) {
            showError("加载PDF时出错: " + e.getMessage());
            showOpenOptions();
        }
    }
    
    private void openExternalApp() {
        try {
            // 创建打开系统浏览器的Intent
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fullPdfUrl));
            startActivity(intent);
            finish(); // 关闭当前Activity，因为我们已经启动了外部浏览器
        } catch (Exception e) {
            showError("无法打开浏览器: " + e.getMessage());
        }
    }
    
    private void showOpenOptions() {
        binding.optionsLayout.setVisibility(View.VISIBLE);
    }
    
    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.webView.setVisibility(View.GONE);
        binding.errorTextView.setVisibility(View.VISIBLE);
        binding.errorTextView.setText(message + "\n\n点击尝试其他打开方式");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pdf_viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_open_in_browser) {
            openExternalApp();
            return true;
        } else if (id == R.id.action_retry) {
            loadPdfFromAssets();
            return true;
        } else if (id == R.id.action_share) {
            sharePdfLink();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    // JavaScript接口类
    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void openExternal(String url) {
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(PdfViewerActivity.this, "无法打开外部应用: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        @android.webkit.JavascriptInterface
        public void downloadPdf(String url) {
            runOnUiThread(() -> {
                Toast.makeText(PdfViewerActivity.this, "开始下载PDF文件...", Toast.LENGTH_SHORT).show();
                PdfViewerActivity.this.downloadPdf();
            });
        }
        
        @android.webkit.JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> {
                Toast.makeText(PdfViewerActivity.this, message, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void sharePdfLink() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, fullPdfUrl);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, courseTitle);
        startActivity(Intent.createChooser(shareIntent, "分享PDF链接"));
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        
        // 释放WebView资源
        if (binding != null && binding.webView != null) {
            binding.webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            binding.webView.clearHistory();
            binding.webView.clearCache(true);
            binding.webView.onPause();
            binding.webView.removeAllViews();
            binding.webView.destroyDrawingCache();
            binding.webView.destroy();
        }
        
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // 删除临时文件
        if (tempPdfFile != null && tempPdfFile.exists()) {
            tempPdfFile.delete();
        }
        
        super.onDestroy();
    }

    // 添加下载PDF的方法
    private void downloadPdf() {
        if (isDestroyed) return;
        
        Log.d(TAG, "开始下载PDF文件: " + fullPdfUrl);
        
        executorService.execute(() -> {
            try {
                // 创建下载请求
                java.net.URL url = new java.net.URL(fullPdfUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
                connection.connect();
                
                if (connection.getResponseCode() == java.net.HttpURLConnection.HTTP_OK) {
                    // 获取文件大小
                    int fileLength = connection.getContentLength();
                    
                    // 创建临时文件
                    File tempDir = new File(getCacheDir(), "pdf_temp");
                    if (!tempDir.exists()) {
                        tempDir.mkdirs();
                    }
                    
                    String fileName = "temp_" + System.currentTimeMillis() + ".pdf";
                    File tempFile = new File(tempDir, fileName);
                    
                    // 下载文件
                    try (java.io.InputStream input = connection.getInputStream();
                         java.io.FileOutputStream output = new java.io.FileOutputStream(tempFile)) {
                        
                        byte[] buffer = new byte[4096];
                        long total = 0;
                        int count;
                        
                        while ((count = input.read(buffer)) != -1) {
                            total += count;
                            output.write(buffer, 0, count);
                            
                            // 更新进度（如果需要）
                            if (fileLength > 0) {
                                final int progress = (int) (total * 100 / fileLength);
                                mainHandler.post(() -> {
                                    if (!isDestroyed) {
                                        // 可以在这里更新进度条
                                        Log.d(TAG, "下载进度: " + progress + "%");
                                    }
                                });
                            }
                        }
                    }
                    
                    // 下载完成，在主线程中打开文件
                    mainHandler.post(() -> {
                        if (!isDestroyed) {
                            openDownloadedPdf(tempFile);
                        }
                    });
                    
                } else {
                    final int responseCode = connection.getResponseCode();
                    mainHandler.post(() -> {
                        if (!isDestroyed) {
                            showError("下载失败，HTTP状态码: " + responseCode);
                            showOpenOptions();
                        }
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "下载PDF失败", e);
                mainHandler.post(() -> {
                    if (!isDestroyed) {
                        showError("下载PDF失败: " + e.getMessage());
                        showOpenOptions();
                    }
                });
            }
        });
    }
    
    // 打开下载的PDF文件
    private void openDownloadedPdf(File pdfFile) {
        try {
            // 使用FileProvider创建URI
            android.net.Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                this, 
                getPackageName() + ".fileprovider", 
                pdfFile
            );
            
            // 创建Intent打开PDF
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // 检查是否有应用可以处理PDF
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                finish(); // 关闭当前Activity
            } else {
                // 没有PDF查看器，尝试用浏览器打开
                intent.setDataAndType(pdfUri, "text/plain");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    finish();
                } else {
                    showError("没有找到可以打开PDF的应用，请安装PDF阅读器");
                    showOpenOptions();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "打开下载的PDF失败", e);
            showError("打开PDF文件失败: " + e.getMessage());
            showOpenOptions();
        }
    }
} 