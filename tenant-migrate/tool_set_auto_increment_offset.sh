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
# 说明：设置新环境数据库表的自增偏移量
# 用法：bash tool_set_auto_increment_offset.sh
# 
# 目的：
# 迁移工具设计基于 ID 不变的原则，需要保证新老环境的 ID 不会冲突。
# 在新环境开始使用后，会产生新的记录。为避免与迁移数据的 ID 冲突，
# 需要在迁移前设置新环境表的自增偏移量。
# ##########################################

set -e

# 加载配置
source 0_config_common.sh

echo "========================================="
echo "  设置新环境数据库自增偏移量"
echo "========================================="
echo ""
echo "[警告] 此脚本只能执行一次！在业务使用后重复执行会导致主键冲突！"
echo ""
echo "目标数据库配置："
echo "  主机: ${targetMysqlHost}:${targetMysqlPort}"
echo "  job_manage: ${targetJobManageDb}"
echo "  job_crontab: ${targetJobCrontabDb}"
echo "  job_execute: ${targetJobExecuteDb}"
echo "  job_file_gateway: ${targetJobFileGatewayDb}"
echo ""

# ========================================
# 定义需要设置偏移量的表
# ========================================

# job_execute 数据库的表
declare -A JOB_EXECUTE_TABLES=(
  ["task_instance"]=30000000000
  ["step_instance"]=30000000000
  ["step_instance_variable"]=20000000
  ["step_instance_rolling_task"]=100000
  ["task_instance_variable"]=1000000000
  ["gse_task"]=50000000000
  ["gse_script_agent_task"]=50000000000
  ["gse_script_execute_obj_task"]=200000000000
  ["gse_file_agent_task"]=50000000000
  ["gse_file_execute_obj_task"]=200000000000
  ["operation_log"]=1000000000
  ["file_source_task_log"]=3000000
  ["rolling_config"]=40000
)

# job_manage 数据库的表
declare -A JOB_MANAGE_TABLES=(
  ["dangerous_rule"]=100
  ["notify_black_user_info"]=100
  ["account"]=31000
  ["script_version"]=4100000
  ["task_template"]=400000
  ["task_template_step"]=400000
  ["task_template_step_approval"]=400000
  ["task_template_step_file"]=400000
  ["task_template_step_file_list"]=400000
  ["task_template_step_script"]=400000
  ["task_template_variable"]=4100000
  ["task_plan"]=5100000
  ["task_plan_step"]=500000
  ["task_plan_step_approval"]=400000
  ["task_plan_step_file"]=400000
  ["task_plan_step_file_list"]=400000
  ["task_plan_step_script"]=500000
  ["task_plan_variable"]=400000
  ["notify_trigger_policy"]=3000
  ["notify_policy_role_target"]=3000
  ["notify_role_target_channel"]=3000
  ["task_favorite_plan"]=2000
  ["task_favorite_template"]=2000
  ["tag"]=2000
  ["resource_tag"]=5000
)

# job_crontab 数据库的表
declare -A JOB_CRONTAB_TABLES=(
  ["cron_job"]=4100000
)

# job_file_gateway 数据库的表
declare -A JOB_FILE_GATEWAY_TABLES=(
  ["file_source"]=2000
)

# ========================================
# 设置自增偏移量的函数
# ========================================

function set_auto_increment() {
  local db_name=$1
  local table_name=$2
  local offset=$3
  
  local sql="ALTER TABLE \`${db_name}\`.\`${table_name}\` AUTO_INCREMENT = ${offset}"
  
  echo -n "  设置 ${db_name}.${table_name} AUTO_INCREMENT = ${offset} ... "
  
  # 执行 SQL
  result=$(mysql -h"${targetMysqlHost}" -P"${targetMysqlPort}" -u"${targetMysqlUser}" -p"${targetMysqlPassword}" \
    --default-character-set=utf8mb4 -N -e "${sql}" 2>&1)
  
  if [ $? -eq 0 ]; then
    echo "✓"
    return 0
  else
    echo "✗"
    echo "    错误: ${result}"
    return 1
  fi
}

# ========================================
# 执行设置
# ========================================

SUCCESS_COUNT=0
FAIL_COUNT=0

echo "----------------------------------------"
echo "设置 job_execute 数据库..."
echo "----------------------------------------"
for table in "${!JOB_EXECUTE_TABLES[@]}"; do
  offset=${JOB_EXECUTE_TABLES[$table]}
  if set_auto_increment "${targetJobExecuteDb}" "${table}" "${offset}"; then
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
done
echo ""

echo "----------------------------------------"
echo "设置 job_manage 数据库..."
echo "----------------------------------------"
for table in "${!JOB_MANAGE_TABLES[@]}"; do
  offset=${JOB_MANAGE_TABLES[$table]}
  if set_auto_increment "${targetJobManageDb}" "${table}" "${offset}"; then
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
done
echo ""

echo "----------------------------------------"
echo "设置 job_crontab 数据库..."
echo "----------------------------------------"
for table in "${!JOB_CRONTAB_TABLES[@]}"; do
  offset=${JOB_CRONTAB_TABLES[$table]}
  if set_auto_increment "${targetJobCrontabDb}" "${table}" "${offset}"; then
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
done
echo ""

echo "----------------------------------------"
echo "设置 job_file_gateway 数据库..."
echo "----------------------------------------"
for table in "${!JOB_FILE_GATEWAY_TABLES[@]}"; do
  offset=${JOB_FILE_GATEWAY_TABLES[$table]}
  if set_auto_increment "${targetJobFileGatewayDb}" "${table}" "${offset}"; then
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
done
echo ""

# ========================================
# 输出报告
# ========================================

echo "========================================="
echo "          设置完成报告"
echo "========================================="
echo ""
echo "结果统计："
echo "  成功: ${SUCCESS_COUNT}"
echo "  失败: ${FAIL_COUNT}"
echo ""

if [ ${FAIL_COUNT} -gt 0 ]; then
  echo "⚠️  部分表设置失败，请检查错误信息"
  exit 1
else
  echo "✓ 自增偏移量设置完成！"
fi

