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
# 说明：批量迁移多个业务数据
# 用法：bash batch_migrate_biz.sh <scopeIds> <tenantId> [--skip-prepare]
# 示例：
#   bash batch_migrate_biz.sh 1,2,3,4,5 tencent              # 执行 dump + prepare + 迁移
#   bash batch_migrate_biz.sh 1,2,3,4,5 tencent --skip-prepare  # 跳过 dump 和 prepare，直接迁移
# 
# 该脚本会：
# 1. 设置 defaultTenantId 为指定的租户ID
# 2. 执行 tool_dump_source_db.sh（从源数据库 dump 数据，只执行一次）
# 3. 执行 tool_prepare_db.sh（准备临时数据库，只执行一次）
# 4. 按顺序迁移每个业务（每个业务迁移前会先清理目标环境中的已有数据，确保幂等性）
#
# 可选参数：
#   --skip-prepare, -s  跳过 dump 和 prepare 阶段（适用于已经准备好临时数据库的情况）
# ##########################################

set -e

CONFIG_FILE="0_config_common.sh"
SKIP_PREPARE=false

# 解析参数
function show_usage() {
  echo "用法: bash batch_migrate_biz.sh <scopeIds> <tenantId> [--skip-prepare]"
  echo ""
  echo "示例:"
  echo "  bash batch_migrate_biz.sh 1,2,3,4,5 tencent              # 完整迁移"
  echo "  bash batch_migrate_biz.sh 1,2,3,4,5 tencent --skip-prepare  # 跳过准备阶段"
  echo ""
  echo "参数说明："
  echo "  scopeIds:       用英文逗号分隔的 bk_scope_id 列表"
  echo "  tenantId:       目标租户ID（如 tencent）"
  echo "  --skip-prepare: 可选，跳过 dump 和 prepare 阶段"
  echo "  -s:             --skip-prepare 的简写"
}

# 检查最少参数数量
if [ $# -lt 2 ]; then
  show_usage
  exit 1
fi

# 解析位置参数
IFS=',' read -ra SCOPE_IDS <<< "$1"
TARGET_TENANT_ID="$2"
shift 2

# 解析可选参数
while [ $# -gt 0 ]; do
  case "$1" in
    --skip-prepare|-s)
      SKIP_PREPARE=true
      shift
      ;;
    *)
      echo "错误：未知参数 $1"
      show_usage
      exit 1
      ;;
  esac
done

# 验证参数
if [ ${#SCOPE_IDS[@]} -eq 0 ]; then
  echo "错误：未提供有效的 scopeId"
  exit 1
fi

if [ -z "${TARGET_TENANT_ID}" ]; then
  echo "错误：未提供 tenantId"
  exit 1
fi

# 检查配置文件是否存在
if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "错误：配置文件 ${CONFIG_FILE} 不存在"
  exit 1
fi

echo "========================================="
echo "       BK-JOB 批量业务迁移工具"
echo "========================================="
echo ""
echo "目标租户: ${TARGET_TENANT_ID}"
if [ "${SKIP_PREPARE}" = true ]; then
  echo "模式: 跳过准备阶段（--skip-prepare）"
fi
echo ""
echo "待迁移的业务列表："
for scopeId in "${SCOPE_IDS[@]}"; do
  echo "  - bk_scope_id: ${scopeId}"
done
echo ""
echo "共 ${#SCOPE_IDS[@]} 个业务待迁移"
echo ""

# 记录开始时间
startTime=$(date +%s)

# 统计变量
successCount=0
failedCount=0
failedScopeIds=()

### 阶段0：设置 defaultTenantId
echo "========================================="
echo "阶段0：设置 defaultTenantId=${TARGET_TENANT_ID}"
echo "========================================="
sed -i.bak "s/^defaultTenantId=.*/defaultTenantId=\"${TARGET_TENANT_ID}\"/" ${CONFIG_FILE}
echo "✓ 已将 defaultTenantId 设置为 ${TARGET_TENANT_ID}"
echo ""

### 阶段1和阶段2：准备阶段（可跳过）
if [ "${SKIP_PREPARE}" = false ]; then
  ### 阶段1：从源数据库 dump 数据
  echo "========================================="
  echo "阶段1：从源数据库 dump 需要迁移的表..."
  echo "========================================="
  bash tool_dump_source_db.sh
  if [ $? -ne 0 ]; then
    echo ""
    echo "✗ tool_dump_source_db.sh 执行失败"
    exit 1
  fi
  echo ""
  echo "✓ 源数据库 dump 完成"
  echo ""

  ### 阶段2：准备临时数据库
  echo "========================================="
  echo "阶段2：准备临时数据库..."
  echo "========================================="
  bash tool_prepare_db.sh
  if [ $? -ne 0 ]; then
    echo ""
    echo "✗ tool_prepare_db.sh 执行失败"
    exit 1
  fi
  echo ""
  echo "✓ 临时数据库准备完成"
  echo ""
else
  echo "========================================="
  echo "阶段1-2：跳过准备阶段（--skip-prepare）"
  echo "========================================="
  echo "⚠️  请确保已手动执行过 tool_dump_source_db.sh 和 tool_prepare_db.sh"
  echo ""
fi

### 阶段3：按顺序迁移每个业务
echo "========================================="
echo "阶段3：开始迁移业务..."
echo "========================================="
echo ""

currentIndex=0
for scopeId in "${SCOPE_IDS[@]}"; do
  currentIndex=$((currentIndex + 1))
  
  echo "----------------------------------------"
  echo "[${currentIndex}/${#SCOPE_IDS[@]}] 开始迁移业务: bk_scope_id=${scopeId}"
  echo "----------------------------------------"
  
  # 先清理目标环境中该业务的已有数据（确保幂等性）
  echo "  清理目标环境中该业务的已有数据..."
  bash tool_clear_all_migration_data.sh ${scopeId} || true
  echo "  ✓ 清理完成（如果是首次迁移，可能没有数据需要清理）"
  echo ""
  
  # 执行迁移
  bash migrate.sh ${scopeId}
  
  if [ $? -eq 0 ]; then
    echo ""
    echo "✓ 业务 bk_scope_id=${scopeId} 迁移成功"
    successCount=$((successCount + 1))
  else
    echo ""
    echo "✗ 业务 bk_scope_id=${scopeId} 迁移失败"
    failedCount=$((failedCount + 1))
    failedScopeIds+=("${scopeId}")
    # 继续迁移下一个业务，不中断
  fi
  
  echo ""
done

# 记录结束时间
endTime=$(date +%s)
duration=$((endTime - startTime))

# 输出迁移报告
echo "========================================="
echo "          批量迁移完成报告"
echo "========================================="
echo ""
echo "目标租户: ${TARGET_TENANT_ID}"
echo ""
echo "迁移统计："
echo "  总计: ${#SCOPE_IDS[@]} 个业务"
echo "  成功: ${successCount} 个"
echo "  失败: ${failedCount} 个"
echo "  耗时: ${duration} 秒"
echo ""

if [ ${failedCount} -gt 0 ]; then
  echo "失败的业务列表："
  for failedScopeId in "${failedScopeIds[@]}"; do
    echo "  - bk_scope_id: ${failedScopeId}"
  done
  echo ""
  echo "可以使用以下命令重新迁移失败的业务："
  echo "  bash tool_clear_all_migration_data.sh <scopeId>  # 先清理"
  echo "  bash migrate.sh <scopeId>                        # 再迁移"
  echo ""
  exit 1
else
  echo "所有业务迁移成功！"
  echo ""
fi
