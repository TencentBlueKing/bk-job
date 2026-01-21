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

import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.manage.GlobalAppScopeMappingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * 未存活作业检查与处理
 */
@Slf4j
@Component
@EnableScheduling
public class NotAliveJobDetector {
    private final RunningJobResourceQuotaManager runningJobResourceQuotaManager;

    private final TaskInstanceService taskInstanceService;

    private final String requestId = UUID.randomUUID().toString();

    public NotAliveJobDetector(RunningJobResourceQuotaManager runningJobResourceQuotaManager,
                               TaskInstanceService taskInstanceService) {
        this.runningJobResourceQuotaManager = runningJobResourceQuotaManager;
        this.taskInstanceService = taskInstanceService;
    }

    /**
     * 兜底方案。为了防止系统异常、程序 bug 等原因导致 redis 中的作业记录没有被清理，需要定时清理。每10min触发一次
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void detectNotAliveJob() {
        try {
            if (LockUtils.tryGetDistributedLock("job:execute:not:alive:job:detect:lock", requestId, 60000L)) {
                log.info("Detect not alive job start ...");
                Set<Long> notAliveJobInstanceIds = runningJobResourceQuotaManager.getNotAliveJobInstanceIds();
                if (CollectionUtils.isEmpty(notAliveJobInstanceIds)) {
                    return;
                }
                log.info("Found not alive job, notAliveJobInstanceIds : {}", notAliveJobInstanceIds);
                notAliveJobInstanceIds.forEach(notAliveJobInstanceId -> {
                    TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(notAliveJobInstanceId);
                    if (taskInstance != null) {
                        log.info("Remove not alive job : {}", notAliveJobInstanceId);
                        runningJobResourceQuotaManager.removeJob(
                            taskInstance.getAppCode(),
                            GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId()),
                            notAliveJobInstanceId
                        );
                    } else {
                        log.error("Job instance record not found, notAliveJobInstanceId : {}", notAliveJobInstanceId);
                    }
                });
            }
        } catch (Throwable e) {
            log.error("Detect not alive job caught exception", e);
        }
    }

}
