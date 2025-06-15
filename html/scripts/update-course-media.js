/**
 * 更新课程媒体文件路径配置
 * 此脚本用于更新CourseDetailActivity和VideoPlayerActivity中的媒体文件路径
 */
const fs = require('fs');
const path = require('path');

// 导入配置
const config = require('../config');

// 文件路径
const courseDetailActivityPath = path.resolve(__dirname, '../../app/src/main/java/com/example/yunclass/CourseDetailActivity.java');
const videoPlayerActivityPath = path.resolve(__dirname, '../../app/src/main/java/com/example/yunclass/VideoPlayerActivity.java');

// 更新CourseDetailActivity
function updateCourseDetailActivity() {
  console.log('正在更新CourseDetailActivity...');
  
  try {
    // 读取当前文件内容
    let content = fs.readFileSync(courseDetailActivityPath, 'utf8');
    
    // 更新openVideo方法
    const openVideoRegex = /(private void openVideo\(\) \{[\s\S]*?startActivity\(intent\);[\s\S]*?\})/;
    const openVideoMethod = `private void openVideo() {
        String videoPath;
        
        // 如果有指定的内容路径，则使用它
        if (contentPath != null && !contentPath.isEmpty()) {
            videoPath = contentPath;
        } else {
            // 否则使用默认路径（兼容旧版本）
            videoPath = "html/book/堆.mp4"; // 默认使用堆.mp4
        }
        
        Log.d(TAG, "开始学习课程: " + courseId + ", 视频文件: " + videoPath);
        
        // 跳转到视频播放器
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("course_title", courseTitle);
        intent.putExtra("video_path", videoPath);
        startActivity(intent);
    }`;
    
    // 替换方法
    content = content.replace(openVideoRegex, openVideoMethod);
    
    // 更新openPdf方法
    const openPdfRegex = /(private void openPdf\(\) \{[\s\S]*?startActivity\(intent\);[\s\S]*?\})/;
    const openPdfMethod = `private void openPdf() {
        String pdfPath;
        
        // 如果有指定的内容路径，则使用它
        if (contentPath != null && !contentPath.isEmpty()) {
            pdfPath = contentPath;
        } else {
            // 否则使用默认路径（兼容旧版本）
            pdfPath = "html/book/python.pdf"; // 默认使用python.pdf
        }
        
        Log.d(TAG, "开始学习课程: " + courseId + ", PDF文件: " + pdfPath);
        
        // 跳转到PDF查看器
        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra("course_title", courseTitle);
        intent.putExtra("pdf_path", pdfPath);
        startActivity(intent);
    }`;
    
    // 替换方法
    content = content.replace(openPdfRegex, openPdfMethod);
    
    // 写回文件
    fs.writeFileSync(courseDetailActivityPath, content);
    console.log('CourseDetailActivity.java 已更新');
    
  } catch (error) {
    console.error('更新CourseDetailActivity时出错:', error);
  }
}

// 更新VideoPlayerActivity
function updateVideoPlayerActivity() {
  console.log('正在更新VideoPlayerActivity...');
  
  try {
    // 读取当前文件内容
    let content = fs.readFileSync(videoPlayerActivityPath, 'utf8');
    
    // 更新buildVideoUrl方法
    const buildVideoUrlRegex = /(private String buildVideoUrl\(\) \{[\s\S]*?return baseUrl \+ videoPath;[\s\S]*?\})/;
    const buildVideoUrlMethod = `private String buildVideoUrl() {
        if (videoPath.startsWith("http")) {
            return videoPath;
        } else {
            String baseUrl = AppConfig.BASE_URL;
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            
            if (videoPath.startsWith("/")) {
                videoPath = videoPath.substring(1);
            }
            
            Log.d(TAG, "构建视频URL: 基础URL=" + baseUrl + ", 视频路径=" + videoPath);
            return baseUrl + videoPath;
        }
    }`;
    
    // 替换方法
    content = content.replace(buildVideoUrlRegex, buildVideoUrlMethod);
    
    // 写回文件
    fs.writeFileSync(videoPlayerActivityPath, content);
    console.log('VideoPlayerActivity.java 已更新');
    
  } catch (error) {
    console.error('更新VideoPlayerActivity时出错:', error);
  }
}

// 执行更新
try {
  console.log(`正在更新课程媒体文件配置，使用服务器地址: ${config.HOST}:${config.PORT}`);
  updateCourseDetailActivity();
  updateVideoPlayerActivity();
  console.log('\n所有媒体文件配置已成功更新！');
} catch (error) {
  console.error('更新媒体文件配置时出错:', error);
} 