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

package com.tencent.bk.job.execute.engine.schedule.ha;

import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.execute.engine.schedule.metrics.NotAliveScheduleTasksCounter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Set;
import java.util.UUID;

/**
 * 故障终止的任务发现与恢复
 */
@Slf4j
public class NotAliveScheduleTaskDetector {
    private final ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager;
    private final NotAliveScheduleTasksCounter notAliveScheduleTasksCounter;
    private final String requestId = UUID.randomUUID().toString();
    private final String scheduleName;
    private final String lockKey;

    public NotAliveScheduleTaskDetector(String scheduleName,
                                        MeterRegistry meterRegistry,
                                        ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager) {
        this.scheduleName = scheduleName;
        this.lockKey = "not:alive:task:detect:lock:" + scheduleName;
        this.scheduleTaskKeepaliveManager = scheduleTaskKeepaliveManager;
        this.notAliveScheduleTasksCounter = new NotAliveScheduleTasksCounter(meterRegistry, scheduleName);
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void detectAndResumeNotAliveTasks() {
        try {
            if (LockUtils.tryGetDistributedLock(lockKey, requestId, 5000L)) {
                log.info("[{}] Detect not alive tasks start ...", scheduleName);
                Set<String> notAliveTaskIds = scheduleTaskKeepaliveManager.getNotAliveTaskIds();
                if (CollectionUtils.isEmpty(notAliveTaskIds)) {
                    return;
                }
                log.info("[{}] Found not alive tasks, notAliveTaskIds : {}", scheduleName, notAliveTaskIds);
                notAliveScheduleTasksCounter.increment(notAliveTaskIds.size());
                resumeTasks(notAliveTaskIds);
            }
        } catch (Throwable e) {
            log.error("Detect not alive tasks caught exception", e);
        }
    }

    private void resumeTasks(Set<String> notAliveTaskIds) {
        log.info("[{}] Resume not alive tasks start ...", scheduleName);
        notAliveTaskIds.forEach(taskId -> {
            // 暂时只支持恢复GSE任务
            if (taskId.startsWith("gse_task")) {
                String[] taskInfo = taskId.split(":");
                long stepInstanceId = Long.parseLong(taskInfo[1]);
                int executeCount = Integer.parseInt(taskInfo[2]);
                // 暂时不转移
//                taskExecuteControlMsgSender.resumeGseStep(stepInstanceId, executeCount, UUID.randomUUID().toString());
                scheduleTaskKeepaliveManager.removeTaskKeepaliveInfoFromRedis(taskId);
            }
        });
        log.info("[{}] Resume not alive tasks successfully", scheduleName);
    }
}
