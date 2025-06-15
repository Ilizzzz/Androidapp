/**
 * 自动更新Android配置文件脚本
 * 从config.js读取配置，并更新所有相关文件
 */
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// 导入配置
const config = require('../config');

// 文件路径
const androidConfigPath = path.resolve(__dirname, '../../app/src/main/java/com/example/yunclass/config/AppConfig.java');
const networkSecurityConfigPath = path.resolve(__dirname, '../../app/src/main/res/xml/network_security_config.xml');
const jsConfigPath = path.resolve(__dirname, '../js/config.js');
const projectRoot = path.resolve(__dirname, '../..');

// 新增：课程详情和视频播放器路径
const courseDetailActivityPath = path.resolve(__dirname, '../../app/src/main/java/com/example/yunclass/CourseDetailActivity.java');
const videoPlayerActivityPath = path.resolve(__dirname, '../../app/src/main/java/com/example/yunclass/VideoPlayerActivity.java');

// 旧IP地址列表 - 用于查找可能的硬编码IP
const oldIPs = ['10.163.204.43', '192.168.43.230', '10.161.120.131'];

// 更新Android AppConfig.java
function updateAndroidConfig() {
  console.log('正在更新Android配置...');
  
  // 读取当前配置文件
  let appConfigContent = fs.readFileSync(androidConfigPath, 'utf8');
  
  // 替换HOST和PORT
  appConfigContent = appConfigContent.replace(
    /private static final String HOST = "(.*)";/,
    `private static final String HOST = "${config.HOST}";`
  );
  
  appConfigContent = appConfigContent.replace(
    /private static final int PORT = (\d+);/,
    `private static final int PORT = ${config.PORT};`
  );
  
  // 写回文件
  fs.writeFileSync(androidConfigPath, appConfigContent);
  console.log('Android AppConfig.java 已更新');
}

// 更新网络安全配置
function updateNetworkSecurityConfig() {
  console.log('正在更新网络安全配置...');
  
  // 读取当前配置文件
  let networkConfigContent = fs.readFileSync(networkSecurityConfigPath, 'utf8');
  
  // 替换域名
  networkConfigContent = networkConfigContent.replace(
    /<domain includeSubdomains="true">(.*)<\/domain>/,
    `<domain includeSubdomains="true">${config.HOST}</domain>`
  );
  
  // 写回文件
  fs.writeFileSync(networkSecurityConfigPath, networkConfigContent);
  console.log('network_security_config.xml 已更新');
}

// 更新前端JS配置文件（已合并到主配置文件中，此函数保留用于兼容性）
function updateJsConfig() {
  console.log('前端JS配置已合并到主配置文件html/config.js中');
  
  // 检查是否存在旧的js/config.js文件，如果存在则删除
  if (fs.existsSync(jsConfigPath)) {
    fs.unlinkSync(jsConfigPath);
    console.log('已删除旧的js/config.js文件，现在使用统一的html/config.js');
  }
  
  console.log('前端配置更新完成，请在HTML文件中直接引用html/config.js');
}

