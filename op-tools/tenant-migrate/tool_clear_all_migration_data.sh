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
# 说明：清理目标环境中已迁移的业务数据（回滚用）
# 警告：此操作会删除目标环境中指定业务的所有数据，请谨慎使用！
# 用法：sh tool_clear_all_migration_data.sh <scopeId>
# 示例：sh tool_clear_all_migration_data.sh 2
# ##########################################

# 检查参数
if [ $# -ne 1 ]; then
  echo "用法: sh tool_clear_all_migration_data.sh <scopeId>"
  echo "示例: sh tool_clear_all_migration_data.sh 2"
  exit 1
fi

scopeId=$1

# 加载配置信息
source 0_config_common.sh

echo "========================================="
echo "警告：此操作将删除目标环境中的业务数据！"
echo "scopeId: ${scopeId}"
echo "========================================="

# 从目标环境数据库查询 scopeId 对应的 app_id
# 注意：必须从 targetDb 查询，因为迁移后的数据使用的是 target 环境的 app_id
echo "从目标环境查询 scopeId=${scopeId} 对应的 app_id..."
queryAppIdByScopeIdSql="select app_id from \`${targetJobManageDb}\`.\`application\` where bk_scope_id='${scopeId}'"
executeSqlInTargetDb "${queryAppIdByScopeIdSql}"
appId=$(echo ${dbResult}|awk -F" " '{print $2}')

if [ -z "${appId}" ]; then
  echo "错误：在目标环境中未找到 scopeId=${scopeId} 对应的业务数据"
  exit 1
fi

echo "目标环境 app_id: ${appId}"
echo "开始清理目标环境中的业务数据..."

# 清理job_manage中的数据
echo "清理 ${targetJobManageDb} 中的业务数据..."

# (5)定时任务（先删除，因为有外键依赖）
echo "  删除定时任务..."
executeSqlInTargetDb "delete from \`${targetJobCrontabDb}\`.\`cron_job\` where app_id=${appId};"

# (4)执行方案
echo "  删除执行方案..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_plan_variable\` where plan_id in (select id from \`${targetJobManageDb}\`.\`task_plan\` where app_id=${appId});"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_plan_step_script\` where step_id in (select id from \`${targetJobManageDb}\`.\`task_plan_step\` where plan_id in (select id from \`${targetJobManageDb}\`.\`task_plan\` where app_id=${appId}));"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_plan_step_file_list\` where step_id in (select id from \`${targetJobManageDb}\`.\`task_plan_step\` where plan_id in (select id from \`${targetJobManageDb}\`.\`task_plan\` where app_id=${appId}));"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_plan_step_file\` where step_id in (select id from \`${targetJobManageDb}\`.\`task_plan_step\` where plan_id in (select id from \`${targetJobManageDb}\`.\`task_plan\` where app_id=${appId}));"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_plan_step_approval\` where step_id in (select id from \`${targetJobManageDb}\`.\`task_plan_step\` where plan_id in (select id from \`${targetJobManageDb}\`.\`task_plan\` where app_id=${appId}));"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_plan_step\` where plan_id in (select id from \`${targetJobManageDb}\`.\`task_plan\` where app_id=${appId});"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_plan\` where app_id=${appId};"

# (3)作业模板
echo "  删除作业模板..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_template_variable\` where template_id in (select id from \`${targetJobManageDb}\`.\`task_template\` where app_id=${appId});"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_template_step_script\` where step_id in (select id from \`${targetJobManageDb}\`.\`task_template_step\` where template_id in (select id from \`${targetJobManageDb}\`.\`task_template\` where app_id=${appId}));"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_template_step_file_list\` where step_id in (select id from \`${targetJobManageDb}\`.\`task_template_step\` where template_id in (select id from \`${targetJobManageDb}\`.\`task_template\` where app_id=${appId}));"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_template_step_file\` where step_id in (select id from \`${targetJobManageDb}\`.\`task_template_step\` where template_id in (select id from \`${targetJobManageDb}\`.\`task_template\` where app_id=${appId}));"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_template_step_approval\` where step_id in (select id from \`${targetJobManageDb}\`.\`task_template_step\` where template_id in (select id from \`${targetJobManageDb}\`.\`task_template\` where app_id=${appId}));"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_template_step\` where template_id in (select id from \`${targetJobManageDb}\`.\`task_template\` where app_id=${appId});"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_template\` where app_id=${appId};"

# (2)脚本
echo "  删除脚本..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`script_version\` where script_id in (select id from \`${targetJobManageDb}\`.\`script\` where app_id=${appId});"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`script\` where app_id=${appId};"

# (1)账号
echo "  删除账号..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`account\` where app_id=${appId};"

# (6)凭证
echo "  删除凭证..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`credential\` where app_id=${appId};"

# (7)通知策略
echo "  删除通知策略..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`notify_role_target_channel\` where role_target_id in (select id from \`${targetJobManageDb}\`.\`notify_policy_role_target\` where policy_id in (select id from \`${targetJobManageDb}\`.\`notify_trigger_policy\` where app_id=${appId}));"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`notify_policy_role_target\` where policy_id in (select id from \`${targetJobManageDb}\`.\`notify_trigger_policy\` where app_id=${appId});"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`notify_trigger_policy\` where app_id=${appId};"

# (8)收藏
echo "  删除收藏..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_favorite_plan\` where app_id=${appId};"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`task_favorite_template\` where app_id=${appId};"

# (9)标签
echo "  删除标签..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`resource_tag\` where tag_id in (select id from \`${targetJobManageDb}\`.\`tag\` where app_id=${appId});"
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`tag\` where app_id=${appId};"

# (10)文件源
echo "  删除文件源..."
executeSqlInTargetDb "delete from \`${targetJobFileGatewayDb}\`.\`file_source_share\` where file_source_id in (select id from \`${targetJobFileGatewayDb}\`.\`file_source\` where app_id=${appId});"
executeSqlInTargetDb "delete from \`${targetJobFileGatewayDb}\`.\`file_source\` where app_id=${appId};"

echo "========================================="
echo "业务数据清理完成！"
echo "scopeId ${scopeId} (app_id: ${appId}) 的所有数据已从目标环境删除"
echo ""
echo "已清理的资源："
echo "  ✅ 账号"
echo "  ✅ 脚本"
echo "  ✅ 作业模板"
echo "  ✅ 执行方案"
echo "  ✅ 定时任务"
echo "  ✅ 凭证"
echo "  ✅ 通知策略"
echo "  ✅ 收藏"
echo "  ✅ 标签"
echo "  ✅ 文件源"
echo "========================================="
