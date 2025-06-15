@echo off
echo 正在启动云课堂服务器...
echo.

REM 检查node是否已安装
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到Node.js，请先安装Node.js
    echo 可以从 https://nodejs.org/ 下载安装
    pause
    exit /b
)

REM 检查是否已安装依赖
if not exist node_modules (
    echo 首次运行，正在安装依赖...
    call npm install
    if %errorlevel% neq 0 (
        echo 安装依赖失败，请检查网络连接
        pause
        exit /b
    )
)

echo 启动服务器...
echo 服务器将在 http://localhost:3000 上运行
echo.
echo PDF文件可通过 http://localhost:3000/html/book/python.pdf 访问
echo 视频文件可通过 http://localhost:3000/html/book/堆.mp4 访问
echo.
echo 按Ctrl+C可停止服务器
echo.

node server.js

pause