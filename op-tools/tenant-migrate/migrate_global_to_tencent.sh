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
# 说明：一键迁移全局资源到 tencent 租户
# 用法：bash migrate_global_to_tencent.sh
# 
# 该脚本会：
# 1. 从源数据库 dump 需要迁移的表
# 2. 准备临时数据库
# 3. 设置 defaultTenantId 为 tencent
# 4. 执行全局资源迁移
# ##########################################

set -e

echo "========================================="
echo "  一键迁移全局资源到 tencent 租户"
echo "========================================="
echo ""

CONFIG_FILE="0_config_common.sh"
TARGET_TENANT_ID="tencent"

# 检查配置文件是否存在
if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "错误：配置文件 ${CONFIG_FILE} 不存在"
  exit 1
fi

# 记录开始时间
startTime=$(date +%s)

### 步骤1：从源数据库 dump 数据
echo "========================================="
echo "步骤 1/4: 从源数据库 dump 需要迁移的表"
echo "========================================="
bash tool_dump_source_db.sh
if [[ $? -ne 0 ]]; then
  echo "错误: tool_dump_source_db.sh 执行失败"
  exit 1
fi
echo ""
echo "✓ 源数据库 dump 完成"
echo ""

### 步骤2：准备临时数据库
echo "========================================="
echo "步骤 2/4: 准备临时数据库"
echo "========================================="
bash tool_prepare_db.sh
if [[ $? -ne 0 ]]; then
  echo "错误: tool_prepare_db.sh 执行失败"
  exit 1
fi
echo ""
echo "✓ 临时数据库准备完成"
echo ""

### 步骤3：设置 defaultTenantId
echo "========================================="
echo "步骤 3/4: 设置 defaultTenantId=${TARGET_TENANT_ID}"
echo "========================================="
sed -i.bak "s/^defaultTenantId=.*/defaultTenantId=\"${TARGET_TENANT_ID}\"/" ${CONFIG_FILE}
echo "✓ 已将 defaultTenantId 设置为 ${TARGET_TENANT_ID}"
echo ""

### 步骤4：执行全局资源迁移
echo "========================================="
echo "步骤 4/4: 执行全局资源迁移"
echo "========================================="
bash migrate_global.sh
if [[ $? -ne 0 ]]; then
  echo "错误: migrate_global.sh 执行失败"
  exit 1
fi

# 记录结束时间
endTime=$(date +%s)
duration=$((endTime - startTime))

echo ""
echo "========================================="
echo "  全局资源迁移完成！"
echo "========================================="
echo ""
echo "目标租户: ${TARGET_TENANT_ID}"
echo "总耗时: ${duration} 秒"
echo ""
echo "下一步："
echo "  执行业务迁移: bash batch_migrate_biz.sh <scopeIds> ${TARGET_TENANT_ID}"
echo ""

