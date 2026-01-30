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
# 说明：该脚本用于生成作业平台数据迁移的中间数据（无ID偏移量版本）
# 适用场景：新环境数据库为空，可以直接迁移数据而不需要修改ID
# ##########################################

# 加载配置信息
source 0_config_business.sh

echo "========================================="
echo "开始生成迁移数据..."
echo "源业务scopeId: ${sourceScopeId}"
echo "源业务app_id: ${sourceAppId}"
echo "========================================="

### 1.选择业务数据插入临时表
# 临时数据库名称（与 tool_prepare_db.sh 保持一致）
tmpJobManageDb="new_job_manage"
tmpJobCrontabDb="new_job_crontab"
tmpJobFileGatewayDb="new_job_file_gateway"

function pickDataToTmpTableByAppId(){
  # 创建临时表
  sourceDb=$1
  tableName=$2
  tmpDb=$3
  tmpTableName="${tableName}_${sourceAppId}"
  
  # 先删除临时表（如果存在）
  executeSqlInTmpDb "drop table if exists \`${tmpDb}\`.\`${tmpTableName}\`;"
  
  # 创建临时表（从临时数据库复制表结构）
  executeSqlInTmpDb "create table \`${tmpDb}\`.\`${tmpTableName}\` like \`${sourceDb}\`.\`${tableName}\`;"
  
  # 选择数据插入临时表（从临时数据库读取数据）
  executeSqlInTmpDb "insert into \`${tmpDb}\`.\`${tmpTableName}\` select * from \`${sourceDb}\`.\`${tableName}\` where app_id=${sourceAppId};"
}

function pickJobManageDataToTmpTableByAppId(){
  pickDataToTmpTableByAppId "${tmpJobManageDb}" "$1" "${tmpJobManageDb}"
}

function pickJobCrontabDataToTmpTableByAppId(){
  pickDataToTmpTableByAppId "${tmpJobCrontabDb}" "$1" "${tmpJobCrontabDb}"
}

function pickJobFileGatewayDataToTmpTableByAppId(){
  pickDataToTmpTableByAppId "${tmpJobFileGatewayDb}" "$1" "${tmpJobFileGatewayDb}"
}

echo "步骤1: 选择业务数据到临时表..."

#(1)账号
echo ""
echo "  - 迁移账号数据..."
pickJobManageDataToTmpTableByAppId "account"
echo "    ✓ 完成"

#(2)脚本（排除公共脚本，公共脚本在全局迁移中处理）
echo ""
echo "  - 迁移脚本数据..."
dbName="${tmpJobManageDb}"
tableName="script"
tmpTableName="${tableName}_${sourceAppId}"
executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where app_id=${sourceAppId} and (is_public=0 or is_public is null);"

