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

import com.tencent.bk.job.backup.archive.service.detector.UnarchivedTaskInstanceDetector;
import com.tencent.bk.job.backup.archive.util.lock.CheckTaskLaunchLock;
import lombok.extern.slf4j.Slf4j;

/**
 * 检查历史数据归档情况的任务启动器，查看是否存在未归档完全的数据
 */
@Slf4j
public class HistoricalDataCheckTaskLauncher {

    private final CheckTaskLaunchLock checkTaskLaunchLock;
    private final UnarchivedTaskInstanceDetector unarchivedTaskInstanceDetector;

    public HistoricalDataCheckTaskLauncher(
        CheckTaskLaunchLock checkTaskLaunchLock,
        UnarchivedTaskInstanceDetector unarchivedTaskInstanceDetector
    ) {
        this.checkTaskLaunchLock = checkTaskLaunchLock;
        this.unarchivedTaskInstanceDetector = unarchivedTaskInstanceDetector;
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
            DetectTask task = new DetectTask(unarchivedTaskInstanceDetector);
            task.run();
        } finally {
            if (locked) {
                checkTaskLaunchLock.unlock();
            }
        }

    }

    public static class DetectTask {
        private final UnarchivedTaskInstanceDetector unarchivedTaskInstanceDetector;

        public DetectTask(UnarchivedTaskInstanceDetector unarchivedDataDetector) {
            this.unarchivedTaskInstanceDetector = unarchivedDataDetector;
        }

        public void run() {
            unarchivedTaskInstanceDetector.detect();
        }
    }

}
