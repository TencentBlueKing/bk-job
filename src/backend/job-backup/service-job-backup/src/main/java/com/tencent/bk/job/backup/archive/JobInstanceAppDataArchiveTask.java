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
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceRecord;

import java.util.List;

/**
 * 作业实例数据归档-分库分表场景下，冗余的基于业务 ID 进行分库分表的数据
 */
public class JobInstanceAppDataArchiveTask extends AbstractJobInstanceArchiveTask<TaskInstanceRecord> {


    public JobInstanceAppDataArchiveTask(JobInstanceColdDAO jobInstanceColdDAO,
                                         ArchiveProperties archiveProperties,
                                         ArchiveTaskLock archiveTaskLock,
                                         ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                         JobInstanceArchiveTaskInfo archiveTask,
                                         ArchiveTaskService archiveTaskService,
                                         ArchiveTablePropsStorage archiveTablePropsStorage) {
        super(
            jobInstanceColdDAO,
            archiveProperties,
            archiveTaskLock,
            archiveErrorTaskCounter,
            archiveTask,
            archiveTaskService,
            archiveTablePropsStorage
        );
    }

    @Override
    protected void backupJobInstanceToColdDb(List<TaskInstanceRecord> jobInstances) {

    }

    @Override
    protected void deleteJobInstanceHotData(List<Long> jobInstanceIds) {

    }

    @Override
    protected List<TaskInstanceRecord> readSortedJobInstanceFromHotDB(Long fromTimestamp,
                                                                      Long endTimestamp,
                                                                      Long fromJobInstanceId,
                                                                      int limit) {
        return null;
    }

    @Override
    protected Long extractJobInstanceId(TaskInstanceRecord record) {
        return null;
    }

    @Override
    protected Long extractJobInstanceCreateTime(TaskInstanceRecord record) {
        return null;
    }
}
