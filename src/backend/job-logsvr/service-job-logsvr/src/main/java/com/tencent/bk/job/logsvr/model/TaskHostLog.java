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

package com.tencent.bk.job.logsvr.model;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;

/**
 * 任务执行日志
 */
@Getter
@Setter
public class TaskHostLog {
    /**
     * 文件任务执行日志
     */
    List<FileTaskLogDoc> fileTaskLogs;
    /**
     * 作业实例创建时间,格式yyyy_MM_dd
     */
    private String jobCreateDate;
    /**
     * 作业步骤实例ID
     */
    private Long stepInstanceId;
    /**
     * 执行任务的主机ip
     */
    @CompatibleImplementation(explain = "兼容参数,由于IP不再唯一，后续使用hostId参数替换", version = "3.7.x")
    private String ip;
    /**
     * 主机ID
     */
    private Long hostId;
    /**
     * 执行次数
     */
    private Integer executeCount;
    /**
     * 滚动批次
     */
    private Integer batch;
    /**
     * 脚本任务执行日志
     */
    private ScriptTaskLogDoc scriptTaskLog;
    /**
     * 脚本任务执行日志内容
     */
    private String scriptContent;
    /**
     * 日志类型
     *
     * @see com.tencent.bk.job.logsvr.consts.LogTypeEnum
     */
    private Integer logType;

    @Override
    public String toString() {
        return new StringJoiner(", ", TaskHostLog.class.getSimpleName() + "[", "]")
            .add("jobCreateDate='" + jobCreateDate + "'")
            .add("stepInstanceId=" + stepInstanceId)
            .add("hostId='" + hostId + "'")
            .add("ip='" + ip + "'")
            .add("executeCount=" + executeCount)
            .add("batch=" + batch)
            .add("scriptContentLength=" + (StringUtils.isEmpty(scriptContent) ? 0 : scriptContent.length()))
            .add("logType=" + logType)
            .toString();
    }
}
