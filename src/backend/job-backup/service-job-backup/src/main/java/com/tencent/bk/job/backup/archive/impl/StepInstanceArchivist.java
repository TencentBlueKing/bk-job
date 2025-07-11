/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
import com.tencent.bk.job.backup.archive.ArchiveTaskLock;
import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import com.tencent.bk.job.backup.dao.impl.StepInstanceRecordDAO;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.backup.service.ArchiveProgressService;
import com.tencent.bk.job.execute.model.tables.records.StepInstanceRecord;

import java.util.concurrent.CountDownLatch;

/**
 * step_instance 表归档
 */
public class StepInstanceArchivist extends AbstractArchivist<StepInstanceRecord> {

    public StepInstanceArchivist(StepInstanceRecordDAO executeRecordDAO,
                                 ExecuteArchiveDAO executeArchiveDAO,
                                 ArchiveProgressService archiveProgressService,
                                 ArchiveDBProperties archiveDBProperties,
                                 ArchiveTaskLock archiveTaskLock,
                                 Long maxNeedArchiveId,
                                 CountDownLatch countDownLatch,
                                 ArchiveErrorTaskCounter archiveErrorTaskCounter) {
        super(executeRecordDAO,
            executeArchiveDAO,
            archiveProgressService,
            archiveDBProperties,
            archiveTaskLock,
            maxNeedArchiveId,
            countDownLatch,
            archiveErrorTaskCounter);
        this.deleteIdStepSize = 10_000;
    }
}
