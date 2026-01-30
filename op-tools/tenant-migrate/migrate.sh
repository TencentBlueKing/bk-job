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
# 说明：新迁移工具主脚本
# 适用场景：新环境数据库为空，直接迁移数据无需ID偏移
# 使用方法：./migrate.sh <scopeId>
# 示例：./migrate.sh 2
# ##########################################

shellExecutor="/bin/bash"

# 判断是否有其他业务正在迁移
if [[ -f "running.migration.lock" ]];then
  echo "有其他业务正在迁移："$(cat running.migration.lock)
  exit 1
fi

# 记录起止时间与耗时
startTime=`date "+%Y-%m-%d %H:%M:%S"`
startTime_s=`date "+%s"`

sourceScopeId="$1"
sourceScopeId=${sourceScopeId// /}

if [[ "${sourceScopeId}" == "" ]];then
  echo "错误: 请提供业务scopeId参数"
  echo "使用方法: ./migrate.sh <scopeId>"
  echo "示例: ./migrate.sh 2"
  exit 1
fi

echo "========================================="
echo "开始迁移业务: scopeId=${sourceScopeId}"
echo "开始时间: ${startTime}"
echo "========================================="

# 创建锁文件
echo "业务scopeId:${sourceScopeId}" > running.migration.lock

# 1.生成中间SQL
echo ""
echo ">>> 步骤1: 生成迁移数据"
${shellExecutor} 1_gen_migration_data.sh ${sourceScopeId}
if [[ $? -ne 0 ]];then
  echo "错误: 生成迁移数据失败"
  rm -f running.migration.lock
  exit 1
fi

# 2.应用中间SQL到目标环境
echo ""
echo ">>> 步骤2: 应用迁移数据到目标环境"
${shellExecutor} 2_apply_migration_data.sh ${sourceScopeId}
if [[ $? -ne 0 ]];then
  echo "错误: 应用迁移数据失败"
  rm -f running.migration.lock
  exit 1
fi

# 3.清理临时表数据
echo ""
echo ">>> 步骤3: 清理临时数据"
${shellExecutor} tool_clear_tmp_data.sh ${sourceScopeId}
if [[ $? -ne 0 ]];then
  echo "警告: 清理临时数据失败，但不影响迁移结果"
fi

# 删除锁文件
if [[ -f "running.migration.lock" ]];then
  rm running.migration.lock
fi

# 记录起止时间与耗时
endTime=`date "+%Y-%m-%d %H:%M:%S"`
endTime_s=`date "+%s"`
sumTime=$[ $endTime_s - $startTime_s ]

echo ""
echo "========================================="
echo "迁移完成！"
echo "业务scopeId: ${sourceScopeId}"
echo "开始时间: ${startTime}"
echo "结束时间: ${endTime}"
echo "总耗时: ${sumTime} 秒"
echo "========================================="
