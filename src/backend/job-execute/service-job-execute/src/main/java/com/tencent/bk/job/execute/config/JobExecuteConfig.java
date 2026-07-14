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

package com.tencent.bk.job.execute.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class JobExecuteConfig {

    /**
     * 功能开关 - 启用账号鉴权
     */
    @Value("${feature.toggle.auth-account.mode:enabled}")
    private String enableAuthAccountMode;

    /**
     * 账号鉴权灰度业务(用,分隔)
     */
    @Value("${feature.toggle.auth-account.gray.apps:}")
    private String accountAuthGrayApps;

    @Value("${job.execute.result.handle.tasks.limit: 2000}")
    private int resultHandleTasksLimit;

    /**
     * Symmetric encryption password
     */
    @Value("${job.encrypt.password}")
    private String encryptPassword;

    @Value("${job.execute.limit.file-task.max-tasks:100000}")
    private Integer fileTasksMax;

    @Value("${job.execute.limit.script-task.max-target-server:50000}")
    private Integer scriptTaskMaxTargetServer;

    @Value("${gse.script.rootPath:/tmp/bkjob}")
    private String gseScriptFileRootPath;

    /**
     * 当脚本在容器上执行时，脚本在容器内目录会在主机目录前再加个前缀
     */
    @Value("${gse.script.containerScriptPathPrefix:/bktmp}")
    private String gseScriptContainerPathPrefix;

    /**
     * GSE 脚本任务执行结果查询 API 单次返回的执行输出内容长度
     * 默认值：512M
     */
    @Value("${job.execute.scriptTask.query.contentSizeLimit:512MB}")
    private String scriptTaskQueryContentSizeLimit;

    /**
     * 滚动并行错峰模式下允许的最大批次总数，超出则校验拒绝，防止并行 GSE 任务规模失控
     */
    @Value("${job.execute.rolling.scatter.max-batch:200}")
    private int rollingScatterMaxBatch;

    /**
     * 滚动并行错峰调度器消费者线程数
     */
    @Value("${job.execute.rolling.scatter.worker-num:3}")
    private int rollingScatterWorkerNum;

    /**
     * 是否启用「运行中作业配额」系统级加权计数的周期性对账任务，默认启用
     */
    @Value("${job.execute.runningJobQuota.reconcile.enabled:true}")
    private boolean runningJobQuotaReconcileEnabled;
}
