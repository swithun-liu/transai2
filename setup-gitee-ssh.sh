#!/bin/bash

echo "🔑 配置 Gitee SSH 密钥..."

# 检查 SSH 密钥是否存在
if [ ! -f "$HOME/.ssh/id_ed25519.pub" ]; then
    echo "❌ 未找到 SSH 公钥文件"
    echo "请先生成 SSH 密钥："
    echo "ssh-keygen -t ed25519 -C 'your_email@example.com'"
    exit 1
fi

echo "✅ 找到 SSH 公钥文件"

# 显示公钥内容
echo ""
echo "📋 请复制以下公钥内容并添加到 Gitee："
echo "=========================================="
cat ~/.ssh/id_ed25519.pub
echo "=========================================="

echo ""
echo "📝 添加步骤："
echo "1. 登录 https://gitee.com"
echo "2. 点击头像 → 设置 → SSH 公钥"
echo "3. 标题：Mac SSH Key"
echo "4. 公钥：粘贴上面的内容"
echo "5. 点击确定"

echo ""
read -p "按回车键继续测试连接..."

# 测试 SSH 连接
echo ""
echo "🔗 测试 Gitee SSH 连接..."
ssh -T git@gitee.com

echo ""
echo "📋 下一步操作："
echo "1. 确保 Gitee 上已创建 transai2 仓库"
echo "2. 运行: git push -u gitee master"
echo "3. 测试: ./push-to-both.sh"

echo ""
echo "💡 提示：如果仓库不存在，请先访问 https://gitee.com/projects/new 创建"