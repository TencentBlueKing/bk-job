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
import com.tencent.bk.job.backup.archive.util.lock.JobLogArchiveTaskScheduleLock;
import com.tencent.bk.job.backup.config.JobLogArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 作业执行日志归档任务调度
 */
@Slf4j
public class JobLogArchiveTaskScheduler extends AbstractArchiveTaskScheduler<JobLogArchiveTask> {

    private final JobLogArchiveTaskScheduleLock scheduleLock;
    private final JobLogArchivers jobLogArchivers;
    private final ArchiveLogTaskExecuteLock archiveLogTaskExecuteLock;
    private final JobLogArchiveProperties archiveProperties;

    public JobLogArchiveTaskScheduler(ArchiveTaskService archiveTaskService,
                                      JobLogArchiveProperties archiveProperties,
                                      JobLogArchiveTaskScheduleLock scheduleLock,
                                      JobLogArchivers jobLogArchivers,
                                      ArchiveLogTaskExecuteLock archiveLogTaskExecuteLock,
                                      ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                      Tracer tracer,
                                      ExecutorService archiveTaskStopExecutor) {
        super(archiveTaskService, archiveTaskStopExecutor, archiveErrorTaskCounter, tracer);
        this.scheduleLock = scheduleLock;
        this.jobLogArchivers = jobLogArchivers;
        this.archiveLogTaskExecuteLock = archiveLogTaskExecuteLock;
        this.archiveProperties = archiveProperties;
    }

    @Override
    protected boolean acquireScheduleLock() {
        return scheduleLock.lock();
    }

    @Override
    protected void releaseScheduleLock() {
        scheduleLock.unlock();
    }

    @Override
    protected Map<String, Integer> countScheduleTasksGroupByDb() {
        return archiveTaskService.countScheduleTasksGroupByDb(ArchiveTaskTypeEnum.JOB_EXECUTE_LOG);
    }

    @Override
    protected List<ArchiveTaskInfo> listRunningTasks() {
        return archiveTaskService.listRunningTasks(ArchiveTaskTypeEnum.JOB_EXECUTE_LOG);
    }

    @Override
    protected ArchiveTaskInfo getNextTask(Map<String, Integer> group, List<ArchiveTaskInfo> running) {
        return archiveTaskService.getFirstScheduleArchiveTask(ArchiveTaskTypeEnum.JOB_EXECUTE_LOG);
    }

    @Override
    protected Integer getTaskMaxConcurrent() {
        return archiveProperties.getConcurrent();
    }

    @Override
    protected JobLogArchiveTask createArchiveTask(ArchiveTaskInfo archiveTaskInfo) {
        return new JobLogArchiveTask(
            archiveProperties,
            archiveLogTaskExecuteLock,
            super.archiveErrorTaskCounter,
            archiveTaskInfo,
            super.archiveTaskService,
            jobLogArchivers
        );
    }
}
