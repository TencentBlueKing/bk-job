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
# 说明：从源数据库 dump 需要迁移的表到 SQL 文件
# 功能：
# 1. 从源数据库 dump 需要迁移的表（表结构 + 数据）
# 2. 生成 SQL 文件保存到 source_dump/ 目录
# 3. 这个脚本在批次开始前手动执行一次即可
# 
# 使用场景：
# - 批次1（业务1-100）开始前执行一次
# - 批次2（业务101-150）开始前再执行一次
# ##########################################

source 0_config_common.sh

# SQL 文件保存目录
sourceDumpDir="source_dump"

echo "========================================="
echo "从源数据库 dump 需要迁移的表..."
echo "源数据库: ${sourceMysqlHost}:${sourceMysqlPort}"
echo "dump 文件保存目录: ${sourceDumpDir}/"
echo "========================================="

# 创建 dump 目录
if [[ ! -d "${sourceDumpDir}" ]]; then
  mkdir -p "${sourceDumpDir}"
  echo "  - 创建目录: ${sourceDumpDir}/"
else
  echo "  - 目录已存在: ${sourceDumpDir}/"
  echo "  ⚠️  警告：将覆盖已有的 dump 文件"
fi

echo ""
echo "开始 dump 需要迁移的表..."

# 定义需要迁移的表清单
# job_manage 库需要迁移的表
JOB_MANAGE_TABLES=(
  # 全局资源
  "dangerous_rule"
  "notify_black_user_info"
  "notify_trigger_policy"
  # 业务资源
  "account"
  "script"
  "script_version"
  "task_template"
  "task_template_step"
  "task_template_step_approval"
  "task_template_step_file"
  "task_template_step_file_list"
  "task_template_step_script"
  "task_template_variable"
  "task_plan"
  "task_plan_step"
  "task_plan_step_approval"
  "task_plan_step_file"
  "task_plan_step_file_list"
  "task_plan_step_script"
  "task_plan_variable"
  "credential"
  "notify_policy_role_target"
  "notify_role_target_channel"
  "task_favorite_plan"
  "task_favorite_template"
  "tag"
  "resource_tag"
)

# job_crontab 库需要迁移的表
JOB_CRONTAB_TABLES=(
  "cron_job"
)

# job_file_gateway 库需要迁移的表
JOB_FILE_GATEWAY_TABLES=(
  "file_source"
  "file_source_share"
)

# 函数：dump 指定表到 SQL 文件
function dumpTablesToFile() {
  sourceDb=$1
  shift
  tables=("$@")
  
  echo ""
  echo "  [${sourceDb}] 开始 dump ${#tables[@]} 张表..."
  
  # 拼接表名列表
  tableList=""
  for table in "${tables[@]}"; do
    tableList="${tableList} ${table}"
  done
  
  dumpFile="${sourceDumpDir}/${sourceDb}.sql"
  
  # 从源数据库 dump 指定表的结构和数据
  echo "    - 正在从源数据库导出表..."
  echo "    - 表清单: ${tableList}"
  
  mysqldump -h${sourceMysqlHost} -P${sourceMysqlPort} -u${sourceMysqlUser} -p${sourceMysqlPassword} \
    --default-character-set=utf8mb4 \
    --single-transaction \
    --quick \
    --lock-tables=false \
    --skip-add-drop-table \
    --skip-comments \
    --set-gtid-purged=OFF \
    ${sourceDb} ${tableList} > ${dumpFile}
  
  if [[ $? -ne 0 ]]; then
    echo "    ✗ dump 失败！"
    return 1
  fi
  
  echo "    - dump 完成"
  echo "    - 文件: ${dumpFile}"
  echo "    - 大小: $(du -h ${dumpFile} | cut -f1)"
  echo "    ✓ [${sourceDb}] ${#tables[@]} 张表 dump 完成"
  
  return 0
}

# dump job_manage 需要迁移的表
dumpTablesToFile "${sourceJobManageDb}" "${JOB_MANAGE_TABLES[@]}"
if [[ $? -ne 0 ]]; then
  echo ""
  echo "✗ job_manage 数据库表 dump 失败，请检查源数据库连接和权限"
  exit 1
fi

# dump job_crontab 需要迁移的表
dumpTablesToFile "${sourceJobCrontabDb}" "${JOB_CRONTAB_TABLES[@]}"
if [[ $? -ne 0 ]]; then
  echo ""
  echo "✗ job_crontab 数据库表 dump 失败，请检查源数据库连接和权限"
  exit 1
fi

# dump job_file_gateway 需要迁移的表
dumpTablesToFile "${sourceJobFileGatewayDb}" "${JOB_FILE_GATEWAY_TABLES[@]}"
if [[ $? -ne 0 ]]; then
  echo ""
  echo "✗ job_file_gateway 数据库表 dump 失败，请检查源数据库连接和权限"
  exit 1
fi

echo ""
echo "========================================="
echo "dump 完成！"
echo ""
echo "已生成的 SQL 文件："
echo "  - ${sourceDumpDir}/${sourceJobManageDb}.sql"
echo "  - ${sourceDumpDir}/${sourceJobCrontabDb}.sql"
echo "  - ${sourceDumpDir}/${sourceJobFileGatewayDb}.sql"
echo ""
echo "这些文件将在后续的迁移过程中使用。"
echo "下次需要重新 dump 时，再次执行本脚本即可。"
echo "========================================="