// 更新课程媒体文件路径
function updateCourseMediaPaths() {
  console.log('正在更新课程媒体文件路径...');
  
  // 更新VideoPlayerActivity
  try {
    if (fs.existsSync(videoPlayerActivityPath)) {
      // 读取当前文件内容
      let content = fs.readFileSync(videoPlayerActivityPath, 'utf8');
      
      // 检查是否需要更新buildVideoUrl方法
      if (!content.includes('构建视频URL: 基础URL=')) {
        // 更新buildVideoUrl方法，添加日志输出
        content = content.replace(
          /private String buildVideoUrl\(\) \{[\s\S]*?if \(videoPath\.startsWith\("http"\)\) \{[\s\S]*?return baseUrl \+ videoPath;[\s\S]*?\}/,
          `private String buildVideoUrl() {
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
    }`
        );
        
        // 写回文件
        fs.writeFileSync(videoPlayerActivityPath, content);
        console.log('VideoPlayerActivity.java 已更新');
      } else {
        console.log('VideoPlayerActivity.java 已经是最新的');
      }
    } else {
      console.log('找不到 VideoPlayerActivity.java 文件');
    }
  } catch (error) {
    console.error('更新VideoPlayerActivity时出错:', error);
  }
}

// 查找并报告可能的硬编码IP地址
function findHardcodedIPs() {
  console.log('正在查找可能的硬编码IP地址...');
  
  // 构建grep命令，查找所有旧IP地址
  const ipPattern = oldIPs.join('|');
  
  try {
    // 不同操作系统使用不同的命令
    let command;
    if (process.platform === 'win32') {
      // Windows
      command = `findstr /s /i /m "${ipPattern}" "${projectRoot}\\*.java" "${projectRoot}\\*.xml" "${projectRoot}\\*.js" "${projectRoot}\\*.json" "${projectRoot}\\*.html" "${projectRoot}\\*.css"`;
    } else {
      // Unix/Linux/macOS
      command = `grep -r -l "${ipPattern}" --include="*.java" --include="*.xml" --include="*.js" --include="*.json" --include="*.html" --include="*.css" ${projectRoot}`;
    }
    
    const result = execSync(command, { encoding: 'utf8' });
    
    if (result.trim()) {
      console.log('\n警告: 以下文件可能包含硬编码的IP地址:');
      console.log(result);
      console.log('请检查这些文件并手动更新IP地址。');
    } else {
      console.log('没有找到硬编码的IP地址。');
    }
  } catch (error) {
    // 如果命令执行失败或没有找到匹配项
    if (error.status !== 1) {
      console.error('查找硬编码IP时出错:', error.message);
    } else {
      console.log('没有找到硬编码的IP地址。');
    }
  }
}

// 检查并更新JSON文件中的URL
function updateJsonFiles() {
  console.log('正在检查JSON文件...');
  
  const jsonFiles = [
    path.resolve(__dirname, '../doc/websites.json'),
    path.resolve(__dirname, '../doc/courses.json')
  ];
  
  jsonFiles.forEach(filePath => {
    if (fs.existsSync(filePath)) {
      try {
        const content = fs.readFileSync(filePath, 'utf8');
        const jsonData = JSON.parse(content);
        
        // 检查是否有任何对象包含旧IP的URL
        let modified = false;
        const processObject = (obj) => {
          for (const key in obj) {
            if (typeof obj[key] === 'string') {
              // 检查URL是否包含旧IP
              for (const oldIP of oldIPs) {
                if (obj[key].includes(oldIP)) {
                  // 替换URL中的旧IP为新IP
                  obj[key] = obj[key].replace(oldIP, config.HOST);
                  modified = true;
                  console.log(`在 ${path.basename(filePath)} 中更新了URL: ${obj[key]}`);
                }
              }
            } else if (typeof obj[key] === 'object' && obj[key] !== null) {
              // 递归处理嵌套对象
              processObject(obj[key]);
            }
          }
        };
        
        // 如果是数组，处理每个元素
        if (Array.isArray(jsonData)) {
          jsonData.forEach(item => processObject(item));
        } else {
          processObject(jsonData);
        }
        
        // 如果有修改，写回文件
        if (modified) {
          fs.writeFileSync(filePath, JSON.stringify(jsonData, null, 2));
          console.log(`${path.basename(filePath)} 已更新`);
        }
      } catch (error) {
        console.error(`处理 ${filePath} 时出错:`, error);
      }
    }
  });
}

// 执行更新
try {
  console.log(`正在将所有配置更新为 HOST: ${config.HOST}, PORT: ${config.PORT}`);
  updateAndroidConfig();
  updateNetworkSecurityConfig();
  updateJsConfig();
  updateCourseMediaPaths();
  updateJsonFiles();
  findHardcodedIPs();
  console.log('\n所有配置已成功更新！');
  console.log('如果有硬编码的IP地址被报告，请手动检查并更新这些文件。');
} catch (error) {
  console.error('更新配置时出错:', error);
} 