# 表：script_version
dbName="${tmpJobManageDb}"
tableName="script_version"
tmpTableName="${tableName}_${sourceAppId}"
executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
# 选择数据插入临时表（排除公共脚本的版本）
executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where script_id in (select id from \`${tmpJobManageDb}\`.\`script\` where app_id=${sourceAppId} and (is_public=0 or is_public is null));"
echo "    ✓ 完成"

#(3)作业模板
echo ""
echo "  - 迁移作业模板数据..."
pickJobManageDataToTmpTableByAppId "task_template"

# 作业模板子元素表
function pickTemplateChildDataToTmpTableByAppId(){
  dbName="${tmpJobManageDb}"
  tableName="$1"
  tmpTableName="${tableName}_${sourceAppId}"
  executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
  executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
  # 选择数据插入临时表
  executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where template_id in (select id from \`${tmpJobManageDb}\`.\`task_template\` where app_id=${sourceAppId});"
}

# 表：task_template_step
pickTemplateChildDataToTmpTableByAppId "task_template_step"

# 作业模板步骤详情表
function pickTemplateStepDetailDataToTmpTableByAppId(){
  dbName="${tmpJobManageDb}"
  tableName="$1"
  tmpTableName="${tableName}_${sourceAppId}"
  executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
  executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
  # 选择数据插入临时表
  executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where step_id in (select id from \`${tmpJobManageDb}\`.\`task_template_step\` where template_id in (select id from \`${tmpJobManageDb}\`.\`task_template\` where app_id=${sourceAppId}));"
}

# 表：task_template_step_approval
pickTemplateStepDetailDataToTmpTableByAppId "task_template_step_approval"

# 表：task_template_step_file
pickTemplateStepDetailDataToTmpTableByAppId "task_template_step_file"

# 表：task_template_step_file_list
pickTemplateStepDetailDataToTmpTableByAppId "task_template_step_file_list"

# 表：task_template_step_script
pickTemplateStepDetailDataToTmpTableByAppId "task_template_step_script"

# 表：task_template_variable
pickTemplateChildDataToTmpTableByAppId "task_template_variable"
echo "    ✓ 完成"

#(4)执行方案
echo ""
echo "  - 迁移执行方案数据..."
pickJobManageDataToTmpTableByAppId "task_plan"

# 执行方案子元素表
function pickPlanChildDataToTmpTableByAppId(){
  dbName="${tmpJobManageDb}"
  tableName="$1"
  tmpTableName="${tableName}_${sourceAppId}"
  executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
  executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
  # 选择数据插入临时表
  executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where plan_id in (select id from \`${tmpJobManageDb}\`.\`task_plan\` where app_id=${sourceAppId});"
}

# 表：task_plan_step
pickPlanChildDataToTmpTableByAppId "task_plan_step"

# 执行方案步骤详情表
function pickPlanStepDetailDataToTmpTableByAppId(){
  dbName="${tmpJobManageDb}"
  tableName="$1"
  tmpTableName="${tableName}_${sourceAppId}"
  executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
  executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
  # 选择数据插入临时表
  executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where step_id in (select id from \`${tmpJobManageDb}\`.\`task_plan_step\` where plan_id in (select id from \`${tmpJobManageDb}\`.\`task_plan\` where app_id=${sourceAppId}));"
}

# 表：task_plan_step_approval
pickPlanStepDetailDataToTmpTableByAppId "task_plan_step_approval"

# 表：task_plan_step_file
pickPlanStepDetailDataToTmpTableByAppId "task_plan_step_file"

# 表：task_plan_step_file_list
pickPlanStepDetailDataToTmpTableByAppId "task_plan_step_file_list"

# 表：task_plan_step_script
pickPlanStepDetailDataToTmpTableByAppId "task_plan_step_script"

# 表：task_plan_variable
pickPlanChildDataToTmpTableByAppId "task_plan_variable"
echo "    ✓ 完成"

#(5)凭证
echo ""
echo "  - 迁移凭证数据..."
pickJobManageDataToTmpTableByAppId "credential"
echo "    ✓ 完成"

#(6)通知策略（业务相关部分，app_id!=-1）
echo ""
echo "  - 迁移通知策略数据..."
dbName="${tmpJobManageDb}"
tableName="notify_trigger_policy"
tmpTableName="${tableName}_${sourceAppId}"
executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where app_id=${sourceAppId};"

# 表：notify_policy_role_target（通过 policy_id 关联）
tableName="notify_policy_role_target"
tmpTableName="${tableName}_${sourceAppId}"
executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where policy_id in (select id from \`${tmpJobManageDb}\`.\`notify_trigger_policy\` where app_id=${sourceAppId});"

# 表：notify_role_target_channel（通过 role_target_id 关联）
tableName="notify_role_target_channel"
tmpTableName="${tableName}_${sourceAppId}"
executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where role_target_id in (select id from \`${tmpJobManageDb}\`.\`notify_policy_role_target\` where policy_id in (select id from \`${tmpJobManageDb}\`.\`notify_trigger_policy\` where app_id=${sourceAppId}));"
echo "    ✓ 完成"

#(7)收藏
echo ""
echo "  - 迁移收藏数据..."
pickJobManageDataToTmpTableByAppId "task_favorite_plan"
pickJobManageDataToTmpTableByAppId "task_favorite_template"
echo "    ✓ 完成"

#(8)标签
echo ""
echo "  - 迁移标签数据..."
pickJobManageDataToTmpTableByAppId "tag"

# 表：resource_tag（按业务资源关联）
# resource_tag 表没有 app_id 字段，需要通过 resource_id 和 resource_type 关联
# 这里采用简化策略：迁移与已迁移的脚本和模板关联的标签
dbName="${tmpJobManageDb}"
tableName="resource_tag"
tmpTableName="${tableName}_${sourceAppId}"
executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobManageDb}\`.\`${tableName}\`;"
# 迁移脚本相关的 resource_tag（resource_type=1 假设为脚本）
executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobManageDb}\`.\`${tableName}\` where tag_id in (select id from \`${tmpJobManageDb}\`.\`tag\` where app_id=${sourceAppId});"
echo "    ✓ 完成"

#(9)定时任务
echo ""
echo "  - 迁移定时任务数据..."
pickJobCrontabDataToTmpTableByAppId "cron_job"
echo "    ✓ 完成"

#(10)文件源（级联迁移 file_source 和 file_source_share）
echo ""
echo "  - 迁移文件源数据..."
# 先迁移 file_source 表
pickJobFileGatewayDataToTmpTableByAppId "file_source"

# 级联迁移 file_source_share 表
# 只迁移属于该业务的文件源的共享关系（file_source_share.file_source_id in 该业务的file_source.id）
dbName="${tmpJobFileGatewayDb}"
tableName="file_source_share"
tmpTableName="${tableName}_${sourceAppId}"
tmpFileSourceTableName="file_source_${sourceAppId}"
executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "create table \`${dbName}\`.\`${tmpTableName}\` like \`${tmpJobFileGatewayDb}\`.\`${tableName}\`;"
# 级联迁移：只迁移临时表中已存在的 file_source_id 对应的共享关系
executeSqlInTmpDb "insert into \`${dbName}\`.\`${tmpTableName}\` select * from \`${tmpJobFileGatewayDb}\`.\`${tableName}\` where file_source_id in (select id from \`${dbName}\`.\`${tmpFileSourceTableName}\`);"
echo "    ✓ 完成"

echo ""
echo "步骤1完成: 业务数据已选择到临时表"

### 2.数据处理（可选）
echo ""
echo "步骤2: 数据处理..."

# 注意：由于新环境DB是空的，不需要进行ID偏移量转换
# 保留标签数据，不再清空 tags 字段

dbName="${tmpJobManageDb}"

# 禁用定时任务（安全起见，迁移后的定时任务默认禁用）
dbName="${tmpJobCrontabDb}"
tableName="cron_job"
tmpTableName="${tableName}_${sourceAppId}"
executeSqlInTmpDb "use ${dbName};update ${tmpTableName} set is_enable=0;"

### 2.1 补充 tenant_id 字段（针对需要迁移且在 V3.12.0 添加了 tenant_id 的表）
echo ""
echo "步骤2.1: 补充 tenant_id 字段..."

# script 表补充 tenant_id（业务脚本部分）
tableName="script"
tmpTableName="${tableName}_${sourceAppId}"
dbName="${tmpJobManageDb}"
# 检查是否有 tenant_id 列，如果没有则添加
hasColumn=$(executeSqlInTmpDb "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='${dbName}' AND table_name='${tmpTableName}' AND column_name='tenant_id';" 2>/dev/null | tail -1)
if [[ "${hasColumn}" == "0" || "${hasColumn}" == "" ]]; then
  echo "  - 为 ${tableName} 添加 tenant_id 列..."
  # 与 support-files/sql 保持一致的 ALTER TABLE 语句
  executeSqlInTmpDb "ALTER TABLE \`${dbName}\`.\`${tmpTableName}\` ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT '${defaultTenantId}';"
fi
# 无论列是否已存在，都更新 tenant_id 值（幂等操作）
echo "  - 为 ${tableName} 设置 tenant_id 值..."
executeSqlInTmpDb "UPDATE \`${dbName}\`.\`${tmpTableName}\` SET tenant_id='${defaultTenantId}';"

# file_source 表补充 tenant_id
tableName="file_source"
tmpTableName="${tableName}_${sourceAppId}"
dbName="${tmpJobFileGatewayDb}"
hasColumn=$(executeSqlInTmpDb "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='${dbName}' AND table_name='${tmpTableName}' AND column_name='tenant_id';" 2>/dev/null | tail -1)
if [[ "${hasColumn}" == "0" || "${hasColumn}" == "" ]]; then
  echo "  - 为 ${tableName} 添加 tenant_id 列..."
  # 与 support-files/sql 保持一致的 ALTER TABLE 语句（指定 AFTER id）
  executeSqlInTmpDb "ALTER TABLE \`${dbName}\`.\`${tmpTableName}\` ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT '${defaultTenantId}' AFTER \`id\`;"
fi
# 无论列是否已存在，都更新 tenant_id 值（幂等操作）
echo "  - 为 ${tableName} 设置 tenant_id 值..."
executeSqlInTmpDb "UPDATE \`${dbName}\`.\`${tmpTableName}\` SET tenant_id='${defaultTenantId}';"

### 2.2 替换 app_id（将 source 环境的 app_id 替换为 target 环境的 app_id）
echo ""
echo "步骤2.2: 替换 app_id 字段..."

# 检查是否需要替换 app_id
if [[ "${sourceAppId}" != "${targetAppId}" ]]; then
  echo "  源环境 app_id: ${sourceAppId} -> 目标环境 app_id: ${targetAppId}"
  echo ""
  
  # job_manage 库中包含 app_id 字段的表
  echo "  替换 job_manage 库中的 app_id..."
  jobManageTablesWithAppId=(account script task_template task_plan credential notify_trigger_policy task_favorite_plan task_favorite_template tag)
  for tableName in ${jobManageTablesWithAppId[@]}
  do
    tmpTableName="${tableName}_${sourceAppId}"
    echo "    - 替换 ${tableName} 表的 app_id..."
    executeSqlInTmpDb "UPDATE \`${tmpJobManageDb}\`.\`${tmpTableName}\` SET app_id=${targetAppId} WHERE app_id=${sourceAppId};"
  done
  
  # job_crontab 库中包含 app_id 字段的表
  echo "  替换 job_crontab 库中的 app_id..."
  jobCrontabTablesWithAppId=(cron_job)
  for tableName in ${jobCrontabTablesWithAppId[@]}
  do
    tmpTableName="${tableName}_${sourceAppId}"
    echo "    - 替换 ${tableName} 表的 app_id..."
    executeSqlInTmpDb "UPDATE \`${tmpJobCrontabDb}\`.\`${tmpTableName}\` SET app_id=${targetAppId} WHERE app_id=${sourceAppId};"
  done
  
  # job_file_gateway 库中包含 app_id 字段的表
  echo "  替换 job_file_gateway 库中的 app_id..."
  jobFileGatewayTablesWithAppId=(file_source)
  for tableName in ${jobFileGatewayTablesWithAppId[@]}
  do
    tmpTableName="${tableName}_${sourceAppId}"
    echo "    - 替换 ${tableName} 表的 app_id..."
    executeSqlInTmpDb "UPDATE \`${tmpJobFileGatewayDb}\`.\`${tmpTableName}\` SET app_id=${targetAppId} WHERE app_id=${sourceAppId};"
  done
  
  echo "  ✓ app_id 替换完成"
else
  echo "  源环境和目标环境的 app_id 相同 (${sourceAppId})，无需替换"
fi

### 2.3 处理 file_source_share 表的 app_id（跨业务共享关系）
# file_source_share.app_id 表示"可以使用该文件源的业务ID"，可能是其他业务的 app_id
# 需要根据 sourceDB 的 app_id 查找对应的 bk_scope_id，再根据 bk_scope_id 查找 targetDB 的 app_id
echo ""
echo "步骤2.3: 处理 file_source_share 表的 app_id（跨业务共享关系）..."

dbName="${tmpJobFileGatewayDb}"
tableName="file_source_share"
tmpTableName="${tableName}_${sourceAppId}"

# 1. 获取 file_source_share 表中所有不同的 app_id
echo "  - 获取 file_source_share 表中的所有 app_id..."
distinctAppIdsSql="SELECT DISTINCT app_id FROM \`${dbName}\`.\`${tmpTableName}\`"
executeSqlInTmpDb "${distinctAppIdsSql}"
# 解析结果（跳过表头）
distinctAppIds=$(echo "${dbResult}" | tail -n +2)

if [[ -z "${distinctAppIds}" ]]; then
  echo "    file_source_share 表为空，无需处理"
else
  echo "    找到的 app_id 列表: ${distinctAppIds}"
  
  # 2. 构建 app_id 映射缓存（sourceAppId -> targetAppId）
  echo "  - 构建 app_id 映射缓存..."
  
  # 创建临时映射表
  mappingTableName="app_id_mapping_${sourceAppId}"
  executeSqlInTmpDb "DROP TABLE IF EXISTS \`${tmpMigrationDb}\`.\`${mappingTableName}\`;"
  executeSqlInTmpDb "CREATE TABLE \`${tmpMigrationDb}\`.\`${mappingTableName}\` (source_app_id BIGINT PRIMARY KEY, target_app_id BIGINT, bk_scope_id VARCHAR(64));"
  
  # 遍历每个 app_id，查询 bk_scope_id 和 target_app_id
  for srcAppId in ${distinctAppIds}
  do
    # 2a. 从 sourceDB（老环境）中查询 bk_scope_id
    queryBkScopeIdSql="SELECT bk_scope_id FROM \`${sourceJobManageDb}\`.\`application\` WHERE app_id=${srcAppId}"
    executeSqlInSourceDb "${queryBkScopeIdSql}"
    bkScopeId=$(echo "${dbResult}" | tail -n +2 | head -1)
    
    if [[ -z "${bkScopeId}" ]]; then
      echo "    ⚠️  app_id=${srcAppId} 在源环境中找不到对应的 bk_scope_id，将删除相关记录"
      # 插入映射记录，target_app_id 为 NULL 表示需要删除
      executeSqlInTmpDb "INSERT INTO \`${tmpMigrationDb}\`.\`${mappingTableName}\` (source_app_id, target_app_id, bk_scope_id) VALUES (${srcAppId}, NULL, NULL);"
      continue
    fi
    
    # 2b. 从 targetDB 中查询 target_app_id
    queryTargetAppIdSql="SELECT app_id FROM \`${targetJobManageDb}\`.\`application\` WHERE bk_scope_id='${bkScopeId}'"
    executeSqlInTargetDb "${queryTargetAppIdSql}"
    tgtAppId=$(echo "${dbResult}" | tail -n +2 | head -1)
    
    if [[ -z "${tgtAppId}" ]]; then
      echo "    ⚠️  bk_scope_id=${bkScopeId} 在目标环境中找不到对应的 app_id，将删除相关记录"
      executeSqlInTmpDb "INSERT INTO \`${tmpMigrationDb}\`.\`${mappingTableName}\` (source_app_id, target_app_id, bk_scope_id) VALUES (${srcAppId}, NULL, '${bkScopeId}');"
    else
      echo "    ✓ 映射: source_app_id=${srcAppId} -> bk_scope_id=${bkScopeId} -> target_app_id=${tgtAppId}"
      executeSqlInTmpDb "INSERT INTO \`${tmpMigrationDb}\`.\`${mappingTableName}\` (source_app_id, target_app_id, bk_scope_id) VALUES (${srcAppId}, ${tgtAppId}, '${bkScopeId}');"
    fi
  done
  
  # 3. 删除在目标环境中找不到对应 app_id 的记录
  echo "  - 删除在目标环境中不存在的业务共享记录..."
  deleteSql="DELETE FROM \`${dbName}\`.\`${tmpTableName}\` WHERE app_id IN (SELECT source_app_id FROM \`${tmpMigrationDb}\`.\`${mappingTableName}\` WHERE target_app_id IS NULL)"
  executeSqlInTmpDb "${deleteSql}"
  
  # 4. 更新 file_source_share 表中的 app_id
  echo "  - 更新 file_source_share 表中的 app_id..."
  updateSql="UPDATE \`${dbName}\`.\`${tmpTableName}\` t INNER JOIN \`${tmpMigrationDb}\`.\`${mappingTableName}\` m ON t.app_id = m.source_app_id SET t.app_id = m.target_app_id WHERE m.target_app_id IS NOT NULL"
  executeSqlInTmpDb "${updateSql}"
  
  # 5. 清理临时映射表
  executeSqlInTmpDb "DROP TABLE IF EXISTS \`${tmpMigrationDb}\`.\`${mappingTableName}\`;"
  
  echo "  ✓ file_source_share 表 app_id 处理完成"
fi

echo ""
echo "步骤2完成: 数据处理完成"

### 3.导出为SQL文件
echo ""
echo "步骤3: 导出SQL文件..."

function dumpTableInTmpDb(){
  _dbName="$1"
  _tableName="$2"
  _filePath="$3"
  echo "  导出: ${_dbName}.${_tableName} -> ${_filePath}"
  mysqldump -h${tmpMysqlHost} -P${tmpMysqlPort} -u${tmpMysqlRootUser} -p${tmpMysqlRootPassword} --default-character-set=utf8mb4 --skip-opt --complete-insert -t --compact ${_dbName} ${_tableName} > ${_filePath}
}

function dumpTableInTmpJobManage(){
  _tableName="$1"
  _filePath="$2"
  dumpTableInTmpDb "${tmpJobManageDb}" ${_tableName} ${_filePath}
}

function dumpTableInTmpJobCrontab(){
  _tableName="$1"
  _filePath="$2"
  dumpTableInTmpDb "${tmpJobCrontabDb}" ${_tableName} ${_filePath}
}

function dumpTableInTmpJobFileGateway(){
  _tableName="$1"
  _filePath="$2"
  dumpTableInTmpDb "${tmpJobFileGatewayDb}" ${_tableName} ${_filePath}
}

# 创建目标目录
if [[ ! -d "sql/${sourceScopeId}/job_manage" ]];then
  mkdir -p sql/${sourceScopeId}/job_manage
fi
if [[ ! -d "sql/${sourceScopeId}/job_crontab" ]];then
  mkdir -p sql/${sourceScopeId}/job_crontab
fi
if [[ ! -d "sql/${sourceScopeId}/job_file_gateway" ]];then
  mkdir -p sql/${sourceScopeId}/job_file_gateway
fi

# 清理目标目录中的文件
rm -f sql/${sourceScopeId}/job_manage/*
rm -f sql/${sourceScopeId}/job_crontab/*
rm -f sql/${sourceScopeId}/job_file_gateway/*

# 导出job_manage中的表
for tableName in ${jobManageTables[@]}
do
  tmpTableName="${tableName}_${sourceAppId}"
  dumpTableInTmpJobManage ${tmpTableName} "sql/${sourceScopeId}/job_manage/${tmpTableName}.sql"
  # 替换表名（去掉临时表后缀）
  sed -i "s/\`${tmpTableName}\`/\`${tableName}\`/g" "sql/${sourceScopeId}/job_manage/${tmpTableName}.sql"
done

# 导出job_crontab中的表
for tableName in ${jobCrontabTables[@]}
do
  tmpTableName="${tableName}_${sourceAppId}"
  dumpTableInTmpJobCrontab ${tmpTableName} "sql/${sourceScopeId}/job_crontab/${tmpTableName}.sql"
  # 替换表名（去掉临时表后缀）
  sed -i "s/\`${tmpTableName}\`/\`${tableName}\`/g" "sql/${sourceScopeId}/job_crontab/${tmpTableName}.sql"
done

# 导出job_file_gateway中的表
for tableName in ${jobFileGatewayTables[@]}
do
  tmpTableName="${tableName}_${sourceAppId}"
  dumpTableInTmpJobFileGateway ${tmpTableName} "sql/${sourceScopeId}/job_file_gateway/${tmpTableName}.sql"
  # 替换表名（去掉临时表后缀）
  sed -i "s/\`${tmpTableName}\`/\`${tableName}\`/g" "sql/${sourceScopeId}/job_file_gateway/${tmpTableName}.sql"
done

echo ""
echo "步骤3完成: SQL文件已导出到 sql/${sourceScopeId}/"

echo "========================================="
echo "迁移数据生成完成！"
echo "SQL文件位置: sql/${sourceScopeId}/"
echo "========================================="
