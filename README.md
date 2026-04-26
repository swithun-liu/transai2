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
- **线索人物**: 支持先展示阶段性线索人物，保留“逐步探索”的阅读体验
- **人物整理**: 支持 AI 人物归并，可把线索人物整理为更准确的人物实体
- **整理触发控制**: 可在设置中控制自动整理、强证据触发、冲突触发，并支持手动整理
- **EPUB 目录解析**: 优先使用 EPUB 自带 TOC，避免把目录页误当正文，同时显示更完整的章节标题
- **阅读进度**: 自动记录阅读位置，支持书架管理
- **阅读 UI 升级**: 当前 Web/UI 已切到更现代的深色阅读风格，强调正文占屏、紧凑排版与低干扰操作区
- **分层字体策略**: 英文 UI、英文正文、中文界面与中文译文分别使用不同字体策略，兼顾质感与可读性

---

## 🌐 Web 版本在线体验

**生产环境**: 当前准备迁移到 EdgeOne Pages，线上域名待新的 EdgeOne 项目创建后补充

### Web AI 请求说明
- Web 端 AI 请求不会直接从浏览器访问第三方模型接口
- Web 端默认代理路径仍为 `/api/chat/completions`
- 当前仓库已支持通过 `runtime-config.js` 注入新的代理地址，用于解决浏览器 `CORS` 问题
- 因此 OpenAI、DeepSeek、Gemini、LongCat 以及兼容 OpenAI 的自定义中转服务，都可以通过 `Base URL + Model + API Key` 接入

### Web 阅读体验说明
- 单段点击 `Translate` 时会按当前段落发起即时翻译
- 点击 `翻译到此` 时会从第 1 段开始补齐到当前段落
- 已经翻译过的段落不会重复请求，会直接使用本地缓存并展开
- 尚未翻译的段落会按顺序串行请求，避免并发触发模型 API 频控
- 批量翻译不会阻塞用户继续阅读、滚动、打开目录或查看人物列表
- 人物表会先展示线索人物，后续在证据足够时再自动或手动整理为更稳定的人物实体

### 快速开始
1. 访问新的 EdgeOne 线上地址
2. 导入 EPUB 文件或使用内置示例书籍
3. 在设置中配置 AI 模型和 API Key
4. 开始阅读，点击翻译按钮体验 AI 辅助阅读
5. 点击正文中的高亮人物名，查看角色列表并快速定位
6. 使用 `翻译到此` 批量补齐前文翻译，并通过顶部 `进度` 查看后台任务
7. 打开 `Characters` 弹窗，可手动点击 `整理人物` 触发当前书的人物整理

---

## 🛠 技术栈

- **语言**: Kotlin
- **UI 框架**: Compose Multiplatform (Android, iOS, Desktop, Web)
- **网络**: Ktor Client
- **架构**: MVI (Model-View-Intent) / Clean Architecture
- **存储**: Browser Local Storage (Web)
- **字体**: Source Sans 3 / Source Serif 4 / Noto Sans SC

### UI 与字体说明

当前版本的界面和排版策略：

- **整体风格**: 深色阅读器风格，减少“概念图式”的悬浮感，优先保证正文阅读效率
- **阅读区目标**: 尽量提升正文屏幕利用率，减少过厚顶部区域和过大的段落间距
- **书架页目标**: 更接近“阅读工作台”，而不是纯展示型首页

当前字体分工：

- **英文 UI**: `Source Sans 3`
- **英文正文**: `Source Serif 4`
- **中文 UI / 中文译文 / 中文说明**: `Noto Sans SC`

这样做的原因：

- 英文正文使用衬线体后，长段英文阅读更像电子书
- 中文统一走 `Noto Sans SC`，避免 Web/Canvas 渲染场景下因为 fallback 不稳定而出现方块字
- UI 与正文分离后，界面更利落，正文也更有“书感”

### 字体许可说明

当前仓库内使用的字体以**开源、可商用、低版权风险**为原则，优先选用 `SIL Open Font License 1.1`（OFL）字体。

- `Source Sans 3`: Adobe 开源字体，适合 UI 场景
- `Source Serif 4`: Adobe 开源字体，适合长文阅读
- `Noto Sans SC`: 开源中文字体，用于中文界面和译文兜底

说明：

- 这些字体可以随项目一起分发、嵌入和商用，但**不能把字体本身单独拿出来售卖**
- 如果后续替换为其他字体，必须先确认字体授权，不要直接把商业字体打包进仓库
- 若新增字体文件，建议同时在 README 中补充字体来源与许可说明

---

## 🌐 Web 版本部署

项目当前推荐使用 **EdgeOne Pages** 部署 Web 版本。原因：

- 国内访问体验通常比 `Vercel` 更友好
- 支持 `Pages Functions`
- 你的 Web 端 AI 代理可以继续保持 `/api/chat/completions` 这条路径

当前部署结构：
- `dist/`：存放 Web 静态资源
- `functions/api/chat/completions.js`：EdgeOne Pages Function 版 AI 代理
- `edgeone.json`：EdgeOne Pages 项目配置
- `prepare-web-dist.sh`：本地构建并生成可部署的 `dist/`
- `deploy-edgeone.sh`：本地准备 EdgeOne 部署产物

