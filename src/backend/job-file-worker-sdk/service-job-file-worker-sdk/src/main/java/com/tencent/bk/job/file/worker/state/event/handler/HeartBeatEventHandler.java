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

import com.tencent.bk.job.file.worker.state.WorkerStateEnum;
import com.tencent.bk.job.file.worker.state.WorkerStateMachine;
import com.tencent.bk.job.file.worker.state.event.WorkerEvent;
import com.tencent.bk.job.file.worker.state.event.WorkerEventService;
import com.tencent.bk.job.file.worker.task.heartbeat.HeartBeatTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 心跳事件处理器，用于向File-Gateway上报Worker状态信息
 */
@Slf4j
@Component
public class HeartBeatEventHandler implements EventHandler {

    private final WorkerEventService workerEventService;
    private final WorkerStateMachine workerStateMachine;
    private final HeartBeatTask heartBeatTask;

    @Autowired
    public HeartBeatEventHandler(@Lazy WorkerEventService workerEventService,
                                 WorkerStateMachine workerStateMachine,
                                 HeartBeatTask heartBeatTask) {
        this.workerEventService = workerEventService;
        this.workerStateMachine = workerStateMachine;
        this.heartBeatTask = heartBeatTask;
    }

    @Override
    public void handleEvent(WorkerEvent event) {
        WorkerStateEnum workerState = workerStateMachine.getWorkerState();
        switch (workerState) {
            case STARTING:
                workerEventService.commitWorkerEvent(WorkerEvent.waitAccessReady());
                break;
            case WAIT_ACCESS_READY:
                log.info("wait access ready, ignore current event:{}", event);
                break;
            case HEART_BEAT_WAIT:
            case RUNNING:
                heartBeat();
                break;
            default:
                log.info("currentState:{}, heartBeat condition not satisfy, ignore", workerState);
                break;
        }
    }

    private Long lastSuccessHeartBeatTime = null;

    private void heartBeat() {
        workerStateMachine.heartBeatStart();
        try {
            // 如果上一次成功的心跳在10s内发生，则忽略本次心跳
            if (lastSuccessHeartBeatTime != null && System.currentTimeMillis() - lastSuccessHeartBeatTime < 10_000L) {
                log.info("lastSuccessHeartBeat finish with 10s, ignore current heartBeat");
                workerStateMachine.heartBeatSuccess();
            } else {
                heartBeatTask.doHeartBeat();
                workerStateMachine.heartBeatSuccess();
                lastSuccessHeartBeatTime = System.currentTimeMillis();
            }
        } catch (Throwable t) {
            log.warn("Fail to heartBeat", t);
            workerStateMachine.heartBeatFailed();
        }
    }
}
