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
# 说明：清理迁移过程中创建的临时表数据
# ##########################################

# 加载配置信息
source 0_config_business.sh

echo "========================================="
echo "开始清理临时数据..."
echo "业务scopeId: ${sourceScopeId}"
echo "业务app_id: ${sourceAppId}"
echo "========================================="

# 清理job_manage临时表
dbName="${tmpJobManageDb}"
echo ""
echo "清理 ${dbName} 中的临时表..."
for tableName in ${jobManageTables[@]}
do
  tmpTableName="${tableName}_${sourceAppId}"
  echo "  删除表: ${dbName}.${tmpTableName}"
  executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
done
echo "  ✓ 完成"

# 清理job_crontab临时表
dbName="${tmpJobCrontabDb}"
echo ""
echo "清理 ${dbName} 中的临时表..."
for tableName in ${jobCrontabTables[@]}
do
  tmpTableName="${tableName}_${sourceAppId}"
  echo "  删除表: ${dbName}.${tmpTableName}"
  executeSqlInTmpDb "drop table if exists \`${dbName}\`.\`${tmpTableName}\`;"
done
echo "  ✓ 完成"

echo ""
echo "=========================================="
echo "临时数据清理完成！"
echo "========================================="
