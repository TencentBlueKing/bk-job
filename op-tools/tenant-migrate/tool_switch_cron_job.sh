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
# 说明：批量切换定时任务状态（关闭老环境、开启新环境）
# 用法：bash tool_switch_cron_job.sh <tenantId> <scopeId> --oldUrl <url> --oldAppCode <code> --oldAppSecret <secret> --newUrl <url> --newAppCode <code> --newAppSecret <secret>
# 
# 功能：
# 1. 从老环境数据库读取该业务下 is_enable=1 的定时任务 ID 列表
# 2. 逐个切换：先关闭老环境，再开启新环境
# 3. 如果某个任务切换失败，记录 scopeId 和 cronJobId
# ##########################################

# 加载配置
source 0_config_common.sh

# API 路径（使用 bk-apigateway 的 job v3 接口）
UPDATE_CRON_STATUS_API="/api/v3/system/update_cron_status"

# ========================================
# 参数解析
# ========================================

function show_usage() {
  echo "用法: bash tool_switch_cron_job.sh <tenantId> <scopeId> [选项]"
  echo ""
  echo "示例:"
  echo "  bash tool_switch_cron_job.sh tencent 2 \\"
  echo "    --oldUrl 'http://bkapigw-old.example.com' \\"
  echo "    --oldAppCode 'bk_job' \\"
  echo "    --oldAppSecret 'xxx' \\"
  echo "    --oldEnvOperator 'xxx' \\"
  echo "    --newUrl 'http://bkapigw-new.example.com' \\"
  echo "    --newAppCode 'bk_job' \\"
  echo "    --newAppSecret 'xxx' \\"
  echo "    --newEnvOperator 'xxx'"
  echo ""
  echo "参数说明："
  echo "  tenantId:          新环境的租户ID（老环境无多租户概念）"
  echo "  scopeId:           业务的 bk_scope_id"
  echo "  --oldUrl:          老环境 bk-apigateway 地址"
  echo "  --oldAppCode:      老环境 bk_app_code"
  echo "  --oldAppSecret:    老环境 bk_app_secret"
  echo "  --oldEnvOperator:  老环境操作人（bk_username）"
  echo "  --newUrl:          新环境 bk-apigateway 地址"
  echo "  --newAppCode:      新环境 bk_app_code"
  echo "  --newAppSecret:    新环境 bk_app_secret"
  echo "  --newEnvOperator:  新环境操作人（bk_username）"
}

if [ $# -lt 2 ]; then
  show_usage
  exit 1
fi

TENANT_ID="$1"
SCOPE_ID="$2"
shift 2

# 解析命名参数
while [ $# -gt 0 ]; do
  case "$1" in
    --oldUrl)
      OLD_URL="$2"
      shift 2
      ;;
    --oldAppCode)
      OLD_APP_CODE="$2"
      shift 2
      ;;
    --oldAppSecret)
      OLD_APP_SECRET="$2"
      shift 2
      ;;
    --oldEnvOperator)
      OLD_ENV_OPERATOR="$2"
      shift 2
      ;;
    --newUrl)
      NEW_URL="$2"
      shift 2
      ;;
    --newAppCode)
      NEW_APP_CODE="$2"
      shift 2
      ;;
    --newAppSecret)
      NEW_APP_SECRET="$2"
      shift 2
      ;;
    --newEnvOperator)
      NEW_ENV_OPERATOR="$2"
      shift 2
      ;;
    *)
      echo "错误：未知参数 $1"
      show_usage
      exit 1
      ;;
  esac
done

# ========================================
# 检查必要参数
# ========================================

MISSING_PARAMS=""
[ -z "${OLD_URL}" ] && MISSING_PARAMS="${MISSING_PARAMS} --oldUrl"
[ -z "${OLD_APP_CODE}" ] && MISSING_PARAMS="${MISSING_PARAMS} --oldAppCode"
[ -z "${OLD_APP_SECRET}" ] && MISSING_PARAMS="${MISSING_PARAMS} --oldAppSecret"
[ -z "${OLD_ENV_OPERATOR}" ] && MISSING_PARAMS="${MISSING_PARAMS} --oldEnvOperator"
[ -z "${NEW_URL}" ] && MISSING_PARAMS="${MISSING_PARAMS} --newUrl"
[ -z "${NEW_APP_CODE}" ] && MISSING_PARAMS="${MISSING_PARAMS} --newAppCode"
[ -z "${NEW_APP_SECRET}" ] && MISSING_PARAMS="${MISSING_PARAMS} --newAppSecret"
[ -z "${NEW_ENV_OPERATOR}" ] && MISSING_PARAMS="${MISSING_PARAMS} --newEnvOperator"

