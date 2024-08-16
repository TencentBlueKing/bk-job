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

import com.tencent.bk.job.backup.archive.dao.ArchiveTaskDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTask;
import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 作业执行历史热数据归档任务调度
 */
@Slf4j
public class JobInstanceHotDataArchiveTaskScheduler {

    private final ArchiveTaskTypeEnum archiveTaskType = ArchiveTaskTypeEnum.JOB_INSTANCE_HOT;

    private final ArchiveTaskDAO archiveTaskDAO;

    private final TaskInstanceRecordDAO taskInstanceRecordDAO;

    private final ArchiveDBProperties archiveDBProperties;

    private final ShardingProperties shardingProperties;

    private final ArchiveTaskScheduleLock archiveTaskScheduleLock;

    private final JobInstanceHotDataArchiveTaskGenerator jobInstanceHotDataArchiveTaskGenerator;

    private final Set<ArchiveTaskWorker> workers = new HashSet<>();

    public JobInstanceHotDataArchiveTaskScheduler(ArchiveTaskDAO archiveTaskDAO,
                                                  TaskInstanceRecordDAO taskInstanceRecordDAO,
                                                  ArchiveDBProperties archiveDBProperties,
                                                  ShardingProperties shardingProperties,
                                                  ArchiveTaskScheduleLock archiveTaskScheduleLock,
                                                  JobInstanceHotDataArchiveTaskGenerator jobInstanceHotDataArchiveTaskGenerator) {
        this.archiveTaskDAO = archiveTaskDAO;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.archiveDBProperties = archiveDBProperties;
        this.shardingProperties = shardingProperties;
        this.archiveTaskScheduleLock = archiveTaskScheduleLock;
        this.jobInstanceHotDataArchiveTaskGenerator = jobInstanceHotDataArchiveTaskGenerator;
    }

    @Scheduled(cron = "${job.backup.archive.execute.jobInstanceHotData.cron:0 0 4 * * *}")
    public void schedule() {
        try {
            // 获取归档任务调度锁
            archiveTaskScheduleLock.lock(archiveTaskType);
            // 生成归档任务，并存储到 db 中
            jobInstanceHotDataArchiveTaskGenerator.generate();
            List<JobInstanceArchiveTask> runningTasks =
                archiveTaskDAO.listRunningTasks(ArchiveTaskTypeEnum.JOB_INSTANCE_HOT);

            List<JobInstanceArchiveTask> needScheduleTasks =
                archiveTaskDAO.listScheduleTasks(ArchiveTaskTypeEnum.JOB_INSTANCE_HOT, 100);

            ArchiveTaskPriorityEvaluator.sort(runningTasks, shardingProperties.getDbNodeCount(), needScheduleTasks);


        } finally {
            archiveTaskScheduleLock.unlock(archiveTaskType);
        }

    }

    private void startArchiveTask(JobInstanceArchiveTask archiveTask) {
        ArchiveTaskWorker worker = new ArchiveTaskWorker(archiveTask);
        workers.add(worker);
        worker.start();
    }


}
