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
import com.tencent.bk.job.backup.archive.dao.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.FileSourceTaskLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseFileAgentTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseFileExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseScriptAgentTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseScriptExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.OperationLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.RollingConfigRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceConfirmRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceFileRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceRollingTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceScriptRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTask;
import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.execute.model.tables.TaskInstance;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 作业实例热数据归档
 **/
@Slf4j
public class JobInstanceHotDataArchivist extends AbstractJobInstanceArchivist<TaskInstanceRecord> {

    private final TaskInstanceRecordDAO taskInstanceRecordDAO;
    private final StepInstanceRecordDAO stepInstanceRecordDAO;
    private final StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO;
    private final StepInstanceFileRecordDAO stepInstanceFileRecordDAO;
    private final StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO;
    private final StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO;
    private final TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO;
    private final OperationLogRecordDAO operationLogRecordDAO;
    private final FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO;
    private final GseTaskRecordDAO gseTaskRecordDAO;
    private final GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO;
    private final GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO;
    private final GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO;
    private final GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO;
    private final StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO;
    private final RollingConfigRecordDAO rollingConfigRecordDAO;

    public JobInstanceHotDataArchivist(TaskInstanceRecordDAO taskInstanceRecordDAO,
                                       StepInstanceRecordDAO stepInstanceRecordDAO,
                                       StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO,
                                       StepInstanceFileRecordDAO stepInstanceFileRecordDAO,
                                       StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO,
                                       StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO,
                                       TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO,
                                       OperationLogRecordDAO operationLogRecordDAO,
                                       FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO,
                                       GseTaskRecordDAO gseTaskRecordDAO,
                                       GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO,
                                       GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO,
                                       GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO,
                                       GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO,
                                       StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO,
                                       RollingConfigRecordDAO rollingConfigRecordDAO,
                                       JobInstanceHotRecordDAO<TaskInstanceRecord> jobInstanceHotRecordDAO,
                                       JobInstanceColdDAO jobInstanceColdDAO,
                                       ArchiveDBProperties.ArchiveTaskProperties archiveTaskProperties,
                                       ArchiveTaskLock archiveTaskLock,
                                       ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                       JobInstanceArchiveTask archiveTask,
                                       ArchiveTaskService archiveTaskService) {
        super(
            jobInstanceHotRecordDAO,
            jobInstanceColdDAO,
            archiveTaskProperties,
            archiveTaskLock,
            archiveErrorTaskCounter,
            archiveTask,
            archiveTaskService
        );
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.stepInstanceRecordDAO = stepInstanceRecordDAO;
        this.stepInstanceScriptRecordDAO = stepInstanceScriptRecordDAO;
        this.stepInstanceFileRecordDAO = stepInstanceFileRecordDAO;
        this.stepInstanceConfirmRecordDAO = stepInstanceConfirmRecordDAO;
        this.stepInstanceVariableRecordDAO = stepInstanceVariableRecordDAO;
        this.taskInstanceVariableRecordDAO = taskInstanceVariableRecordDAO;
        this.operationLogRecordDAO = operationLogRecordDAO;
        this.fileSourceTaskLogRecordDAO = fileSourceTaskLogRecordDAO;
        this.gseTaskRecordDAO = gseTaskRecordDAO;
        this.gseScriptAgentTaskRecordDAO = gseScriptAgentTaskRecordDAO;
        this.gseFileAgentTaskRecordDAO = gseFileAgentTaskRecordDAO;
        this.gseScriptExecuteObjTaskRecordDAO = gseScriptExecuteObjTaskRecordDAO;
        this.gseFileExecuteObjTaskRecordDAO = gseFileExecuteObjTaskRecordDAO;
        this.stepInstanceRollingTaskRecordDAO = stepInstanceRollingTaskRecordDAO;
        this.rollingConfigRecordDAO = rollingConfigRecordDAO;
    }


    @Override
    protected void backupJobInstanceToColdDb(List<TaskInstanceRecord> jobInstances,
                                             List<Long> jobInstanceIds) {
        jobInstanceColdDAO.batchInsert(jobInstances, 1000);
    }

    @Override
    protected void deleteJobInstanceHotData(List<Long> jobInstanceIds) {

    }

    @Override
    protected List<TaskInstanceRecord> readSortedJobInstanceFromHotDB(Long fromTimestamp,
                                                                      Long endTimestamp,
                                                                      Long fromJobInstanceId,
                                                                      int limit) {
        return taskInstanceRecordDAO.readSortedJobInstanceFromHotDB(
            fromTimestamp,
            endTimestamp,
            fromJobInstanceId,
            limit
        );
    }

    @Override
    protected Long extractJobInstanceId(TaskInstanceRecord record) {
        return record.get(TaskInstance.TASK_INSTANCE.ID);
    }

    @Override
    protected Long extractJobInstanceCreateTime(TaskInstanceRecord record) {
        return record.get(TaskInstance.TASK_INSTANCE.CREATE_TIME);
    }
}