if [ -n "${MISSING_PARAMS}" ]; then
  echo "错误：缺少必要参数:${MISSING_PARAMS}"
  echo ""
  show_usage
  exit 1
fi

echo "========================================="
echo "  定时任务切换工具"
echo "========================================="
echo ""
echo "租户ID: ${TENANT_ID}"
echo "业务 scopeId: ${SCOPE_ID}"
echo "老环境: ${OLD_URL}"
echo "新环境: ${NEW_URL}"
echo ""

# ========================================
# 步骤1：获取老环境的 app_id
# ========================================

echo "步骤1: 获取业务 app_id..."

# 从老环境数据库查询 app_id
querySourceAppIdSql="SELECT app_id FROM \`${sourceJobManageDb}\`.\`application\` WHERE bk_scope_id='${SCOPE_ID}'"
executeSqlInSourceDb "${querySourceAppIdSql}"
SOURCE_APP_ID=$(echo "${dbResult}" | tail -n +2 | head -1)

if [ -z "${SOURCE_APP_ID}" ]; then
  echo "错误：在老环境中未找到 scopeId=${SCOPE_ID} 对应的业务"
  exit 1
fi
echo "  老环境 app_id: ${SOURCE_APP_ID}"

# 从新环境数据库查询 app_id
queryTargetAppIdSql="SELECT app_id FROM \`${targetJobManageDb}\`.\`application\` WHERE bk_scope_id='${SCOPE_ID}'"
executeSqlInTargetDb "${queryTargetAppIdSql}"
TARGET_APP_ID=$(echo "${dbResult}" | tail -n +2 | head -1)

if [ -z "${TARGET_APP_ID}" ]; then
  echo "错误：在新环境中未找到 scopeId=${SCOPE_ID} 对应的业务"
  exit 1
fi
echo "  新环境 app_id: ${TARGET_APP_ID}"
echo ""

# ========================================
# 步骤2：获取老环境中开启的定时任务列表
# ========================================

echo "步骤2: 获取老环境中开启的定时任务..."

# 查询老环境中 is_enable=1 的定时任务
queryEnabledCronJobsSql="SELECT id, name FROM \`${sourceJobCrontabDb}\`.\`cron_job\` WHERE app_id=${SOURCE_APP_ID} AND is_enable=1"
executeSqlInSourceDb "${queryEnabledCronJobsSql}"

# 解析结果
CRON_JOB_DATA=$(echo "${dbResult}" | tail -n +2)
CRON_JOB_COUNT=$(echo "${CRON_JOB_DATA}" | grep -c "^" 2>/dev/null || echo "0")

if [ -z "${CRON_JOB_DATA}" ] || [ "${CRON_JOB_COUNT}" -eq 0 ]; then
  echo "  未找到开启的定时任务，无需切换"
  exit 0
fi

echo "  找到 ${CRON_JOB_COUNT} 个开启的定时任务："
echo ""
echo "  ID         名称"
echo "  --------   ----------------------------------------"
echo "${CRON_JOB_DATA}" | while read line; do
  id=$(echo "$line" | awk '{print $1}')
  name=$(echo "$line" | cut -d' ' -f2-)
  printf "  %-10s %s\n" "$id" "$name"
done
echo ""

# 提取 ID 列表
CRON_JOB_IDS=$(echo "${CRON_JOB_DATA}" | awk '{print $1}')

# ========================================
# 步骤3：逐个切换定时任务（关一个、开一个）
# ========================================

echo "步骤3: 逐个切换定时任务..."
echo ""

