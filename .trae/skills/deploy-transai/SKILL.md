---
name: "deploy-transai"
description: "执行 TransAI Reader 的 Web/Vercel 部署。仅在用户明确要求发布 Web、部署线上、更新 dist 或推送触发 Vercel 时调用。"
---

# Deploy TransAI

用于在 `transai2` 仓库中执行 **Web 版本部署**。这个项目的实际发布方式是：

1. 本地执行 `./deploy-vercel.sh`
2. 生成并刷新根目录 `dist/`
3. 提交代码与 `dist/` 产物
4. 推送到 `origin/master`
5. 由 Vercel 自动部署到 `https://transai2.vercel.app`

只有在以下场景才调用本 skill：

- 用户明确说“部署”“发布到线上”“执行 web 部署”“推送触发 vercel”
- 用户要求刷新 `dist/` 并完成完整上线流程
- 用户要求按当前 README 的发布步骤执行

以下场景不要调用本 skill：

- 用户只是想本地运行 Web 开发环境
- 用户只想构建、不想提交或推送
- 用户只想讨论部署方案，不要求实际执行

## 项目事实

执行前必须遵循这些仓库约定：

- 项目根目录有 `README.md`，发布流程以其中的 `Web 版本部署` 和 `实际发布时建议按下面执行` 为准
- Web 构建脚本是 `./deploy-vercel.sh`
- Vercel 线上地址是 `https://transai2.vercel.app`
- 发布依赖 GitHub 推送触发，不是手动调用 Vercel CLI
- 部署产物目录是根目录 `dist/`

## Vercel 免费版限制

为了避免低质量模型误解限制，必须明确区分下面两类额度：

- 本项目当前默认发布方式是 **Git push 触发 Vercel 自动部署**
- 因此优先关注 `Deployments Created per Day = 100/day`
- `Deployments Created from CLI per Week = 2000/week` 主要适用于 `vercel deploy` 这类 CLI 直接部署，不是本项目当前主流程
- 如果用户只是执行当前仓库的标准发布流程，应优先把风险判断建立在 `100/day` 上，而不是 `2000/week`
- 还需要记住 Hobby 版 `Concurrent Builds = 1`，频繁连续推送会排队
- 单次部署构建时长上限是 `45 minutes`
- 如果用户担心免费额度，默认建议：先本地构建验证成功，再 push，减少无效部署次数

当你向用户解释免费版是否够用时，优先使用下面的判断逻辑：

1. 如果是 GitHub 推送触发部署：主要看 `100/day`
2. 如果是 Vercel CLI 直接部署：再补充 `2000/week`
3. 如果只是周末个人开发：通常够用，但仍应避免每个微小改动都 push

## 执行原则

- 先检查，再构建，再提交，再推送
- 先本地构建成功，再允许推送，尽量减少无效部署次数
- 如果工作区有**无关改动**，不要擅自提交，先告知用户并请求确认
- 如果当前改动就是本次要部署的内容，可以继续
- 只提交与本次部署相关的文件
- 不要使用破坏性 git 命令，例如 `git reset --hard`
- 不要自动创建 MR；本项目部署是直接推送 `master`

## 标准流程

### 1. 读取上下文

先读取以下文件，确认发布方式没有变化：

- `README.md`
- `deploy-vercel.sh`
- `vercel-build.sh`

### 2. 检查 Git 状态

执行：

```bash
git status --short --branch
```

检查要点：

- 当前分支是否为 `master`
- 是否存在未提交改动
- 是否有无关文件变更

处理规则：

- 如果存在无关改动：停止并向用户说明，询问是否继续
- 如果只有本次需要部署的改动：继续

### 3. 执行 Web 构建

执行：

```bash
./deploy-vercel.sh
```

该脚本会：

- 先执行 `./gradlew clean`
- 再执行 `./gradlew :composeApp:wasmJsBrowserDistribution`
- 将 `composeApp/build/dist/wasmJs/productionExecutable` 同步到根目录 `dist/`

必须确认：

- 命令退出码为 `0`
- 输出中包含 `BUILD SUCCESSFUL`
- 输出中包含 `构建成功`
- 根目录 `dist/` 已更新

如果失败：

- 不要继续提交或推送
- 记录失败命令和关键报错
- 向用户汇报失败原因

### 4. 复查构建结果

执行：

```bash
git status --short
```

重点确认：

- 代码改动是否仍在
- `dist/` 是否有新增、修改、删除的 wasm/js 文件
- 没有意外改动被带入提交

### 5. 检查诊断

如果本次部署包含代码改动，优先对最近编辑的 Kotlin 文件运行诊断工具。

规则：

- 如果有新引入的明显错误，先修复再部署
- 如果只是 IDE/Kotlin 版本兼容噪音，但真实构建成功，可以继续，并在最终说明里注明

### 6. 提交部署内容

只在用户已经明确要求执行部署时，才进行提交和推送。

执行：

```bash
git add <相关源码文件> dist
git commit -m "<清晰的提交信息>"
```

提交信息要求：

- 直接描述本次功能或修复
- 不要写成空泛的 “update” 或 “deploy”
- 优先使用类似：

```text
feat: highlight detected characters in reader
fix: repair web translation request flow
```

### 7. 推送触发部署

执行：

```bash
git push origin master
```

必须确认：

- 推送退出码为 `0`
- 输出包含 `master -> master`

如果推送失败：

- 停止后续动作
- 汇报失败原因，例如权限、冲突、网络问题

### 8. 汇报结果

最终返回结果时必须包含：

- 是否构建成功
- 是否已提交
- commit hash
- 是否已推送到 `origin/master`
- 线上地址 `https://transai2.vercel.app`
- 需要用户手动验证的关键点

## 推荐输出模板

```markdown
**部署结果**
- 已执行 `./deploy-vercel.sh`，本地 Web 构建成功并刷新 `dist/`
- 已提交并推送到 `master`：`<commit>`
- 已触发 Vercel 自动部署
- 线上地址：`https://transai2.vercel.app`

**验证**
- 构建命令：`./deploy-vercel.sh`
- 推送命令：`git push origin master`
- 构建/推送是否成功：`成功` 或 `失败`

**注意**
- 说明本次是否存在非阻塞警告
- 告知用户下一步可以去 Vercel 控制台查看部署状态
```

## 异常处理

### 工作区脏且包含无关改动

不要继续提交。向用户说明：

- 当前有无关文件改动
- 继续部署会把这些文件一起带上
- 询问用户是否要先清理、拆分提交，或明确允许一起发布

### 构建成功但产物体积警告

这是非阻塞问题，可以继续推送，但需要在最终结果里注明，例如：

- webpack 提示 wasm/js 体积较大
- 本次部署未受阻

### 构建成功但 IDE 诊断报版本噪音

如果真实 `gradlew` 构建成功，可继续部署，但要明确说明：

- IDE 诊断是环境噪音
- 实际构建通过

## 快速触发词

出现以下用户意图时，优先调用本 skill：

- “执行 web 部署”
- “帮我发布到线上”
- “把当前改动部署到 vercel”
- “更新 dist 并推送”
- “按 README 流程发布”
