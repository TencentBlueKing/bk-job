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
# 说明：该脚本用于将生成的全局迁移数据应用到目标环境
# ##########################################

# 加载配置信息
source 0_config_common.sh

echo "========================================="
echo "开始应用全局迁移数据到目标环境..."
echo "========================================="

# 检查SQL文件是否存在
if [[ ! -d "sql/global" ]]; then
  echo "错误: 找不到SQL文件目录 sql/global/"
  echo "请先运行 1_gen_global_data.sh 生成迁移数据"
  exit 1
fi

# 1. 数据导入目标环境的表
echo "步骤1: 导入 job_manage 全局数据..."
ALL_SQL=($(echo ./sql/global/job_manage/*.sql))
for sql in "${ALL_SQL[@]}"; do
  if [[ -f "$sql" ]]; then
    echo "  导入: $sql"
    mysql -h${targetMysqlHost} -P${targetMysqlPort} -u${targetMysqlUser} -p${targetMysqlPassword} --default-character-set=utf8mb4 ${targetJobManageDb} < $sql
    if [[ $? -ne 0 ]]; then
      echo "  错误: 导入失败 $sql"
      exit 1
    fi
  fi
done

echo "========================================="
echo "全局迁移数据应用完成！"
echo "已迁移的全局资源:"
echo "  - dangerous_rule (高危规则)"
echo "  - notify_black_user_info (通知黑名单)"
echo "  - script (公共脚本，is_public=1)"
echo "  - script_version (公共脚本版本)"
echo "========================================="

