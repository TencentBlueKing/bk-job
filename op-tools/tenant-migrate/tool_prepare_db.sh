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
# 说明：准备迁移所需的临时数据库
# 功能：
# 1. 创建临时数据库
# 2. 从 SQL 文件导入表结构和数据到临时数据库
# 3. 确保临时数据库可用于迁移
# 
# 前置条件：
# - 必须先执行 tool_dump_source_db.sh 生成 SQL 文件
# ##########################################

source 0_config_common.sh

# 临时数据库前缀
prefix="new_"

# SQL 文件保存目录
sourceDumpDir="source_dump"

echo "========================================="
echo "准备迁移临时数据库..."
echo "临时数据库: ${tmpMysqlHost}:${tmpMysqlPort}"
echo "SQL 文件目录: ${sourceDumpDir}/"
echo "========================================="

# 检查 SQL 文件是否存在
echo ""
echo "检查 SQL 文件..."
missingFiles=0

if [[ ! -f "${sourceDumpDir}/${sourceJobManageDb}.sql" ]]; then
  echo "  ✗ 缺少文件: ${sourceDumpDir}/${sourceJobManageDb}.sql"
  missingFiles=1
fi

if [[ ! -f "${sourceDumpDir}/${sourceJobCrontabDb}.sql" ]]; then
  echo "  ✗ 缺少文件: ${sourceDumpDir}/${sourceJobCrontabDb}.sql"
  missingFiles=1
fi

if [[ ! -f "${sourceDumpDir}/${sourceJobFileGatewayDb}.sql" ]]; then
  echo "  ✗ 缺少文件: ${sourceDumpDir}/${sourceJobFileGatewayDb}.sql"
  missingFiles=1
fi

if [[ ${missingFiles} -eq 1 ]]; then
  echo ""
  echo "✗ 缺少必要的 SQL 文件！"
  echo ""
  echo "请先执行以下命令生成 SQL 文件："
  echo "  bash tool_dump_source_db.sh"
  echo ""
  exit 1
fi

echo "  ✓ 所有 SQL 文件已就绪"

echo ""
echo "步骤1: 创建临时数据库..."

# 创建临时数据库（如果不存在）
echo "  - 创建临时数据库: ${tmpMigrationDb}"
executeSqlInTmpDb "create database if not exists \`${tmpMigrationDb}\` default character set utf8mb4;"

# 创建临时job_manage数据库（业务迁移用）
echo "  - 创建临时数据库: ${prefix}job_manage"
executeSqlInTmpDb "create database if not exists \`${prefix}job_manage\` default character set utf8mb4;"

# 创建临时job_crontab数据库（业务迁移用）
echo "  - 创建临时数据库: ${prefix}job_crontab"
executeSqlInTmpDb "create database if not exists \`${prefix}job_crontab\` default character set utf8mb4;"

# 创建临时job_file_gateway数据库（业务迁移用）
echo "  - 创建临时数据库: ${prefix}job_file_gateway"
executeSqlInTmpDb "create database if not exists \`${prefix}job_file_gateway\` default character set utf8mb4;"

# 创建全局迁移临时数据库
echo "  - 创建临时数据库: global_job_manage"
executeSqlInTmpDb "create database if not exists \`global_job_manage\` default character set utf8mb4;"

echo ""
echo "步骤2: 从 SQL 文件导入表结构和数据到临时数据库..."

# 函数：从 SQL 文件导入到临时数据库
function importSqlFileToTmpDb() {
  sourceDb=$1
  tmpDb=$2
  sqlFile="${sourceDumpDir}/${sourceDb}.sql"
  
  echo ""
  echo "  [${sourceDb}] 开始导入..."
  echo "    - SQL 文件: ${sqlFile}"
  echo "    - 目标库: ${tmpDb}"
  
  # 1. 清空目标数据库（如果已有数据）
  echo "    - 正在清空目标数据库..."
  executeSqlInTmpDb "DROP DATABASE IF EXISTS \`${tmpDb}\`;"
  executeSqlInTmpDb "CREATE DATABASE \`${tmpDb}\` DEFAULT CHARACTER SET utf8mb4;"
  
  # 2. 准备导入文件（添加 USE 语句）
  tmpFile="${sourceDumpDir}/${sourceDb}_import.sql"
  echo "USE \`${tmpDb}\`;" > ${tmpFile}
  cat ${sqlFile} >> ${tmpFile}
  
  # 3. 导入到临时数据库
  echo "    - 正在导入数据..."
  mysql -h${tmpMysqlHost} -P${tmpMysqlPort} -u${tmpMysqlRootUser} -p${tmpMysqlRootPassword} --default-character-set=utf8mb4 < ${tmpFile}
  
  if [[ $? -ne 0 ]]; then
    echo "    ✗ 导入失败！"
    rm -f ${tmpFile}
    return 1
  fi
  
  # 4. 清理临时文件
  rm -f ${tmpFile}
  
  echo "    ✓ [${sourceDb}] 导入完成"
  
  return 0
}

# 导入 job_manage
importSqlFileToTmpDb "${sourceJobManageDb}" "${prefix}job_manage"
if [[ $? -ne 0 ]]; then
  echo ""
  echo "✗ job_manage 数据库导入失败"
  exit 1
fi

# 导入 job_crontab
importSqlFileToTmpDb "${sourceJobCrontabDb}" "${prefix}job_crontab"
if [[ $? -ne 0 ]]; then
  echo ""
  echo "✗ job_crontab 数据库导入失败"
  exit 1
fi

# 导入 job_file_gateway
importSqlFileToTmpDb "${sourceJobFileGatewayDb}" "${prefix}job_file_gateway"
if [[ $? -ne 0 ]]; then
  echo ""
  echo "✗ job_file_gateway 数据库导入失败"
  exit 1
fi

echo ""
echo "步骤3: 验证临时数据库..."

# 验证临时数据库中的表数量
function verifyTmpDb() {
  tmpDb=$1
  tableCount=$(querySqlInTmpDb "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${tmpDb}';")
  echo "  - ${tmpDb}: ${tableCount} 张表"
}

verifyTmpDb "${prefix}job_manage"
verifyTmpDb "${prefix}job_crontab"
verifyTmpDb "${prefix}job_file_gateway"

echo ""
echo "========================================="
echo "临时数据库准备完成！"
echo "已创建的临时数据库："
echo "  - ${tmpMigrationDb}"
echo "  - ${prefix}job_manage (已从 ${sourceDumpDir}/${sourceJobManageDb}.sql 导入)"
echo "  - ${prefix}job_crontab (已从 ${sourceDumpDir}/${sourceJobCrontabDb}.sql 导入)"
echo "  - ${prefix}job_file_gateway (已从 ${sourceDumpDir}/${sourceJobFileGatewayDb}.sql 导入)"
echo "  - global_job_manage"
echo ""
echo "现在可以开始迁移业务了！"
echo "========================================="
