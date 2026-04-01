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
# 说明：清理目标环境中已迁移的全局数据（回滚用）
# 警告：此操作会删除目标环境中的全局资源数据，请谨慎使用！
# ##########################################

# 加载配置信息
source 0_config_common.sh

echo "========================================="
echo "警告：此操作将删除目标环境中的全局资源数据！"
echo "========================================="
echo ""
echo "将删除的数据："
echo "  - dangerous_rule (高危规则)"
echo "  - notify_black_user_info (通知黑名单)"
echo "  - script (公共脚本，is_public=1)"
echo "  - script_version (公共脚本版本)"
echo ""
echo "开始清理目标环境中的全局数据..."

# (1)清理高危规则
echo "  删除高危规则..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`dangerous_rule\`;"

# (2)清理通知黑名单
echo "  删除通知黑名单..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`notify_black_user_info\`;"

# (3)清理公共脚本版本
echo "  删除公共脚本版本..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`script_version\` where script_id in (select id from \`${targetJobManageDb}\`.\`script\` where is_public=1);"

# (4)清理公共脚本
echo "  删除公共脚本..."
executeSqlInTargetDb "delete from \`${targetJobManageDb}\`.\`script\` where is_public=1;"

echo "========================================="
echo "全局数据清理完成！"
echo ""
echo "已清理的资源："
echo "  ✅ dangerous_rule (高危规则)"
echo "  ✅ notify_black_user_info (通知黑名单)"
echo "  ✅ script (公共脚本)"
echo "  ✅ script_version (公共脚本版本)"
echo "========================================="

