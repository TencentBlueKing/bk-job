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

import com.tencent.bk.job.backup.archive.model.ArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveLogTaskExecuteLock;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import lombok.extern.slf4j.Slf4j;

/**
 * 作业执行日志归档任务实现
 */
@Slf4j
public class JobExecuteLogArchiveTask extends AbstractHistoricalDataArchiveTask {
    private final ArchiveLogTaskExecuteLock archiveLogTaskExecuteLock;
    private final JobExecuteLogArchivers jobExecuteLogArchivers;

    public JobExecuteLogArchiveTask(ArchiveProperties archiveProperties,
                                    ArchiveLogTaskExecuteLock archiveLogTaskExecuteLock,
                                    ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                    ArchiveTaskInfo archiveTaskInfo,
                                    ArchiveTaskService archiveTaskService,
                                    JobExecuteLogArchivers jobExecuteLogArchivers) {
        super(archiveProperties, archiveErrorTaskCounter, archiveTaskInfo, archiveTaskService);
        this.archiveLogTaskExecuteLock = archiveLogTaskExecuteLock;
        this.jobExecuteLogArchivers = jobExecuteLogArchivers;
    }

    @Override
    public void backupAndDelete() {
        long startTime = System.currentTimeMillis();
        log.info("[{}] archive log task mode: {}", taskId, archiveProperties.getExecuteLog().getMode());
        jobExecuteLogArchivers.getAll().forEach(archiver -> {
            archiver.backupRecords(archiveTaskInfo.getDay());
            archiver.deleteRecords(archiveTaskInfo.getDay());
        });
        long archiveCost = System.currentTimeMillis() - startTime;
        if (archiveProperties.getExecuteLog().isDryRun()) {
            // dry-run模式，状态设置成pending，下次重新调度
            updateCompletedExecuteInfo(ArchiveTaskStatusEnum.PENDING, null);
            return;
        }
        setArchiveTaskExecutionDetail(null, archiveCost, null);
        updateCompletedExecuteInfo(ArchiveTaskStatusEnum.SUCCESS, null);
    }

    @Override
    public boolean acquireLock() {
        this.isAcquireLock = archiveLogTaskExecuteLock.lock(taskId);
        if (!isAcquireLock) {
            log.info("{} [{}] Acquire archive log task lock fail", getClass().getSimpleName(), taskId);
        }
        return isAcquireLock;
    }

    @Override
    protected void unlock() {
        archiveLogTaskExecuteLock.unlock(taskId);
    }
}
