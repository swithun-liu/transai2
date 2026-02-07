# TransAI Reader (KMP)

**TransAI Reader** 是一个基于 Kotlin Multiplatform (KMP) 和 Compose Multiplatform 开发的跨平台 AI 辅助阅读应用。

它的核心理念不是替代阅读，而是**增强阅读**。通过 AI 提供的母语级翻译和文化注解，消除语言障碍，让读者能够沉浸式地欣赏外文文学作品的原汁原味。

---

## 🚀 现有功能 (Current Features - MVP)

目前处于 **MVP (Minimum Viable Product)** 阶段，已验证核心的“阅读 + 即时翻译”闭环。

### 1. 跨平台支持
- [x] **Android**: 完整的原生体验。
- [x] **iOS**: 通过 SwiftUI + Compose Multiplatform 实现。
- [x] **Desktop (macOS)**: 支持桌面端阅读。

### 2. 沉浸式阅读体验
- [x] **点击即译**: 默认显示英文原文，点击任意段落即可在下方展开 AI 生成的中文翻译。
- [x] **智能分段**: 自动识别长段落，基于语义和标点进行智能拆分（默认 ~300 字符），确保移动端阅读体验和翻译颗粒度。
- [x] **目录跳转**: 支持侧边栏目录显示，自动提取章节标题（支持 h1-h3 标签及 Title 回退），点击即可快速跳转至对应章节。
- [x] **状态管理**: 支持展开/收起，加载状态显示，以及错误重试机制（如 API Key 未填写时的提示）。
- [x] **流式 UI**: 简洁的阅读界面，适配不同屏幕尺寸。

### 3. 多模型 AI 翻译引擎
支持多种主流 AI 模型，用户可根据需求自由切换：
- [x] **OpenAI** (GPT-3.5/4)
- [x] **DeepSeek** (深度求索 - 高性价比国产模型)
- [x] **Gemini** (Google - OpenAI 兼容模式)
- [x] **自定义 (Custom)**: 支持任意兼容 OpenAI 接口的 LLM 服务。

### 4. 书架与文件管理
- [x] **书架管理**: 首页展示书架，支持添加本地书籍（.epub）。
- [x] **删除功能**: 支持删除书籍，包含二次确认弹窗，并会同步物理删除磁盘上的文件（彻底释放空间）。
- [x] **内置示例**: 首次启动自动加载内置示例书籍，方便快速体验。
- [x] **文件导入**: 支持系统文件选择器导入本地 EPUB 文件，并自动复制到应用沙盒目录。
- [x] **文件管理**: 支持查看书籍的实际存储路径，并提供打开所在文件夹的功能（支持 Android/Desktop）。
- [x] **阅读进度**: 自动记录阅读进度，下次打开自动跳转至上次阅读位置，并在书架显示阅读百分比。

### 5. 动态配置
- [x] **设置界面**: 用户可实时配置 API Key、Base URL 和模型名称。
- [x] **预设菜单**: 提供常用模型的快捷选择（自动填充 Base URL）。
- [x] **配置热生效**: 无需重启应用，切换模型后立即生效。

### 6. 翻译缓存
- [x] **本地存储**: 自动缓存已翻译的段落，再次阅读时直接加载，节省 Token 并支持离线查看。

---

## 🔮 未来规划 (Roadmap)

我们致力于将其打造为“随身带着一位博学译者的电子书阅读器”。

### Phase 1: 内容与管理
- [x] **多格式导入**: 目前支持 `.epub` 文件的导入与解析，计划增加 `.txt`, `.pdf` 支持。
- [x] **智能分段**: 优化文本切分算法，更精准地处理长句和跨段落对话。
- [x] **书架管理**: 本地图书库管理，支持书籍的增删改查。

### Phase 2: 深度文学体验
- [ ] **文风定制 (Style Persona)**: 用户可选择翻译风格（如“古典严肃”、“现代通俗”、“赛博朋克”），AI 自动调整用词风格。
- [ ] **术语一致性 (Glossary)**: AI 自动提取人名、地名，并在全书中保持翻译一致。
- [ ] **文化注解 (Cultural Footnotes)**: 识别文中的典故、双关语或文化背景，并在翻译旁生成小注（类似译者注）。

### Phase 3: 性能与生态
- [x] **本地缓存**: 缓存已翻译段落，节省 Token 消耗并支持离线阅读。
- [ ] **云端同步**: 多端同步阅读进度和配置。
- [ ] **TTS 朗读**: 结合 AI 语音生成，支持多角色的有声书体验。

---

## 🛠 技术栈

- **语言**: Kotlin
- **UI 框架**: Compose Multiplatform (Android, iOS, Desktop)
- **网络**: Ktor Client
- **序列化**: Kotlinx.serialization
- **架构**: MVI (Model-View-Intent) / Clean Architecture
  - **Presentation**: ViewModel (UiState, UiEvent)
  - **Domain**: UseCases (Business Logic)
  - **Data**: Repository & Data Sources
- **存储**: Multiplatform Settings

---

## ▶️ 如何运行

### Android
使用 Android Studio 直接运行 `composeApp`，或在终端执行：
```shell
./gradlew :composeApp:assembleDebug
```

### Desktop (JVM)
运行 Gradle 任务：
```shell
./gradlew :composeApp:run
```

### iOS
1. 确保已安装 Xcode。
2. 在 `iosApp` 目录下配置 Signing Team。
3. 使用 Android Studio 运行 `iosApp` 配置，或在 Xcode 中打开项目运行。

---
*Created with ❤️ by TransAI Team*