# 调用老环境 API 关闭定时任务的函数
function disable_cron_job_in_source() {
  local cron_job_id=$1
  local api_url="${OLD_URL}${UPDATE_CRON_STATUS_API}"
  
  local response=$(curl -s -X POST "${api_url}" \
    -H "Content-Type: application/json" \
    -H "X-Bkapi-Authorization: {\"bk_app_code\": \"${OLD_APP_CODE}\", \"bk_app_secret\": \"${OLD_APP_SECRET}\", \"bk_username\": \"${OLD_ENV_OPERATOR}\"}" \
    -d '{
      "bk_biz_id": '"${SCOPE_ID}"',
      "id": '"${cron_job_id}"',
      "status": 2
    }')
  
  local result=$(echo "$response" | grep -o '"result":[^,]*' | cut -d':' -f2)
  if [ "$result" = "true" ]; then
    return 0
  else
    echo "请求失败，response: ${response:-未知错误}"
    return 1
  fi
}

# 调用新环境 API 开启定时任务的函数（新环境需要 X-Bk-Tenant-Id）
function enable_cron_job_in_target() {
  local cron_job_id=$1
  local api_url="${NEW_URL}${UPDATE_CRON_STATUS_API}"
  
  local response=$(curl -s -X POST "${api_url}" \
    -H "Content-Type: application/json" \
    -H "X-Bkapi-Authorization: {\"bk_app_code\": \"${NEW_APP_CODE}\", \"bk_app_secret\": \"${NEW_APP_SECRET}\", \"bk_username\": \"${NEW_ENV_OPERATOR}\"}" \
    -H "X-Bk-Tenant-Id: ${TENANT_ID}" \
    -d '{
      "bk_biz_id": '"${SCOPE_ID}"',
      "id": '"${cron_job_id}"',
      "status": 1
    }')
  
  local result=$(echo "$response" | grep -o '"result":[^,]*' | cut -d':' -f2)
  if [ "$result" = "true" ]; then
    return 0
  else
    echo "请求失败，response: ${response:-未知错误}"
    return 1
  fi
}

SUCCESS_COUNT=0
FAIL_COUNT=0
FAILED_CRON_JOBS=()

currentIndex=0
for cron_job_id in ${CRON_JOB_IDS}; do
  currentIndex=$((currentIndex + 1))
  echo "  [${currentIndex}/${CRON_JOB_COUNT}] 切换定时任务 ID=${cron_job_id}..."
  
  # 先关闭老环境
  disable_error=$(disable_cron_job_in_source "${cron_job_id}")
  if [ $? -ne 0 ]; then
    echo "    ✗ 老环境关闭失败: scopeId=${SCOPE_ID}, cronJobId=${cron_job_id}, 原因: ${disable_error}"
    FAIL_COUNT=$((FAIL_COUNT + 1))
    FAILED_CRON_JOBS+=("${cron_job_id}")
    continue
  fi
  echo "    ✓ 老环境关闭成功"
  
  # 再开启新环境
  enable_error=$(enable_cron_job_in_target "${cron_job_id}")
  if [ $? -ne 0 ]; then
    echo "    ✗ 新环境开启失败: scopeId=${SCOPE_ID}, cronJobId=${cron_job_id}, 原因: ${enable_error}"
    FAIL_COUNT=$((FAIL_COUNT + 1))
    FAILED_CRON_JOBS+=("${cron_job_id}")
    continue
  fi
  echo "    ✓ 新环境开启成功"
  
  SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
done

echo ""

# ========================================
# 输出报告
# ========================================

echo "========================================="
echo "          定时任务切换报告"
echo "========================================="
echo ""
echo "租户ID: ${TENANT_ID}"
echo "业务 scopeId: ${SCOPE_ID}"
echo "定时任务总数: ${CRON_JOB_COUNT}"
echo ""
echo "切换结果:"
echo "  成功: ${SUCCESS_COUNT}"
echo "  失败: ${FAIL_COUNT}"
echo ""

if [ ${FAIL_COUNT} -gt 0 ]; then
  echo "失败的定时任务列表（scopeId=${SCOPE_ID}）："
  for failedId in "${FAILED_CRON_JOBS[@]}"; do
    echo "  - cronJobId: ${failedId}"
  done
  echo ""
  echo "⚠️  部分操作失败，请检查日志并手动处理"
  exit 1
else
  echo "✓ 所有定时任务切换成功！"
fi
