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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.FailedArchiveTaskRescheduleLock;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * 异常归档任务重调度
 */
@Slf4j
public class AbnormalArchiveTaskReScheduler {

    /**
     * 归档任务超时时间，用于判定归档任务是否调度异常
     */
    private static final long TIMEOUT_MILLS = 86400000L;

    private final ArchiveTaskService archiveTaskService;

    private final FailedArchiveTaskRescheduleLock failedArchiveTaskRescheduleLock;


    public AbnormalArchiveTaskReScheduler(ArchiveTaskService archiveTaskService,
                                          FailedArchiveTaskRescheduleLock failedArchiveTaskRescheduleLock) {
        this.archiveTaskService = archiveTaskService;
        this.failedArchiveTaskRescheduleLock = failedArchiveTaskRescheduleLock;
    }

    /**
     * 重新调度异常任务（执行失败、超时未结束）
     */
    public void rescheduleFailedArchiveTasks() {
        boolean locked = false;
        try {
            log.info("Abnormal archive task reSchedule start ...");
            locked = failedArchiveTaskRescheduleLock.lock();
            if (!locked) {
                log.info("Get failed archive reSchedule lock fail");
                return;
            }
            // 处理失败的任务
            reScheduleFailedTasks();
            // 处理超时未结束的任务
            reScheduleTimeoutTasks();
        } finally {
            if (locked) {
                failedArchiveTaskRescheduleLock.unlock();
            }
            log.info("Abnormal archive task reSchedule end");
        }
    }

    private void reScheduleFailedTasks() {
        int readLimit = 100;
        List<JobInstanceArchiveTaskInfo> failedTasks;
        do {
            failedTasks =
                archiveTaskService.listTasks(ArchiveTaskTypeEnum.JOB_INSTANCE, ArchiveTaskStatusEnum.FAIL,
                    readLimit);
            if (CollectionUtils.isEmpty(failedTasks)) {
                return;
            }
            // 设置为 pending 状态，会被重新调度
            failedTasks.forEach(failTask -> {
                log.info("Found fail archive task, and set archive task status to pending, taskId : {}",
                    failTask.buildTaskUniqueId());
                archiveTaskService.updateArchiveTaskStatus(
                    failTask.getTaskType(),
                    failTask.getDbDataNode(),
                    failTask.getDay(),
                    failTask.getHour(),
                    ArchiveTaskStatusEnum.PENDING
                );
            });
        } while (failedTasks.size() == readLimit);
    }

    private void reScheduleTimeoutTasks() {
        List<JobInstanceArchiveTaskInfo> runningTasks =
            archiveTaskService.listRunningTasks(ArchiveTaskTypeEnum.JOB_INSTANCE);
        if (CollectionUtils.isEmpty(runningTasks)) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        runningTasks.forEach(runningTask -> {
            // 如果归档任务没有正常结束，通过当前时间减去任务最后修改时间计算执行时长，判断是否超过合理的执行时长
            if (currentTime - runningTask.getLastUpdateTime() > TIMEOUT_MILLS) {
                log.info("Found timeout archive task, and set archive task status to pending. taskId: {}",
                    runningTask.buildTaskUniqueId());
                // 设置为 pending 状态，会被重新调度
                archiveTaskService.updateArchiveTaskStatus(
                    runningTask.getTaskType(),
                    runningTask.getDbDataNode(),
                    runningTask.getDay(),
                    runningTask.getHour(),
                    ArchiveTaskStatusEnum.PENDING
                );
            }
        });
    }
}
