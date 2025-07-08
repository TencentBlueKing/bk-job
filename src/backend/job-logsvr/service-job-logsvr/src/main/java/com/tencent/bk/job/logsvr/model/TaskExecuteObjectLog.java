/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
import com.tencent.bk.job.common.constant.CompatibleType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 执行对象任务执行日志
 */
@Getter
@Setter
public class TaskExecuteObjectLog {
    /**
     * 作业实例创建时间,格式yyyy_MM_dd
     */
    private String jobCreateDate;
    /**
     * 作业步骤实例ID
     */
    private Long stepInstanceId;
    /**
     * 执行对象 ID
     */
    private String executeObjectId;
    /**
     * 主机ipv4,格式: 云区域ID:IPv4
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    private String ip;
    /**
     * 主机ipv6,格式: 云区域ID:IPv6
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    private String ipv6;
    /**
     * 主机ID
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
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
     * 文件任务执行日志
     */
    List<FileTaskLogDoc> fileTaskLogs;
    /**
     * 日志类型
     *
     * @see com.tencent.bk.job.logsvr.consts.LogTypeEnum
     */
    private Integer logType;
}
