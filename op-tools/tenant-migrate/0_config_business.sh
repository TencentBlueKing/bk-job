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

# ###### 业务配置信息 ######
source 0_config_common.sh

# 迁移数据来源：源环境业务scopeId
sourceScopeId=
# 从scopeId查询到的app_id（源环境）
sourceAppId=
# 目标环境的app_id（可能与源环境不同）
targetAppId=

# 临时数据库表名前缀
prefix="new_"
tmpJobManageDb="${prefix}job_manage"
tmpJobCrontabDb="${prefix}job_crontab"
tmpJobFileGatewayDb="${prefix}job_file_gateway"

# 需要迁移的表列表
# job_manage 库中的业务相关表
jobManageTables=(
  # 账号
  account
  # 脚本
  script
  script_version
  # 作业模板
  task_template
  task_template_step
  task_template_step_approval
  task_template_step_file
  task_template_step_file_list
  task_template_step_script
  task_template_variable
  # 执行方案
  task_plan
  task_plan_step
  task_plan_step_approval
  task_plan_step_file
  task_plan_step_file_list
  task_plan_step_script
  task_plan_variable
  # 凭证
  credential
  # 通知策略
  notify_trigger_policy
  notify_policy_role_target
  notify_role_target_channel
  # 收藏
  task_favorite_plan
  task_favorite_template
  # 标签
  tag
  resource_tag
)

# job_crontab 库中的业务相关表
jobCrontabTables=(cron_job)

# job_file_gateway 库中的业务相关表
jobFileGatewayTables=(file_source file_source_share)
# ############   配置信息结束    #############

# 从命令行参数读取源业务scopeId：
function showRequireParamMsg(){
  echo "请传入参数：参数1：源环境业务scopeId"
}

# 参数1：源环境业务scopeId
if [[ "$1" != "" ]];then
  sourceScopeId=$1
else
  showRequireParamMsg
  exit 1
fi
echo "源环境业务scopeId: ${sourceScopeId}"

# 从数据库查询 scopeId 对应的 app_id
echo "查询 scopeId=${sourceScopeId} 对应的 app_id..."
queryAppIdByScopeIdSql="select app_id from \`${sourceJobManageDb}\`.\`application\` where bk_scope_id='${sourceScopeId}'"
executeSqlInSourceDb "${queryAppIdByScopeIdSql}"
sourceAppId=$(echo ${dbResult}|awk -F" " '{print $2}')

if [[ "${sourceAppId}" == "" ]];then
  echo "错误：未找到 scopeId=${sourceScopeId} 对应的业务数据"
  exit 1
fi

echo "  源环境 app_id: ${sourceAppId}"

# 从目标环境数据库查询 scopeId 对应的 app_id
echo "查询 scopeId=${sourceScopeId} 在目标环境中对应的 app_id..."
queryTargetAppIdByScopeIdSql="select app_id from \`${targetJobManageDb}\`.\`application\` where bk_scope_id='${sourceScopeId}'"
executeSqlInTargetDb "${queryTargetAppIdByScopeIdSql}"
targetAppId=$(echo ${dbResult}|awk -F" " '{print $2}')

if [[ "${targetAppId}" == "" ]];then
  echo ""
  echo "错误：在目标环境中未找到 scopeId=${sourceScopeId} 对应的业务！"
  echo ""
  echo "可能的原因："
  echo "  1. 目标环境尚未从 CMDB 同步该业务"
  echo "  2. 该业务在目标环境中不存在"
  echo ""
  echo "解决方法："
  echo "  1. 确保目标环境已开启从 CMDB 同步业务的功能"
  echo "  2. 手动触发同步或等待自动同步完成"
  echo ""
  exit 1
fi

echo "  目标环境 app_id: ${targetAppId}"

# 检查 source 和 target 的 app_id 是否一致
if [[ "${sourceAppId}" != "${targetAppId}" ]];then
  echo ""
  echo "⚠️  注意：源环境和目标环境的 app_id 不一致！"
  echo "    源环境 app_id: ${sourceAppId}"
  echo "    目标环境 app_id: ${targetAppId}"
  echo "    迁移时将自动替换为目标环境的 app_id"
  echo ""
fi

# 0.参数校验
configError=0

# 验证查询到的 app_id 是否有效
if [[ "${sourceAppId}" == "" ]];then
  echo "在源数据库（临时库）中查询不到指定的业务，请检查配置项：sourceScopeId=${sourceScopeId}"
  configError=1
fi

if [[ "${targetAppId}" == "" ]];then
  echo "在目标数据库中查询不到指定的业务，请检查配置项：sourceScopeId=${sourceScopeId}"
  configError=1
fi

if [[ "${configError}" != "0" ]];then
  exit 1
fi

echo ""
echo "配置校验通过，开始迁移..."
