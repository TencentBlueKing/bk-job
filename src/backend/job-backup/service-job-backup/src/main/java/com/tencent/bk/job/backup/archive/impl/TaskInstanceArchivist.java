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

package com.tencent.bk.job.backup.archive.impl;

import com.tencent.bk.job.backup.archive.AbstractArchivist;
import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import com.tencent.bk.job.backup.dao.JobExecuteDAO;
import com.tencent.bk.job.backup.service.ArchiveProgressService;
import org.jooq.generated.tables.TaskInstance;
import org.jooq.generated.tables.records.TaskInstanceRecord;

import java.io.IOException;
import java.util.List;

public class TaskInstanceArchivist extends AbstractArchivist<TaskInstanceRecord> {

    public TaskInstanceArchivist(JobExecuteDAO jobExecuteDAO,
                                 ExecuteArchiveDAO executeArchiveDAO,
                                 ArchiveProgressService archiveProgressService) {
        this.jobExecuteDAO = jobExecuteDAO;
        this.executeArchiveDAO = executeArchiveDAO;
        this.archiveProgressService = archiveProgressService;
        this.deleteIdStepSize = 10_000;
        this.setTableName("task_instance");
    }

    @Override
    public List<TaskInstanceRecord> listRecord(Long start, Long stop) {
        return jobExecuteDAO.listTaskInstance(start, stop);
    }

    @Override
    protected int batchInsert(List<TaskInstanceRecord> recordList) throws IOException {
        return executeArchiveDAO.batchInsert(jobExecuteDAO.getTaskInstanceFields(), recordList, 500);
    }

    @Override
    protected int deleteRecord(Long start, Long stop) {
        return jobExecuteDAO.deleteTaskInstance(start, stop);
    }

    @Override
    protected long getFirstInstanceId() {
        return jobExecuteDAO.getFirstInstanceId(TaskInstance.TASK_INSTANCE, TaskInstance.TASK_INSTANCE.ID);
    }
}
