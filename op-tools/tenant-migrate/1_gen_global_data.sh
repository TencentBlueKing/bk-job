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
# 说明：该脚本用于生成租户内全局性资源的迁移数据
# 适用场景：新环境数据库为空，迁移租户内全局性、业务无关的资源
# 执行时机：新环境搭建后立即执行一次
# ##########################################

# 加载配置信息
source 0_config_common.sh

echo "========================================="
echo "开始生成全局迁移数据..."
echo "默认租户ID: ${defaultTenantId}"
echo "========================================="

# 临时数据库名称（与 tool_prepare_db.sh 保持一致）
tmpJobManageDb="new_job_manage"

# 全局迁移临时数据库
globalTmpJobManageDb="global_job_manage"

# 全局迁移的表列表
globalJobManageTables=(dangerous_rule notify_black_user_info script script_version)

### 1. 创建临时数据库
echo "步骤1: 创建临时数据库..."
executeSqlInTmpDb "CREATE DATABASE IF NOT EXISTS \`${globalTmpJobManageDb}\` DEFAULT CHARACTER SET utf8mb4;"

### 2. 选择全局数据插入临时表
echo "步骤2: 选择全局数据到临时表..."

# (1) 迁移 dangerous_rule 表
echo ""
echo "  - 迁移高危规则数据 (dangerous_rule)..."
tableName="dangerous_rule"
tmpTableName="${tableName}_global"
executeSqlInTmpDb "DROP TABLE IF EXISTS \`${globalTmpJobManageDb}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "CREATE TABLE \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` LIKE \`${tmpJobManageDb}\`.\`${tableName}\`;"
executeSqlInTmpDb "INSERT INTO \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` SELECT * FROM \`${tmpJobManageDb}\`.\`${tableName}\`;"
echo "    ✓ 完成"

# (2) 迁移 notify_black_user_info 表
echo ""
echo "  - 迁移通知黑名单数据 (notify_black_user_info)..."
tableName="notify_black_user_info"
tmpTableName="${tableName}_global"
executeSqlInTmpDb "DROP TABLE IF EXISTS \`${globalTmpJobManageDb}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "CREATE TABLE \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` LIKE \`${tmpJobManageDb}\`.\`${tableName}\`;"
executeSqlInTmpDb "INSERT INTO \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` SELECT * FROM \`${tmpJobManageDb}\`.\`${tableName}\`;"
echo "    ✓ 完成"

# (3) 迁移公共脚本 (is_public=1)
echo ""
echo "  - 迁移公共脚本数据 (script where is_public=1)..."
tableName="script"
tmpTableName="${tableName}_global"
executeSqlInTmpDb "DROP TABLE IF EXISTS \`${globalTmpJobManageDb}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "CREATE TABLE \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` LIKE \`${tmpJobManageDb}\`.\`${tableName}\`;"
executeSqlInTmpDb "INSERT INTO \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` SELECT * FROM \`${tmpJobManageDb}\`.\`${tableName}\` WHERE is_public=1;"
echo "    ✓ 完成"

# (4) 迁移公共脚本版本
echo ""
echo "  - 迁移公共脚本版本数据 (script_version for public scripts)..."
tableName="script_version"
tmpTableName="${tableName}_global"
executeSqlInTmpDb "DROP TABLE IF EXISTS \`${globalTmpJobManageDb}\`.\`${tmpTableName}\`;"
executeSqlInTmpDb "CREATE TABLE \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` LIKE \`${tmpJobManageDb}\`.\`${tableName}\`;"
executeSqlInTmpDb "INSERT INTO \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` SELECT * FROM \`${tmpJobManageDb}\`.\`${tableName}\` WHERE script_id IN (SELECT id FROM \`${tmpJobManageDb}\`.\`script\` WHERE is_public=1);"
echo "    ✓ 完成"

echo ""
echo "步骤2完成: 全局数据已选择到临时表"

### 3. 数据处理 - 补充 tenant_id 字段
echo ""
echo "步骤3: 数据处理（补充 tenant_id 字段）..."

# 检查并添加 tenant_id 字段（如果目标表需要）
# dangerous_rule 表补充 tenant_id
echo ""
tableName="dangerous_rule"
tmpTableName="${tableName}_global"
# 检查是否有 tenant_id 列，如果没有则添加
hasColumn=$(executeSqlInTmpDb "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='${globalTmpJobManageDb}' AND table_name='${tmpTableName}' AND column_name='tenant_id';" 2>/dev/null | tail -1)
if [[ "${hasColumn}" == "0" || "${hasColumn}" == "" ]]; then
  echo "  - 为 ${tableName} 添加 tenant_id 列..."
  # 与 support-files/sql 保持一致的 ALTER TABLE 语句
  executeSqlInTmpDb "ALTER TABLE \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT '${defaultTenantId}';"
fi
# 无论列是否已存在，都更新 tenant_id 值（幂等操作）
echo "  - 为 ${tableName} 设置 tenant_id 值..."
executeSqlInTmpDb "UPDATE \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` SET tenant_id='${defaultTenantId}';"

