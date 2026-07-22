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

package com.tencent.bk.job.execute.engine.quota.limit;

import com.tencent.bk.job.common.annotation.ScheduledOnOperationTimeZone;
import com.tencent.bk.job.common.redis.util.DistributedUniqueTask;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

/**
 * 运行中作业配额系统级加权计数对账任务。
 * <p>
 * 系统级加权计数（{@code job:execute:running:job:weighted:count:system}）为增量维护的缓存，
 * 滚动升级/混版窗口/崩溃部分更新可能导致其与真值（在跑作业权重之和）产生漂移。本任务周期性以真值重算刷新，
 * 使其强制收敛，而非依赖计数触底归零的「自愈」。
 * <p>
 * 多副本下通过 {@link DistributedUniqueTask}（心跳锁执行期间自动续期）保证同一时刻仅一个实例执行。
 */
@Slf4j
@Component
@EnableScheduling
public class RunningJobQuotaReconcileTask {

    private static final String RECONCILE_LOCK_KEY = "job:execute:running:job:quota:reconcile:lock";

    private final RunningJobResourceQuotaManager runningJobResourceQuotaManager;
    private final StringRedisTemplate redisTemplate;
    private final JobExecuteConfig jobExecuteConfig;
    private final String machineIp = IpUtils.getFirstMachineIP();

    public RunningJobQuotaReconcileTask(RunningJobResourceQuotaManager runningJobResourceQuotaManager,
                                        StringRedisTemplate redisTemplate,
                                        JobExecuteConfig jobExecuteConfig) {
        this.runningJobResourceQuotaManager = runningJobResourceQuotaManager;
        this.redisTemplate = redisTemplate;
        this.jobExecuteConfig = jobExecuteConfig;
    }

    /**
     * 每 5 分钟对账一次系统级加权计数。
     */
    @ScheduledOnOperationTimeZone(cron = "0 0/5 * * * ?")
    public void reconcile() {
        if (!jobExecuteConfig.isRunningJobQuotaReconcileEnabled()) {
            return;
        }
        try {
            new DistributedUniqueTask<>(
                redisTemplate,
                this.getClass().getSimpleName(),
                RECONCILE_LOCK_KEY,
                machineIp,
                () -> {
                    runningJobResourceQuotaManager.reconcileSystemWeightedCount();
                    return null;
                }
            ).execute();
        } catch (Throwable e) {
            log.error("Reconcile running job quota system weighted count caught exception", e);
        }
    }
}
