#!/bin/bash

echo "正在启动云课堂服务器..."
echo

# 检查node是否已安装
if ! command -v node &> /dev/null; then
    echo "错误: 未找到Node.js，请先安装Node.js"
    echo "可以从 https://nodejs.org/ 下载安装"
    exit 1
fi

# 检查是否已安装依赖
if [ ! -d "node_modules" ]; then
    echo "首次运行，正在安装依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "安装依赖失败，请检查网络连接"
        exit 1
    fi
fi

echo "启动服务器..."
echo "服务器将在 http://localhost:3000 上运行"
echo
echo "PDF文件可通过 http://localhost:3000/html/book/python.pdf 访问"
echo "视频文件可通过 http://localhost:3000/html/book/堆.mp4 访问"
echo
echo "按Ctrl+C可停止服务器"
echo

node server.js 