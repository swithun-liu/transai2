# TransAI Reader (KMP)

**TransAI Reader** 是一个基于 Kotlin Multiplatform (KMP) 和 Compose Multiplatform 开发的跨平台 AI 辅助阅读应用。

核心功能：AI 辅助阅读，支持 EPUB 导入、即时翻译、单词释义，让外文阅读更轻松。

---

## 🚀 核心功能

- **跨平台支持**: Android, iOS, Desktop, Web
- **AI 翻译**: 支持 OpenAI, DeepSeek, Gemini, LongCat 等主流模型
- **EPUB 阅读**: 完整的 EPUB 文件支持，包含《东方快车谋杀案》示例书籍
- **即时翻译**: 段落翻译和单词释义，基于上下文智能翻译
- **阅读进度**: 自动记录阅读位置，支持书架管理

---

## 🌐 Web 版本在线体验

**生产环境**: https://transai2.vercel.app

### Web AI 请求说明
- Web 端 AI 请求不会直接从浏览器访问第三方模型接口
- 所有 Web 端 `chat/completions` 请求统一走同源代理：`/api/chat/completions`
- 该代理由当前 Vercel 项目提供，用于解决浏览器 `CORS` 问题
- 因此 OpenAI、DeepSeek、Gemini、LongCat 以及兼容 OpenAI 的自定义中转服务，都可以通过 `Base URL + Model + API Key` 接入

### 快速开始
1. 访问 https://transai2.vercel.app
2. 导入 EPUB 文件或使用内置示例书籍
3. 在设置中配置 AI 模型和 API Key
4. 开始阅读，点击翻译按钮体验 AI 辅助阅读

---

## 🛠 技术栈

- **语言**: Kotlin
- **UI 框架**: Compose Multiplatform (Android, iOS, Desktop, Web)
- **网络**: Ktor Client
- **架构**: MVI (Model-View-Intent) / Clean Architecture
- **存储**: Browser Local Storage (Web)

---

## 🌐 Web 版本部署

项目使用 **GitHub + Vercel** 自动化部署。

当前部署结构：
- `dist/`：存放 Web 静态资源
- `api/`：存放 Vercel Serverless Function
- `vercel.json`：同时处理静态站点和 `/api/*` 代理路由

### 部署地址
- **GitHub**: https://github.com/swithun-liu/transai2
- **生产环境**: https://transai2.vercel.app

### 🚀 部署流程（3步完成）

#### 第一步：本地构建 WebAssembly 版本
```bash
# 使用部署脚本（推荐）
./deploy-vercel.sh

# 或者手动构建
./gradlew :composeApp:wasmJsBrowserDistribution
```

说明：
- `./deploy-vercel.sh` 会自动构建 Web 版本
- 构建完成后会自动把最新产物同步到 `dist/`

#### 第二步：提交代码到 GitHub
```bash
git add .
git commit -m "更新描述"
git push origin master
```

#### 第三步：Vercel 自动部署
- **自动触发**: 代码推送到 GitHub 后，Vercel 自动开始部署
- **部署时间**: 约 1-3 分钟
- **访问地址**: https://transai2.vercel.app

### ✅ 实际发布时建议按下面执行

#### 命令
```bash
# 1. 本地重新构建并更新 dist
./deploy-vercel.sh

# 2. 检查改动
git status

# 3. 提交代码
git add .
git commit -m "fix: add vercel proxy for web ai requests"

# 4. 推送到远端
git push origin master
```

#### 操作
1. 打开 Vercel 控制台确认新的部署任务已开始
2. 等待部署完成后访问线上地址
3. 打开设置页，选择 `LongCat` 或自定义兼容 OpenAI 的服务商进行测试
4. 确认浏览器请求走的是同源 `/api/chat/completions`，而不是直接请求第三方域名
5. 验证段落翻译、单词释义是否正常

### 📊 部署状态监控

1. **Vercel 控制台**: https://vercel.com/swithun-lius-projects/transai2
2. **查看部署日志**: 实时监控构建过程
3. **访问测试**: 部署完成后立即测试功能
4. **网络检查**: 浏览器 DevTools 中确认 Web 请求命中 `/api/chat/completions`

### 🔧 特殊情况处理

#### 网络依赖问题（如果出现）
```bash
# 清理缓存重新构建
rm -rf build kotlin-js-store
./gradlew clean
./deploy-vercel.sh
```

#### 部署失败处理
- **自动回滚**: Vercel 会自动回滚到上一个可用版本
- **错误日志**: 在 Vercel 控制台查看详细错误信息
- **重新部署**: 修复问题后重新推送代码

### 🎯 部署验证清单

**每次部署后检查：**
- ✅ 网站可访问：https://transai2.vercel.app
- ✅ 界面正常显示
- ✅ 基本功能正常（EPUB 导入、AI 翻译）
- ✅ Web 端不再直接请求第三方模型域名
- ✅ `LongCat` / 自定义兼容 OpenAI 服务商无 CORS 报错
- ✅ 无报错信息

---

## ▶️ 本地开发

### Web 开发模式
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### 生产构建
```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

### 说明：
- Web 端通过浏览器文件选择器导入 `.epub` 文件
- UI 由 Compose Multiplatform `wasmJs` 渲染
- 文件导入、ZIP 解压与浏览器存储通过 JS bridge 提供能力
- 书架、阅读进度、翻译缓存等数据会持久化到浏览器本地存储

---

*Created with ❤️ by TransAI Team*
