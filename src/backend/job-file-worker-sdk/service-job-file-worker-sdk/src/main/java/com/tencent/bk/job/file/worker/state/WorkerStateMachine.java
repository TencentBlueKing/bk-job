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

package com.tencent.bk.job.file.worker.state;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;

/**
 * File-Worker状态机，管理Worker状态流转
 */
@Getter
@Slf4j
@Component
public class WorkerStateMachine {

    private WorkerStateEnum workerState = WorkerStateEnum.STARTING;

    @Autowired
    public WorkerStateMachine(Tracer tracer) {
    }

    public void setWorkerState(WorkerStateEnum workerState) {
        log.info("state change: {} -> {}", this.workerState.name(), workerState.name());
        this.workerState = workerState;
    }

    public void waitAccessReady() {
        setWorkerState(WorkerStateEnum.WAIT_ACCESS_READY);
    }

    public void accessReady() {
        setWorkerState(WorkerStateEnum.HEART_BEAT_WAIT);
    }

    public void heartBeatStart() {
        setWorkerState(WorkerStateEnum.HEART_BEATING);
    }

    public void heartBeatSuccess() {
        setWorkerState(WorkerStateEnum.RUNNING);
    }

    public void heartBeatFailed() {
        setWorkerState(WorkerStateEnum.HEART_BEAT_WAIT);
    }

    public void offlineStart() {
        setWorkerState(WorkerStateEnum.OFFLINE_ING);
    }

    public void offlineFailed() {
        setWorkerState(WorkerStateEnum.OFFLINE_FAILED);
    }

    public void offlineSuccess() {
        setWorkerState(WorkerStateEnum.OFFLINE);
    }

    public boolean isWorkerOffLineIncludeFail() {
        return workerState == WorkerStateEnum.OFFLINE || workerState == WorkerStateEnum.OFFLINE_FAILED;
    }
}
