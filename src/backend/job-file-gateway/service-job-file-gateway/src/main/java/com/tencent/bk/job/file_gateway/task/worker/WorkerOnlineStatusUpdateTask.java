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

package com.tencent.bk.job.file_gateway.task.worker;

import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkerOnlineStatusUpdateTask {

    private static final long WORKER_HEART_BEAT_EXPIRE_TIME_MILLS = 150 * 1000L;
    private final FileWorkerDAO fileWorkerDAO;

    @Autowired
    public WorkerOnlineStatusUpdateTask(FileWorkerDAO fileWorkerDAO) {
        this.fileWorkerDAO = fileWorkerDAO;
    }

    public void run() {
        // 150s内没有心跳就判定为离线
        int affectedRowNum = fileWorkerDAO.updateAllFileWorkerOnlineStatus(
            0,
            System.currentTimeMillis() - WORKER_HEART_BEAT_EXPIRE_TIME_MILLS
        );
        if (affectedRowNum > 0) {
            log.info("{} workers offline", affectedRowNum);
        }
    }
}