# notify_black_user_info 表补充 tenant_id
echo ""
tableName="notify_black_user_info"
tmpTableName="${tableName}_global"
hasColumn=$(executeSqlInTmpDb "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='${globalTmpJobManageDb}' AND table_name='${tmpTableName}' AND column_name='tenant_id';" 2>/dev/null | tail -1)
if [[ "${hasColumn}" == "0" || "${hasColumn}" == "" ]]; then
  echo "  - 为 ${tableName} 添加 tenant_id 列..."
  # 与 support-files/sql 保持一致的 ALTER TABLE 语句（指定 AFTER id）
  executeSqlInTmpDb "ALTER TABLE \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` ADD COLUMN tenant_id VARCHAR(32) NOT NULL DEFAULT '${defaultTenantId}' AFTER \`id\`;"
fi
# 无论列是否已存在，都更新 tenant_id 值（幂等操作）
echo "  - 为 ${tableName} 设置 tenant_id 值..."
executeSqlInTmpDb "UPDATE \`${globalTmpJobManageDb}\`.\`${tmpTableName}\` SET tenant_id='${defaultTenantId}';"

echo ""
echo "步骤3完成: 数据处理完成"

### 4. 导出为SQL文件
echo ""
echo "步骤4: 导出SQL文件..."

function dumpTableInTmpDb(){
  _dbName="$1"
  _tableName="$2"
  _filePath="$3"
  echo "  导出: ${_dbName}.${_tableName} -> ${_filePath}"
  mysqldump -h${tmpMysqlHost} -P${tmpMysqlPort} -u${tmpMysqlRootUser} -p${tmpMysqlRootPassword} --default-character-set=utf8mb4 --skip-opt --complete-insert -t --compact ${_dbName} ${_tableName} > ${_filePath}
}

# 创建目标目录
if [[ ! -d "sql/global/job_manage" ]]; then
  mkdir -p sql/global/job_manage
fi

# 清理目标目录中的文件
rm -f sql/global/job_manage/*

# 导出全局表
for tableName in dangerous_rule notify_black_user_info script script_version
do
  tmpTableName="${tableName}_global"
  dumpTableInTmpDb "${globalTmpJobManageDb}" ${tmpTableName} "sql/global/job_manage/${tmpTableName}.sql"
  # 替换表名（去掉临时表后缀）
  sed -i "s/\`${tmpTableName}\`/\`${tableName}\`/g" "sql/global/job_manage/${tmpTableName}.sql"
done

echo ""
echo "步骤4完成: SQL文件已导出到 sql/global/"

### 5. 清理临时数据库
echo ""
echo "步骤5: 清理临时表..."
for tableName in dangerous_rule notify_black_user_info script script_version
do
  tmpTableName="${tableName}_global"
  executeSqlInTmpDb "DROP TABLE IF EXISTS \`${globalTmpJobManageDb}\`.\`${tmpTableName}\`;"
done

echo "========================================="
echo "全局迁移数据生成完成！"
echo "SQL文件位置: sql/global/"
echo "========================================="