### 部署地址
- **GitHub**: https://github.com/swithun-liu/transai2
- **生产环境**: 由你的 EdgeOne Pages 项目分配

### 🚀 当前推荐流程

#### 第一步：本地重新构建并更新 `dist`
```bash
./prepare-web-dist.sh
```

说明：
- `./prepare-web-dist.sh` 会构建 Web 版本并同步到 `dist/`
- 脚本会额外生成 `dist/runtime-config.js`
- 默认线上代理地址仍写入为 `/api/chat/completions`

#### 第二步：准备 EdgeOne 部署文件
```bash
./deploy-edgeone.sh
```

说明：
- 该脚本会先调用 `./prepare-web-dist.sh`
- 静态站点走 `dist/`
- API 代理走 `functions/api/chat/completions.js`
- `EdgeOne Pages` 会把该函数映射成 `/api/chat/completions`

#### 第三步：接入 EdgeOne Pages

推荐方式：

1. 把当前仓库提交并推送到 GitHub
2. 在 EdgeOne Pages 控制台导入该仓库
3. 保持输出目录为 `dist`
4. 保持 `edgeone.json` 生效
5. 部署完成后访问 EdgeOne 提供的默认域名

### 🌐 运行时代理地址

Web 端支持运行时注入代理地址，优先级如下：

1. `runtime-config.js` 中的 `window.TRANSAI_RUNTIME_CONFIG.aiProxyEndpoint`
2. 本地开发环境默认 `http://127.0.0.1:8081/api/chat/completions`
3. 线上默认 `/api/chat/completions`

如果后续你想临时把线上代理改成单独域名，可在构建前指定：

```bash
TRANSAI_WEB_AI_PROXY_URL=https://your-proxy-url \
./prepare-web-dist.sh
```

### 📊 部署后检查

1. 打开 EdgeOne 默认域名确认页面可以访问
2. 导入 EPUB，确认静态资源和 Wasm 文件加载正常
3. 打开设置页，选择 `LongCat` 或自定义兼容 OpenAI 的服务商进行测试
4. 确认浏览器请求命中 `/api/chat/completions`
5. 验证段落翻译、单词释义是否正常
6. 验证 `翻译到此` 是否只补齐未翻译段落，且请求为串行发送
7. 验证顶部 `进度` 弹窗是否能显示后台翻译进度
8. 验证人物高亮、点击人物后角色列表展开与定位是否正常
9. 验证 EPUB 目录是否显示完整章节名，且正文不再混入目录页内容

### 🔧 特殊情况处理

#### 重新清理并构建
```bash
rm -rf build kotlin-js-store dist
./gradlew clean
./prepare-web-dist.sh
```

#### 旧的 Vercel 路线
- `deploy-vercel.sh` 已废弃，仅保留为提示脚本
- `api/chat/completions.js` 是旧的 Vercel 代理实现
- 当前推荐使用 `functions/api/chat/completions.js`

### 🎯 部署验证清单

**每次部署后检查：**
- ✅ 网站可访问
- ✅ 界面正常显示
- ✅ Wasm 资源可正常下载
- ✅ 基本功能正常（EPUB 导入、阅读、AI 翻译）
- ✅ Web 端不再直接请求第三方模型域名
- ✅ 无明显报错信息

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

说明：

- `./run-web-dev.sh` 现在默认以 `--continuous` 模式启动，会持续监听 Kotlin / Compose / 资源文件改动并自动重编译
- `./run-web-dev.sh` 启动前会自动尝试释放 `8080` 端口上的旧 dev server，避免旧实例继续提供过期资源
- 如果浏览器里还看到旧 UI，先确认终端里已经出现新的编译完成日志，再做一次硬刷新
- 如果刚替换过字体资源，浏览器端也建议做一次硬刷新，避免旧字体缓存继续生效
- 如果端口一直没释放，脚本会直接退出，这时可手动执行 `lsof -nP -iTCP:8080 -sTCP:LISTEN` 查 PID 后再 `kill <PID>`

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

## 👥 人物整理

- 人物系统采用“线索人物 -> AI 人物整理 -> 最终人物实体”的思路。
- 刚出现但身份不明确的称呼，可以先以线索人物形式展示。
- 当文本里出现更强证据，或人物表里出现疑似冲突时，可自动或手动触发人物整理。
- 设置页支持控制：
  - `自动整理`
  - `强证据触发`
  - `冲突触发`
- `Characters` 弹窗底部支持手动点击 `整理人物`。
- 更完整的设计说明见 [人物整理技术方案.md](file:///Users/bytedance/my/project/transai2/人物整理技术方案.md)。

## 📚 EPUB 解析说明

- 阅读顺序基于 EPUB 的 `spine`
- 目录侧栏优先使用 EPUB 自带的 `toc.ncx` / TOC 标题
- 解析时会尽量跳过 `Title Page`、`Dedication`、`Contents`、`Copyright` 等明显非正文页面
- 这样可以减少把目录页误当成正文展示的问题，也能让目录显示更完整的章节标题

---

*Created with ❤️ by TransAI Team*
