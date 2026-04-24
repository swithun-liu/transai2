#!/bin/bash

# Vercel 构建脚本 - TransAI Reader
# 解决网络依赖问题，确保构建成功

echo "🚀 开始构建 TransAI Reader Web 版本..."

# 设置环境变量
export NODE_VERSION=18

# 配置 npm 镜像源（解决网络问题）
echo "📦 配置 npm 镜像源..."
yarn config set registry https://registry.npmmirror.com/
npm config set registry https://registry.npmmirror.com/

# 创建 npm 配置文件
echo "registry=https://registry.npmmirror.com/" > .npmrc

# 清理之前的构建
echo "🧹 清理构建缓存..."
./gradlew clean

# 构建 Web 版本
echo "🔨 构建 WebAssembly 版本..."
./gradlew :composeApp:wasmJsBrowserDistribution

# 检查构建是否成功
if [ ! -d "composeApp/build/distributions/wasmJs" ]; then
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi

echo "✅ 构建成功！"
echo "📁 输出目录: composeApp/build/distributions/wasmJs/"

# 显示构建文件
echo "📋 构建文件列表:"
ls -la composeApp/build/distributions/wasmJs/