#!/bin/bash

# TransAI Reader Gitee Pages 部署脚本
# 作者：TransAI Team

echo "🚀 开始部署 TransAI Reader 到 Gitee Pages..."

# 检查是否已安装必要的工具
if ! command -v git &> /dev/null; then
    echo "❌ Git 未安装，请先安装 Git"
    exit 1
fi

# 设置 npm 镜像源（解决网络问题）
echo "📦 配置 npm 镜像源..."
cat > .npmrc << EOF
registry=https://registry.npmmirror.com/
EOF

# 清理并构建项目
echo "🔨 构建 Web 版本..."
./gradlew clean
./gradlew :composeApp:wasmJsBrowserDistribution

# 检查构建是否成功
if [ ! -d "composeApp/build/distributions/wasmJs" ]; then
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi

echo "✅ 构建成功！"

# 创建部署目录
mkdir -p deploy
echo "📁 准备部署文件..."

# 复制构建文件到部署目录
cp -r composeApp/build/distributions/wasmJs/* deploy/

# 创建 Gitee Pages 配置文件
cat > deploy/README.md << 'EOF'
# TransAI Reader - AI 辅助阅读器

这是一个基于 Kotlin Multiplatform 和 Compose Multiplatform 开发的跨平台 AI 辅助阅读应用。

## 功能特性

- 📚 支持 EPUB 格式电子书阅读
- 🤖 多 AI 模型翻译（OpenAI/DeepSeek/Gemini）
- 🌐 跨平台支持（Web/Android/iOS/Desktop）
- 💾 本地存储和缓存

## 使用说明

1. 点击"导入书籍"选择 EPUB 文件
2. 在设置中配置 AI 模型的 API Key
3. 开始阅读，点击段落下方按钮查看翻译

## 技术栈

- Kotlin Multiplatform
- Compose Multiplatform
- WebAssembly (Wasm)
- Ktor Client

---
*Created with ❤️ by TransAI Team*
EOF

# 创建部署说明
cat > DEPLOYMENT_GUIDE.md << 'EOF'
# Gitee Pages 部署指南

## 步骤 1：创建 Gitee 仓库

1. 访问 https://gitee.com/ 注册/登录账号
2. 点击"新建仓库"
3. 仓库名称：transai-reader（或其他名称）
4. 选择"公开"
5. 初始化 README.md

## 步骤 2：上传文件

将 `deploy/` 目录下的所有文件上传到仓库：

```bash
# 克隆仓库
git clone https://gitee.com/你的用户名/transai-reader.git
cd transai-reader

# 复制部署文件
cp -r ../deploy/* .

# 提交并推送
git add .
git commit -m "deploy: TransAI Reader Web版本"
git push origin main
```

## 步骤 3：开启 Gitee Pages

1. 进入仓库页面
2. 点击"服务" → "Gitee Pages"
3. 选择"部署分支"（通常是 main 或 master）
4. 选择"部署目录"（根目录 /）
5. 点击"启动"

## 步骤 4：访问网站

部署完成后，访问：
https://你的用户名.gitee.io/transai-reader/

## 注意事项

- 首次部署可能需要几分钟时间
- API Key 存储在浏览器本地，不会上传到服务器
- 支持离线阅读已缓存的翻译内容
- 建议使用 Chrome/Edge/Safari 等现代浏览器

## 故障排除

如果遇到问题：
1. 检查浏览器控制台错误信息
2. 确认 API Key 配置正确
3. 清除浏览器缓存后重试
EOF

echo ""
echo "🎉 部署准备完成！"
echo ""
echo "📋 下一步操作："
echo "1. 查看 DEPLOYMENT_GUIDE.md 获取详细部署步骤"
echo "2. 将 deploy/ 目录下的文件上传到 Gitee 仓库"
echo "3. 开启 Gitee Pages 服务"
echo ""
echo "🌐 部署完成后访问地址："
echo "   https://你的用户名.gitee.io/transai-reader/"
echo ""
echo "💡 提示：API Key 配置在浏览器本地存储，安全可靠"