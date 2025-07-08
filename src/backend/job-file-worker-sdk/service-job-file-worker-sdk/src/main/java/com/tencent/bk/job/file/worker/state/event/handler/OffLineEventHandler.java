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

package com.tencent.bk.job.file.worker.state.event.handler;

import com.tencent.bk.job.file.worker.service.OpService;
import com.tencent.bk.job.file.worker.state.WorkerStateEnum;
import com.tencent.bk.job.file.worker.state.WorkerStateMachine;
import com.tencent.bk.job.file.worker.state.event.WorkerEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 下线事件处理器
 */
@Slf4j
@Component
public class OffLineEventHandler implements EventHandler {

    private final WorkerStateMachine workerStateMachine;
    private final OpService opService;

    @Autowired
    public OffLineEventHandler(WorkerStateMachine workerStateMachine,
                               @Lazy OpService opService) {
        this.workerStateMachine = workerStateMachine;
        this.opService = opService;
    }

    @Override
    public void handleEvent(WorkerEvent event) {
        WorkerStateEnum workerState = workerStateMachine.getWorkerState();
        switch (workerState) {
            case RUNNING:
            case HEART_BEAT_WAIT:
            case OFFLINE_FAILED:
                offLine();
                break;
            case OFFLINE_ING:
                log.info("last offLine action is executing, ignore current one");
                break;
            default:
                log.info("currentState:{}, offLine condition not satisfy, ignore", workerState);
                break;
        }
    }

    private void offLine() {
        workerStateMachine.offlineStart();
        try {
            opService.doOffLine();
            workerStateMachine.offlineSuccess();
        } catch (Throwable t) {
            log.warn("Fail to offLine", t);
            workerStateMachine.offlineFailed();
        }
    }
}
