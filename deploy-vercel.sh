#!/bin/bash

# TransAI Reader Vercel 部署脚本
# 两步走方案：本地构建 + Vercel 静态部署

echo "🚀 TransAI Reader Vercel 部署"
echo "================================"

# 检查是否在本地环境
if [ "$VERCEL" = "1" ]; then
    echo "📦 Vercel 环境：使用预构建文件"
    
    # 检查是否有预构建文件
    if [ ! -d "dist" ]; then
        echo "❌ 错误：没有找到预构建的 dist 目录"
        echo "💡 请在本地运行：./deploy-vercel.sh --build"
        exit 1
    fi
    
    echo "✅ 使用预构建文件"
    exit 0
fi

# 本地环境：构建 WebAssembly 版本
echo "🔨 本地构建 WebAssembly 版本..."

# 清理之前的构建
./gradlew clean

# 构建 WebAssembly 版本
./gradlew :composeApp:wasmJsBrowserDistribution

# 检查构建是否成功
if [ ! -d "composeApp/build/distributions/wasmJs" ]; then
    echo "❌ 构建失败"
    exit 1
fi

echo "✅ 构建成功！"

# 准备部署文件
echo "📁 准备部署文件..."
rm -rf dist
mkdir -p dist
cp -r composeApp/build/distributions/wasmJs/* dist/

echo "📋 部署文件列表:"
ls -la dist/

echo ""
echo "🎯 下一步操作："
echo "1. 提交代码到 GitHub: git add . && git commit -m '部署TransAI Reader' && git push"
echo "2. Vercel 会自动部署到: https://transai2.vercel.app"
echo "3. 访问网站查看结果"