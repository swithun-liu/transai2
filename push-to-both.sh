#!/bin/bash

# TransAI Reader 双仓库推送脚本
# 同时推送到 GitHub 和 Gitee

echo "🚀 开始推送代码到 GitHub 和 Gitee..."

# 检查当前分支
CURRENT_BRANCH=$(git branch --show-current)
echo "📋 当前分支: $CURRENT_BRANCH"

# 检查远程仓库配置
echo "🌐 远程仓库配置:"
git remote -v

# 推送到 GitHub
echo ""
echo "🔗 推送到 GitHub..."
if git push origin $CURRENT_BRANCH; then
    echo "✅ GitHub 推送成功"
else
    echo "❌ GitHub 推送失败"
    exit 1
fi

# 推送到 Gitee
echo ""
echo "🔗 推送到 Gitee..."
if git push gitee $CURRENT_BRANCH; then
    echo "✅ Gitee 推送成功"
else
    echo "❌ Gitee 推送失败"
    exit 1
fi

echo ""
echo "🎉 双仓库推送完成！"
echo ""
echo "📊 仓库地址："
echo "   GitHub: https://github.com/swithun-liu/transai2"
echo "   Gitee:  https://gitee.com/swithun_liu/transai2"
echo ""
echo "💡 提示：下次可以直接运行 ./push-to-both.sh 一键推送"