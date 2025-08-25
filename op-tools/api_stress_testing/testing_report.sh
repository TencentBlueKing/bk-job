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

set -x
cd ${{ci.workspace}}
pwd

current_time=$(date +%Y%m%d%H%M)
filename="执行历史接口压测_${current_time}_${{processCnt}}_${{concurrentCnt}}_report.md"
setEnv "filename" "${filename}"
touch ${filename}

echo -e "|指标|值|\n|---|---|\n" \
"|平均响应耗时|${{avg_time}} ms|\n" \
"|最大响应耗时|${{max_time}} ms|\n" \
"|最大响应耗时|${{min_time}} ms|\n" \
"|错误率|${{error_rate}} %|\n" \
"|所有请求发送时间|${{time_spent_sending_requests}} ms|\n" \
> ${filename}
