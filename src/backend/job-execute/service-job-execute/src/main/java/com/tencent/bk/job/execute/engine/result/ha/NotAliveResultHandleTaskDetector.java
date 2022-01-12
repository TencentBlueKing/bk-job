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

package com.tencent.bk.job.execute.engine.result.ha;

import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * 故障终止的任务发现与恢复
 */
@Slf4j
@Component
@EnableScheduling
public class NotAliveResultHandleTaskDetector {
    private final ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final ExecuteMonitor executeMonitor;
    private final String requestId = UUID.randomUUID().toString();

    public NotAliveResultHandleTaskDetector(ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                            TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                            ExecuteMonitor executeMonitor) {
        this.resultHandleTaskKeepaliveManager = resultHandleTaskKeepaliveManager;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.executeMonitor = executeMonitor;
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void detectAndResumeNotAliveTasks() {
        try {
            if (LockUtils.tryGetDistributedLock("not:alive:task:detect:lock", requestId, 5000L)) {
                log.info("Detect not alive tasks start ...");
                Set<String> notAliveTaskIds = resultHandleTaskKeepaliveManager.getNotAliveTaskIds();
                if (CollectionUtils.isEmpty(notAliveTaskIds)) {
                    return;
                }
                log.info("Found not alive tasks, notAliveTaskIds : {}", notAliveTaskIds);
                executeMonitor.getNotAliveTasksCounter().increment(notAliveTaskIds.size());
                resumeTasks(notAliveTaskIds);
            }
        } catch (Throwable e) {
            log.error("Detect not alive tasks caught exception", e);
        }
    }

    private void resumeTasks(Set<String> notAliveTaskIds) {
        log.info("Resume not alive tasks start ...");
        notAliveTaskIds.forEach(taskId -> {
            // 暂时只支持恢复GSE任务
            if (taskId.startsWith("gse_task")) {
                String[] taskInfo = taskId.split(":");
                long stepInstanceId = Long.parseLong(taskInfo[1]);
                int executeCount = Integer.parseInt(taskInfo[2]);
                // 暂时不转移
//                taskExecuteControlMsgSender.resumeGseStep(stepInstanceId, executeCount, UUID.randomUUID().toString());
                resultHandleTaskKeepaliveManager.removeTaskKeepaliveInfoFromRedis(taskId);
            }
        });
        log.info("Resume not alive tasks successfully");
    }
}
