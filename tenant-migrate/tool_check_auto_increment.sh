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
# 说明：检查目标数据库表的自增值
# 用法：bash tool_check_auto_increment.sh
# ##########################################

set -e

# 加载配置
source 0_config_common.sh

echo "========================================="
echo "  检查目标数据库自增值"
echo "========================================="
echo ""
echo "目标数据库配置："
echo "  主机: ${targetMysqlHost}:${targetMysqlPort}"
echo "  job_manage: ${targetJobManageDb}"
echo "  job_crontab: ${targetJobCrontabDb}"
echo "  job_execute: ${targetJobExecuteDb}"
echo "  job_file_gateway: ${targetJobFileGatewayDb}"
echo ""

# ========================================
# 定义需要检查的表（与设置偏移量的表一致）
# ========================================

# job_execute 数据库的表
JOB_EXECUTE_TABLES=(
  "task_instance"
  "step_instance"
  "step_instance_variable"
  "step_instance_rolling_task"
  "task_instance_variable"
  "gse_task"
  "gse_script_agent_task"
  "gse_script_execute_obj_task"
  "gse_file_agent_task"
  "gse_file_execute_obj_task"
  "operation_log"
  "file_source_task_log"
  "rolling_config"
)

# job_manage 数据库的表
JOB_MANAGE_TABLES=(
  "dangerous_rule"
  "notify_black_user_info"
  "account"
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
  "notify_trigger_policy"
  "notify_policy_role_target"
  "notify_role_target_channel"
  "task_favorite_plan"
  "task_favorite_template"
  "tag"
  "resource_tag"
)

# job_crontab 数据库的表
JOB_CRONTAB_TABLES=(
  "cron_job"
)

# job_file_gateway 数据库的表
JOB_FILE_GATEWAY_TABLES=(
  "file_source"
)

# ========================================
# 查询自增值的函数
# ========================================

function get_auto_increment() {
  local db_name=$1
  local table_name=$2
  
  # 先执行 ANALYZE TABLE 刷新 information_schema 中的统计信息缓存
  # 否则查询到的 AUTO_INCREMENT 可能是旧值
  mysql -h"${targetMysqlHost}" -P"${targetMysqlPort}" -u"${targetMysqlUser}" -p"${targetMysqlPassword}" \
    --default-character-set=utf8mb4 -N -e "ANALYZE TABLE \`${db_name}\`.\`${table_name}\`" >/dev/null 2>&1
  
  local sql="SELECT AUTO_INCREMENT FROM information_schema.tables WHERE table_schema='${db_name}' AND table_name='${table_name}'"
  
  result=$(mysql -h"${targetMysqlHost}" -P"${targetMysqlPort}" -u"${targetMysqlUser}" -p"${targetMysqlPassword}" \
    --default-character-set=utf8mb4 -N -e "${sql}" 2>/dev/null)
  
  if [ -z "$result" ] || [ "$result" = "NULL" ]; then
    echo "N/A"
  else
    echo "$result"
  fi
}

# ========================================
# 执行检查
# ========================================

echo "----------------------------------------"
echo "job_execute 数据库"
echo "----------------------------------------"
printf "  %-35s %s\n" "表名" "AUTO_INCREMENT"
printf "  %-35s %s\n" "-----------------------------------" "--------------------"
for table in "${JOB_EXECUTE_TABLES[@]}"; do
  value=$(get_auto_increment "${targetJobExecuteDb}" "${table}")
  printf "  %-35s %s\n" "${table}" "${value}"
done
echo ""

echo "----------------------------------------"
echo "job_manage 数据库"
echo "----------------------------------------"
printf "  %-35s %s\n" "表名" "AUTO_INCREMENT"
printf "  %-35s %s\n" "-----------------------------------" "--------------------"
for table in "${JOB_MANAGE_TABLES[@]}"; do
  value=$(get_auto_increment "${targetJobManageDb}" "${table}")
  printf "  %-35s %s\n" "${table}" "${value}"
done
echo ""

echo "----------------------------------------"
echo "job_crontab 数据库"
echo "----------------------------------------"
printf "  %-35s %s\n" "表名" "AUTO_INCREMENT"
printf "  %-35s %s\n" "-----------------------------------" "--------------------"
for table in "${JOB_CRONTAB_TABLES[@]}"; do
  value=$(get_auto_increment "${targetJobCrontabDb}" "${table}")
  printf "  %-35s %s\n" "${table}" "${value}"
done
echo ""

echo "----------------------------------------"
echo "job_file_gateway 数据库"
echo "----------------------------------------"
printf "  %-35s %s\n" "表名" "AUTO_INCREMENT"
printf "  %-35s %s\n" "-----------------------------------" "--------------------"
for table in "${JOB_FILE_GATEWAY_TABLES[@]}"; do
  value=$(get_auto_increment "${targetJobFileGatewayDb}" "${table}")
  printf "  %-35s %s\n" "${table}" "${value}"
done
echo ""

echo "========================================="
echo "检查完成"
echo "========================================="

