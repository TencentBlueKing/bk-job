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

package com.tencent.bk.job.backup.config;

import com.tencent.bk.job.backup.constant.ArchiveModeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 作业执行日志归档配置
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "job.backup.archive.execute-log")
public class JobLogArchiveProperties {
    /**
     * 是否启用执行日志归档
     */
    private boolean enabled = false;

    /**
     * 是否试运行
     */
    private boolean dryRun = true;

    /**
     * 执行日志归档模式
     *
     * @see ArchiveModeEnum
     */
    private String mode;

    /**
     * 归档任务触发时间
     */
    private String cron;

    /**
     * 归档数据时间范围计算所依据的时区，如果不指定默认为系统时区
     */
    private String timeZone;

    /**
     * 执行日志保留天数
     */
    private int keepDays = 360;

    /**
     * 执行日志归档任务并行数量
     */
    private Integer concurrent = 6;
}
