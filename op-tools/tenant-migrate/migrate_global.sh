#!/bin/bash
#
# Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
#
# Copyright (C) 2021 Tencent.  All rights reserved.
#
# BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
#
# License for BK-JOB蓝鲸智云作业平台:
# --------------------------------------------------------------------
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
# documentation files (the "Software"), to deal in the Software without restriction, including without limitation
# the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
# to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of
# the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
# THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
# CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
# IN THE SOFTWARE.
#

# ##########################################
# 说明：租户内全局性资源迁移主脚本
# 用途：一键完成全局资源的迁移流程
# 执行时机：新环境搭建后立即执行一次
# ##########################################

set -e

echo "========================================="
echo "       BK-JOB 全局资源迁移工具"
echo "========================================="
echo ""
echo "本工具用于迁移租户内全局性、业务无关的资源"
echo "包括：高危规则、通知黑名单、公共脚本"
echo ""
echo "注意：此脚本只需执行一次，在业务迁移之前执行"
echo ""

# 检查锁文件
LOCK_FILE=".global_migration.lock"
if [[ -f "${LOCK_FILE}" ]]; then
  echo "警告: 发现锁文件 ${LOCK_FILE}"
  echo "可能有其他全局迁移正在进行，或者上次迁移异常中断"
  echo "如果确认没有其他迁移在运行，请执行: rm ${LOCK_FILE}"
  exit 1
fi

# 创建锁文件
touch ${LOCK_FILE}
trap "rm -f ${LOCK_FILE}" EXIT

echo ""
echo "========================================="
echo "步骤 1/2: 生成全局迁移数据"
echo "========================================="
bash 1_gen_global_data.sh
if [[ $? -ne 0 ]]; then
  echo "错误: 生成全局迁移数据失败"
  exit 1
fi

echo ""
echo "========================================="
echo "步骤 2/2: 应用全局迁移数据"
echo "========================================="
bash 2_apply_global_data.sh
if [[ $? -ne 0 ]]; then
  echo "错误: 应用全局迁移数据失败"
  exit 1
fi

echo ""
echo "========================================="
echo "        全局资源迁移完成！"
echo "========================================="
echo ""
echo "已迁移的资源："
echo "  ✅ dangerous_rule (高危规则)"
echo "  ✅ notify_black_user_info (通知黑名单)"
echo "  ✅ script (公共脚本)"
echo "  ✅ script_version (公共脚本版本)"
echo ""
echo "下一步："
echo "  执行业务迁移: bash migrate.sh <scopeId>"
echo ""

