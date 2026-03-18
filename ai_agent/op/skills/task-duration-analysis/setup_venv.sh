#!/bin/bash
# Task Duration Analysis 虚拟环境设置脚本
#
# 此脚本会:
# 1. 创建 Python 虚拟环境
# 2. 激活虚拟环境
# 3. 安装 requirements.txt 中的依赖

set -e  # 遇到错误立即退出

SKILL_DIR="$(cd "$(dirname "$0")" && pwd)"
VENV_DIR="$SKILL_DIR/venv"

echo "🚀 开始设置 Task Duration Analysis 虚拟环境..."
echo "Skill 目录: $SKILL_DIR"

# 检查 Python 3 是否可用
if ! command -v python3 &> /dev/null; then
    echo "❌ 错误: 未找到 python3，请先安装 Python 3"
    exit 1
fi

PYTHON_VERSION=$(python3 --version | cut -d' ' -f2)
echo "✅ 检测到 Python 版本: $PYTHON_VERSION"

# 检查是否已存在虚拟环境
if [ -d "$VENV_DIR" ]; then
    echo "⚠️  虚拟环境已存在: $VENV_DIR"
    echo "ℹ️  保留现有虚拟环境，仅更新依赖..."
    echo "   如需重新创建，请先手动删除: rm -rf venv/"
fi

# 创建虚拟环境
if [ ! -d "$VENV_DIR" ]; then
    echo "📦 创建虚拟环境..."
    python3 -m venv "$VENV_DIR"
    echo "✅ 虚拟环境创建成功"
fi

# 激活虚拟环境
echo "🔌 激活虚拟环境..."
source "$VENV_DIR/bin/activate"

# 升级 pip
echo "⬆️  升级 pip..."
pip install --upgrade pip > /dev/null 2>&1

# 安装依赖
if [ -f "$SKILL_DIR/requirements.txt" ]; then
    echo "📥 安装依赖（使用腾讯源）..."
    pip install -i https://mirrors.tencent.com/repository/pypi/tencent_pypi/simple --extra-index-url https://mirrors.tencent.com/pypi/simple/ -r "$SKILL_DIR/requirements.txt"
    echo "✅ 依赖安装成功"
else
    echo "⚠️  未找到 requirements.txt 文件"
fi

# 显示已安装的包
echo ""
echo "📋 已安装的包:"
pip list | grep -E "(requests|anthropic|mcp)" || echo "  (无常见包)"

echo ""
echo "✨ 虚拟环境设置完成!"
echo ""
echo "使用方法:"
echo "  激活虚拟环境: source $VENV_DIR/bin/activate"
echo "  退出虚拟环境: deactivate"
echo ""
echo "运行脚本示例:"
echo "  source $VENV_DIR/bin/activate && python scripts/example.py"
