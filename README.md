# TransAI Reader (KMP)

**TransAI Reader** 是一个基于 Kotlin Multiplatform (KMP) 和 Compose Multiplatform 开发的跨平台 AI 辅助阅读应用。

核心功能：AI 辅助阅读，支持 EPUB 导入、即时翻译、单词释义，让外文阅读更轻松。

---

## 🚀 核心功能

- **跨平台支持**: Android, iOS, Desktop, Web
- **AI 翻译**: 支持 OpenAI, DeepSeek, Gemini, LongCat 等主流模型
- **EPUB 阅读**: 完整的 EPUB 文件支持，包含《东方快车谋杀案》示例书籍
- **即时翻译**: 段落翻译和单词释义，基于上下文智能翻译
- **批量翻译**: 支持“一键翻译到此”，已翻译内容直接复用本地缓存，未翻译段落按队列串行请求
- **后台进度**: 批量翻译任务可在后台持续执行，用户可随时查看当前进度
- **人物识别**: 自动提取已识别人物并生成角色列表
- **人物高亮**: 正文中已识别人物会高亮显示，点击可展开并定位到角色列表项
- **EPUB 目录解析**: 优先使用 EPUB 自带 TOC，避免把目录页误当正文，同时显示更完整的章节标题
- **阅读进度**: 自动记录阅读位置，支持书架管理

---

## 🌐 Web 版本在线体验

**生产环境**: https://transai2.vercel.app

### Web AI 请求说明
- Web 端 AI 请求不会直接从浏览器访问第三方模型接口
- 所有 Web 端 `chat/completions` 请求统一走同源代理：`/api/chat/completions`
- 该代理由当前 Vercel 项目提供，用于解决浏览器 `CORS` 问题
- 因此 OpenAI、DeepSeek、Gemini、LongCat 以及兼容 OpenAI 的自定义中转服务，都可以通过 `Base URL + Model + API Key` 接入

### Web 阅读体验说明
- 单段点击 `Translate` 时会按当前段落发起即时翻译
- 点击 `翻译到此` 时会从第 1 段开始补齐到当前段落
- 已经翻译过的段落不会重复请求，会直接使用本地缓存并展开
- 尚未翻译的段落会按顺序串行请求，避免并发触发模型 API 频控
- 批量翻译不会阻塞用户继续阅读、滚动、打开目录或查看人物列表

### 快速开始
1. 访问 https://transai2.vercel.app
2. 导入 EPUB 文件或使用内置示例书籍
3. 在设置中配置 AI 模型和 API Key
4. 开始阅读，点击翻译按钮体验 AI 辅助阅读
5. 点击正文中的高亮人物名，查看角色列表并快速定位
6. 使用 `翻译到此` 批量补齐前文翻译，并通过顶部 `进度` 查看后台任务

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

### 免费版使用建议

如果使用 Vercel Hobby 免费版，当前项目最需要关注的是：

- **Git push 触发部署**: 重点看 `Deployments Created per Day = 100/day`
- **CLI 直接部署**: 重点看 `Deployments Created from CLI per Week = 2000/week`
- **并发构建**: `Concurrent Builds = 1`，连续推送时会排队
- **单次构建时长**: `Build Time per Deployment = 45 minutes`

因此日常开发建议：

- 先在本地开发模式验证，再集中推送
- 不要每个微小改动都立即触发线上部署
- 构建失败时先看本地日志或 Vercel 日志，不要盲目重复 push

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
6. 验证 `翻译到此` 是否只补齐未翻译段落，且请求为串行发送
7. 验证顶部 `进度` 弹窗是否能显示后台翻译进度
8. 验证人物高亮、点击人物后角色列表展开与定位是否正常
9. 验证 EPUB 目录是否显示完整章节名，且正文不再混入目录页内容

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
# 推荐：在当前仓库内使用隔离的 Gradle/Kotlin/npm/yarn 缓存，避免系统目录权限导致 Wasm dev server 起不来
./run-web-dev.sh

# 兼容原始命令
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

启动后通常可在以下地址访问：

```text
http://127.0.0.1:8080/
```

适合场景：

- 日常 UI 调整
- 交互联调
- 阅读页效果验证
- 在真正部署前先本地确认改动

如果需要在本地 Web 环境调试 AI 翻译，还需要额外启动本地代理：

```bash
node local-ai-proxy.mjs
```

默认本地代理地址：

```text
http://127.0.0.1:8081/api/chat/completions
```

说明：

- `localhost:8080` 的 wasm 开发服务器本身不提供 `/api/chat/completions`
- 因此本地开发模式下，Web 端会自动改走 `127.0.0.1:8081`
- 线上环境仍然继续使用同源 `/api/chat/completions`

推荐流程：

```bash
# 1. 启动本地开发服务
./run-web-dev.sh

# 2. 启动本地 AI 代理
node local-ai-proxy.mjs

# 3. 浏览器打开本地地址
http://127.0.0.1:8080/

# 4. 修改代码后直接刷新页面观察效果
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
- 本地开发时如果需要调试翻译，必须同时启动 `local-ai-proxy.mjs`
- 建议优先使用本地开发模式做调试，确认无误后再执行部署流程

## 📚 EPUB 解析说明

- 阅读顺序基于 EPUB 的 `spine`
- 目录侧栏优先使用 EPUB 自带的 `toc.ncx` / TOC 标题
- 解析时会尽量跳过 `Title Page`、`Dedication`、`Contents`、`Copyright` 等明显非正文页面
- 这样可以减少把目录页误当成正文展示的问题，也能让目录显示更完整的章节标题

---

*Created with ❤️ by TransAI Team*
