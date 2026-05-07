#!/bin/bash

# 虚拟环境设置脚本
# 用于自动化创建虚拟环境和安装依赖

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="$SCRIPT_DIR/venv"

echo "=== KQL 查询指导 Skill - 虚拟环境设置 ==="

# 检查 Python 3 是否可用
if ! command -v python3 &> /dev/null; then
    echo "错误: 未找到 python3，请先安装 Python 3"
    exit 1
fi

echo "✓ 找到 Python: $(python3 --version)"

# 创建虚拟环境（如果不存在）
if [ ! -d "$VENV_DIR" ]; then
    echo "创建虚拟环境..."
    python3 -m venv "$VENV_DIR"
    echo "✓ 虚拟环境已创建"
else
    echo "✓ 虚拟环境已存在"
fi

# 激活虚拟环境
echo "激活虚拟环境..."
source "$VENV_DIR/bin/activate"

# 升级 pip
echo "升级 pip..."
pip install --upgrade pip > /dev/null 2>&1

# 安装依赖（如果有）
if [ -f "$SCRIPT_DIR/requirements.txt" ]; then
    echo "安装依赖..."
    pip install -r "$SCRIPT_DIR/requirements.txt" > /dev/null 2>&1
    echo "✓ 依赖已安装"
fi

echo ""
echo "=== 设置完成 ==="
echo "虚拟环境位置: $VENV_DIR"
echo ""
echo "使用方法："
echo "  激活: source venv/bin/activate"
echo "  退出: deactivate"
echo ""
echo "快速测试："
echo "  python3 scripts/generate_kql.py --request-id \"test-id\" --level ERROR"
echo ""
