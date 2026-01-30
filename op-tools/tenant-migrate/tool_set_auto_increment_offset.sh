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

# 加载配置
source 0_config_common.sh

echo "========================================="
echo "  设置新环境数据库自增偏移量"
echo "========================================="
echo ""
echo "[说明] 此脚本会在设置前比较当前偏移量与目标偏移量："
echo "       - 若当前偏移量 < 目标偏移量：执行设置"
echo "       - 若当前偏移量 >= 目标偏移量：跳过该表并打印警告"
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
# 获取当前表自增偏移量的函数
# ========================================

function get_current_auto_increment() {
  local db_name=$1
  local table_name=$2
  
  # 先执行 ANALYZE TABLE 刷新 information_schema 中的统计信息缓存
  # 否则查询到的 AUTO_INCREMENT 可能是旧值
  mysql -h"${targetMysqlHost}" -P"${targetMysqlPort}" -u"${targetMysqlUser}" -p"${targetMysqlPassword}" \
    --default-character-set=utf8mb4 -N -e "ANALYZE TABLE \`${db_name}\`.\`${table_name}\`" >/dev/null 2>&1
  
  local sql="SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = '${db_name}' AND TABLE_NAME = '${table_name}'"
  
  # 执行 SQL 获取当前 AUTO_INCREMENT 值
  local current_value=$(mysql -h"${targetMysqlHost}" -P"${targetMysqlPort}" -u"${targetMysqlUser}" -p"${targetMysqlPassword}" \
    --default-character-set=utf8mb4 -N -e "${sql}" 2>/dev/null)
  
  # 如果获取失败或为空，返回 0
  if [ -z "${current_value}" ] || [ "${current_value}" == "NULL" ]; then
    echo "0"
  else
    echo "${current_value}"
  fi
}

# ========================================
# 设置自增偏移量的函数
# 返回值：
#   0 - 设置成功
#   1 - 设置失败
#   2 - 跳过（当前偏移量 >= 目标偏移量）
# ========================================

function set_auto_increment() {
  local db_name=$1
  local table_name=$2
  local target_offset=$3
  
  # 获取当前表的 AUTO_INCREMENT 值
  local current_offset=$(get_current_auto_increment "${db_name}" "${table_name}")
  
  echo "  [INFO] ${db_name}.${table_name}: 当前偏移量=${current_offset}, 目标偏移量=${target_offset}"
  
  # 比较当前偏移量和目标偏移量
  if [ "${current_offset}" -ge "${target_offset}" ]; then
    echo "  [WARN] ${db_name}.${table_name}: 当前偏移量(${current_offset}) >= 目标偏移量(${target_offset})，跳过设置。"
    echo "         请检查：可能是业务数据已增长到该量级，或该表已设置过偏移量。"
    return 2
  fi
  
  local sql="ALTER TABLE \`${db_name}\`.\`${table_name}\` AUTO_INCREMENT = ${target_offset}"
  
  echo -n "  设置 ${db_name}.${table_name} AUTO_INCREMENT = ${target_offset} ... "
  
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
SKIP_COUNT=0

# 处理 set_auto_increment 返回值的辅助函数
function handle_set_result() {
  local ret=$1
  case $ret in
    0)
      SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
      ;;
    1)
      FAIL_COUNT=$((FAIL_COUNT + 1))
      ;;
    2)
      SKIP_COUNT=$((SKIP_COUNT + 1))
      ;;
  esac
}

echo "----------------------------------------"
echo "设置 job_execute 数据库..."
echo "----------------------------------------"
for table in "${!JOB_EXECUTE_TABLES[@]}"; do
  offset=${JOB_EXECUTE_TABLES[$table]}
  set_auto_increment "${targetJobExecuteDb}" "${table}" "${offset}"
  handle_set_result $?
done
echo ""

echo "----------------------------------------"
echo "设置 job_manage 数据库..."
echo "----------------------------------------"
for table in "${!JOB_MANAGE_TABLES[@]}"; do
  offset=${JOB_MANAGE_TABLES[$table]}
  set_auto_increment "${targetJobManageDb}" "${table}" "${offset}"
  handle_set_result $?
done
echo ""

echo "----------------------------------------"
echo "设置 job_crontab 数据库..."
echo "----------------------------------------"
for table in "${!JOB_CRONTAB_TABLES[@]}"; do
  offset=${JOB_CRONTAB_TABLES[$table]}
  set_auto_increment "${targetJobCrontabDb}" "${table}" "${offset}"
  handle_set_result $?
done
echo ""

echo "----------------------------------------"
echo "设置 job_file_gateway 数据库..."
echo "----------------------------------------"
for table in "${!JOB_FILE_GATEWAY_TABLES[@]}"; do
  offset=${JOB_FILE_GATEWAY_TABLES[$table]}
  set_auto_increment "${targetJobFileGatewayDb}" "${table}" "${offset}"
  handle_set_result $?
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
echo "  跳过: ${SKIP_COUNT}"
echo "  失败: ${FAIL_COUNT}"
echo ""

if [ ${FAIL_COUNT} -gt 0 ]; then
  echo "⚠️  部分表设置失败，请检查错误信息"
  exit 1
fi

if [ ${SKIP_COUNT} -gt 0 ]; then
  echo "⚠️  有 ${SKIP_COUNT} 张表因当前偏移量 >= 目标偏移量而被跳过"
  echo "   请检查上述 [WARN] 日志，确认是业务正常增长还是重复执行导致"
fi

if [ ${FAIL_COUNT} -eq 0 ]; then
  echo "✓ 自增偏移量设置完成！"
fi

