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

import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveTaskExecuteLock;
import com.tencent.bk.job.backup.archive.util.lock.JobInstanceArchiveTaskScheduleLock;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 作业执行历史归档任务调度
 */
@Slf4j
public class JobInstanceArchiveTaskScheduler extends AbstractArchiveTaskScheduler<JobInstanceMainDataArchiveTask> {

    private final JobInstanceHotRecordDAO taskInstanceRecordDAO;
    private final JobInstanceSubTableArchivers jobInstanceSubTableArchivers;
    private final JobInstanceColdDAO jobInstanceColdDAO;
    private final ArchiveTaskExecuteLock archiveTaskExecuteLock;
    private final JobInstanceArchiveTaskScheduleLock scheduleLock;
    private final ArchiveTablePropsStorage archiveTablePropsStorage;
    private final ArchiveProperties archiveProperties;

    public JobInstanceArchiveTaskScheduler(ArchiveTaskService archiveTaskService,
                                           JobInstanceHotRecordDAO taskInstanceRecordDAO,
                                           ArchiveProperties archiveProperties,
                                           JobInstanceArchiveTaskScheduleLock scheduleLock,
                                           JobInstanceSubTableArchivers jobInstanceSubTableArchivers,
                                           JobInstanceColdDAO jobInstanceColdDAO,
                                           ArchiveTaskExecuteLock archiveTaskExecuteLock,
                                           ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                           ArchiveTablePropsStorage archiveTablePropsStorage,
                                           Tracer tracer,
                                           ExecutorService archiveTaskStopExecutor) {
        super(archiveTaskService, archiveTaskStopExecutor, archiveErrorTaskCounter, tracer);
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.scheduleLock = scheduleLock;
        this.jobInstanceSubTableArchivers = jobInstanceSubTableArchivers;
        this.jobInstanceColdDAO = jobInstanceColdDAO;
        this.archiveTaskExecuteLock = archiveTaskExecuteLock;
        this.archiveTablePropsStorage = archiveTablePropsStorage;
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
        return archiveTaskService.countScheduleTasksGroupByDb(ArchiveTaskTypeEnum.JOB_INSTANCE);
    }

    @Override
    protected List<ArchiveTaskInfo> listRunningTasks() {
        return archiveTaskService.listRunningTasks(ArchiveTaskTypeEnum.JOB_INSTANCE);
    }

    @Override
    protected boolean shouldWait(StopWatch watch,
                                 List<ArchiveTaskInfo> runningTasks,
                                 Map<String, Integer> scheduleTasksGroupByDb) {
        // 对待调度的任务进行优先级排序，保证同一个 db 上的归档任务数量尽可能均衡，避免出现db 热点
        watch.start("evaluateTaskPriority");
        ArchiveDbNodePriorityEvaluator.DbNodeTasksInfo highestPriorityDbNodeTasksInfo =
            ArchiveDbNodePriorityEvaluator.evaluateHighestPriorityDbNode(runningTasks,
                scheduleTasksGroupByDb);
        watch.stop();
        return highestPriorityDbNodeTasksInfo.getRunningTaskCount() >= getTaskMaxConcurrent();
    }

    @Override
    protected ArchiveTaskInfo getNextTask(Map<String, Integer> scheduleTasksGroupByDb,
                                          List<ArchiveTaskInfo> running) {
        // 根据优先级评估
        ArchiveDbNodePriorityEvaluator.DbNodeTasksInfo info =
            ArchiveDbNodePriorityEvaluator.evaluateHighestPriorityDbNode(running, scheduleTasksGroupByDb);
        return archiveTaskService.getFirstScheduleArchiveTaskByDb(ArchiveTaskTypeEnum.JOB_INSTANCE,
            info.getDbNodeId());
    }

    @Override
    protected Integer getTaskMaxConcurrent() {
        return archiveProperties.getTasks().getJobInstance().getConcurrent();
    }

    @Override
    protected JobInstanceMainDataArchiveTask createArchiveTask(ArchiveTaskInfo archiveTaskInfo) {
        return new JobInstanceMainDataArchiveTask(
            taskInstanceRecordDAO,
            jobInstanceSubTableArchivers,
            jobInstanceColdDAO,
            archiveProperties,
            archiveTaskExecuteLock,
            super.archiveErrorTaskCounter,
            archiveTaskInfo,
            super.archiveTaskService,
            archiveTablePropsStorage
        );
    }
}
