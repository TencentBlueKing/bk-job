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

# ###### 新迁移工具配置信息 ######

# 源环境数据库配置（老环境）
sourceMysqlHost="127.0.0.1"
sourceMysqlPort="3306"
sourceMysqlUser="root"
sourceMysqlPassword="123456"
sourceJobManageDb="job_manage"
sourceJobCrontabDb="job_crontab"
sourceJobExecuteDb="job_execute"
sourceJobFileGatewayDb="job_file_gateway"

# 目标环境数据库配置（新环境）
targetMysqlHost="127.0.0.1"
targetMysqlPort="3306"
targetMysqlUser="root"
targetMysqlPassword="123456"
targetJobManageDb="job_manage"
targetJobCrontabDb="job_crontab"
targetJobExecuteDb="job_execute"
targetJobFileGatewayDb="job_file_gateway"

# 迁移用的临时数据库配置，用于完成数据转换操作
# 说明：临时数据库可以是独立的 MySQL 实例，不需要与源数据库在同一实例
# 工具会在准备阶段（tool_prepare_db.sh）自动将源数据库的表结构和数据 dump 到临时数据库
tmpMysqlHost="127.0.0.1"
tmpMysqlPort="3306"
# 用户名：需要拥有创建、删除库的权限
tmpMysqlRootUser="root"
tmpMysqlRootPassword="123456"
tmpMigrationDb="new_migration"

# 蓝盾制品库（BK-Repo）URL 配置
# 用于替换 file_source 表中 BLUEKING_ARTIFACTORY 类型文件源的 custom_info.base_url
# 如果不需要替换（新老环境共用同一个制品库），可以将两个值设为相同
sourceBkRepoUrl="http://bkrepo-old.example.com"
targetBkRepoUrl="http://bkrepo-new.example.com"

# 要填充的租户ID
defaultTenantId="tencent"

# ############   配置信息结束    #############

# 公共基础函数
dbResult=""
function showDbResult(){
  _tmp=""
  #echo "dbResult=${dbResult}"
}

function executeSqlInTmpDb(){
  echo "execute sql: mysql -h${tmpMysqlHost} -P${tmpMysqlPort} -u${tmpMysqlRootUser} -p${tmpMysqlRootPassword} -e \"$1\""
  dbResult=$(mysql -h${tmpMysqlHost} -P${tmpMysqlPort} -u${tmpMysqlRootUser} -p${tmpMysqlRootPassword} -e "$1")
  showDbResult
}

# 用于 SELECT 查询并获取结果的函数（不输出 debug 信息，直接返回结果）
function querySqlInTmpDb(){
  mysql -h${tmpMysqlHost} -P${tmpMysqlPort} -u${tmpMysqlRootUser} -p${tmpMysqlRootPassword} -N -e "$1" 2>/dev/null
}

function executeSqlInSourceDb(){
  echo "execute sql: mysql -h${sourceMysqlHost} -P${sourceMysqlPort} -u${sourceMysqlUser} -p${sourceMysqlPassword} -e \"$1\""
  dbResult=$(mysql -h${sourceMysqlHost} -P${sourceMysqlPort} -u${sourceMysqlUser} -p${sourceMysqlPassword} -e "$1")
  showDbResult
}

# 用于 SELECT 查询并获取结果的函数（不输出 debug 信息，直接返回结果）
function querySqlInSourceDb(){
  mysql -h${sourceMysqlHost} -P${sourceMysqlPort} -u${sourceMysqlUser} -p${sourceMysqlPassword} -N -e "$1" 2>/dev/null
}

function executeSqlInTargetDb(){
  echo "execute sql: mysql -h${targetMysqlHost} -P${targetMysqlPort} -u${targetMysqlUser} -p${targetMysqlPassword} -e \"$1\""
  dbResult=$(mysql -h${targetMysqlHost} -P${targetMysqlPort} -u${targetMysqlUser} -p${targetMysqlPassword} -e "$1")
  showDbResult
}

# 用于 SELECT 查询并获取结果的函数（不输出 debug 信息，直接返回结果）
function querySqlInTargetDb(){
  mysql -h${targetMysqlHost} -P${targetMysqlPort} -u${targetMysqlUser} -p${targetMysqlPassword} -N -e "$1" 2>/dev/null
}

# 迁移工具名称
systemName=$(uname -s)
echo "systemName=${systemName}"
migratorName=""
if [[ "${systemName}" =~ "MINGW" ]];then
  migratorName="job-migration.exe"
elif [[ "${systemName}" =~ "Linux" ]];then
  migratorName="job-migration"
fi
echo "migratorName=${migratorName}"
