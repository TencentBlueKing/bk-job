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

import com.tencent.bk.job.backup.archive.impl.FileSourceTaskLogArchiver;
import com.tencent.bk.job.backup.archive.impl.GseFileAgentTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseFileExecuteObjTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseScriptAgentTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseScriptExecuteObjTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.OperationLogArchiver;
import com.tencent.bk.job.backup.archive.impl.RollingConfigArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceConfirmArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceFileArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceRollingTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceScriptArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceVariableArchiver;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceHostArchiver;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceVariableArchiver;

import java.util.ArrayList;
import java.util.List;

public class JobInstanceSubTableArchivers {

    private final List<JobInstanceSubTableArchiver> subTableArchivers = new ArrayList<>();

    public JobInstanceSubTableArchivers(FileSourceTaskLogArchiver fileSourceTaskLogArchiver,
                                        GseFileAgentTaskArchiver gseFileAgentTaskArchiver,
                                        GseFileExecuteObjTaskArchiver gseFileExecuteObjTaskArchiver,
                                        GseScriptAgentTaskArchiver gseScriptAgentTaskArchiver,
                                        GseScriptExecuteObjTaskArchiver gseScriptExecuteObjTaskArchiver,
                                        GseTaskArchiver gseTaskArchiver,
                                        OperationLogArchiver operationLogArchiver,
                                        RollingConfigArchiver rollingConfigArchiver,
                                        StepInstanceArchiver stepInstanceArchiver,
                                        StepInstanceConfirmArchiver stepInstanceConfirmArchiver,
                                        StepInstanceFileArchiver stepInstanceFileArchiver,
                                        StepInstanceScriptArchiver stepInstanceScriptArchiver,
                                        StepInstanceRollingTaskArchiver stepInstanceRollingTaskArchiver,
                                        StepInstanceVariableArchiver stepInstanceVariableArchiver,
                                        TaskInstanceHostArchiver taskInstanceHostArchiver,
                                        TaskInstanceVariableArchiver taskInstanceVariableArchiver) {
        this.subTableArchivers.add(fileSourceTaskLogArchiver);
        this.subTableArchivers.add(gseFileAgentTaskArchiver);
        this.subTableArchivers.add(gseFileExecuteObjTaskArchiver);
        this.subTableArchivers.add(gseScriptAgentTaskArchiver);
        this.subTableArchivers.add(gseScriptExecuteObjTaskArchiver);
        this.subTableArchivers.add(gseTaskArchiver);
        this.subTableArchivers.add(operationLogArchiver);
        this.subTableArchivers.add(rollingConfigArchiver);
        this.subTableArchivers.add(stepInstanceArchiver);
        this.subTableArchivers.add(stepInstanceConfirmArchiver);
        this.subTableArchivers.add(stepInstanceFileArchiver);
        this.subTableArchivers.add(stepInstanceScriptArchiver);
        this.subTableArchivers.add(stepInstanceRollingTaskArchiver);
        this.subTableArchivers.add(stepInstanceVariableArchiver);
        this.subTableArchivers.add(taskInstanceHostArchiver);
        this.subTableArchivers.add(taskInstanceVariableArchiver);
    }

    public List<JobInstanceSubTableArchiver> getAll() {
        return this.subTableArchivers;
    }
}
