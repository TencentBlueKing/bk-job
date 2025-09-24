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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.archive.service.detector.UnarchivedDataDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedFileSourceTaskLogDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedGseFileExecuteObjectTaskDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedGseScriptExecuteObjTaskDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedGseTaskDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedOperationLogDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedRollingConfigDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceConfirmDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceFileDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceScriptDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceVariableDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedTaskInstanceDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedTaskInstanceHostDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedTaskInstanceVariableDetector;
import com.tencent.bk.job.backup.archive.util.lock.CheckTaskLaunchLock;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class HistoricalDataCheckTaskLauncher {

    private final CheckTaskLaunchLock checkTaskLaunchLock;
    private final List<UnarchivedDataDetector> unarchivedDataDetectors;

    public HistoricalDataCheckTaskLauncher(
        CheckTaskLaunchLock checkTaskLaunchLock,
        UnarchivedFileSourceTaskLogDetector unarchivedFileSourceTaskLogDetector,
        UnarchivedGseFileExecuteObjectTaskDetector unarchivedGseFileExecuteObjectTaskDetector,
        UnarchivedGseScriptExecuteObjTaskDetector unarchivedGseScriptExecuteObjTaskDetector,
        UnarchivedGseTaskDetector unarchivedGseTaskDetector,
        UnarchivedOperationLogDetector unarchivedOperationLogDetector,
        UnarchivedRollingConfigDetector unarchivedRollingConfigDetector,
        UnarchivedStepInstanceConfirmDetector unarchivedStepInstanceConfirmDetector,
        UnarchivedStepInstanceDetector unarchivedStepInstanceDetector,
        UnarchivedStepInstanceFileDetector unarchivedStepInstanceFileDetector,
        UnarchivedStepInstanceScriptDetector unarchivedStepInstanceScriptDetector,
        UnarchivedStepInstanceVariableDetector unarchivedStepInstanceVariableDetector,
        UnarchivedTaskInstanceDetector unarchivedTaskInstanceDetector,
        UnarchivedTaskInstanceHostDetector unarchivedTaskInstanceHostDetector,
        UnarchivedTaskInstanceVariableDetector unarchivedTaskInstanceVariableDetector
    ) {
        this.checkTaskLaunchLock = checkTaskLaunchLock;
        this.unarchivedDataDetectors = Arrays.asList(
            unarchivedFileSourceTaskLogDetector,
            unarchivedGseFileExecuteObjectTaskDetector,
            unarchivedGseScriptExecuteObjTaskDetector,
            unarchivedGseTaskDetector,
            unarchivedOperationLogDetector,
            unarchivedRollingConfigDetector,
            unarchivedStepInstanceConfirmDetector,
            unarchivedStepInstanceDetector,
            unarchivedStepInstanceFileDetector,
            unarchivedStepInstanceScriptDetector,
            unarchivedStepInstanceVariableDetector,
            unarchivedTaskInstanceDetector,
            unarchivedTaskInstanceHostDetector,
            unarchivedTaskInstanceVariableDetector
        );
    }

    public void launchCheckTask() {
        boolean locked = false;
        try {
            locked = checkTaskLaunchLock.lock();
            if (!locked) {
                log.info("Failed to lock for launching check task, action: skip");
                return;
            }

            log.info("Launching check task");
            DetectTask task = new DetectTask(unarchivedDataDetectors);
            task.run();
        } finally {
            if (locked) {
                checkTaskLaunchLock.unlock();
            }
        }

    }

    public static class DetectTask {
        private List<UnarchivedDataDetector> unarchivedDataDetectors;

        public DetectTask(List<UnarchivedDataDetector> unarchivedDataDetectors) {
            this.unarchivedDataDetectors = unarchivedDataDetectors;
        }

        public void run() {
            for (UnarchivedDataDetector unarchivedDataDetector : unarchivedDataDetectors) {
                unarchivedDataDetector.detect();
            }
        }
    }

}
