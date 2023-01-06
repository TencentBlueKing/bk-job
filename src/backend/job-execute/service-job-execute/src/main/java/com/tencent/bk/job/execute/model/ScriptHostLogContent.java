/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 脚本日志内容
 */
@Data
@NoArgsConstructor
public class ScriptHostLogContent {
    /**
     * 步骤实例ID
     */
    private long stepInstanceId;
    /**
     * 执行次数
     */
    private int executeCount;
    /**
     * 主机ID
     */
    private Long hostId;
    /**
     * 目标云区域ID:ipv4
     */
    private String cloudIp;
    /**
     * 目标云区域ID:ipv6
     */
    private String cloudIpv6;
    /**
     * 主机IPv6
     */
    private String ipv6;
    /**
     * 日志内容
     */
    private String content;
    /**
     * 日志是否拉取完成
     */
    private boolean finished;

    public ScriptHostLogContent(long stepInstanceId,
                                int executeCount,
                                Long hostId,
                                String cloudIp,
                                String cloudIpv6,
                                String content,
                                boolean finished) {
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.hostId = hostId;
        this.cloudIp = cloudIp;
        this.cloudIpv6 = cloudIpv6;
        this.content = content;
        this.finished = finished;
    }

    /**
     * 获取主机的ip，优先返回ipv4
     *
     * @return 主机ipv4/ipv6, ipv4 优先
     */
    public String getPrimaryIp() {
        return StringUtils.isNotEmpty(cloudIp) ? cloudIp : cloudIpv6;
    }
}
