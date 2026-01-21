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

package com.tencent.bk.job.file.worker.config;

import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.file.worker.service.OpService;
import com.tencent.bk.job.file.worker.state.WorkerStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GracefulShutdown implements ApplicationListener<ContextClosedEvent> {

    private final OpService opService;
    private final WorkerStateMachine workerStateMachine;

    @Value("${app.shutdownTimeout:30}")
    int shutdownTimeout = 30;

    @Autowired
    public GracefulShutdown(OpService opService, WorkerStateMachine workerStateMachine) {
        this.opService = opService;
        this.workerStateMachine = workerStateMachine;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("Close event listened, event:{}", event);
        List<String> runningTaskIdList = opService.offLine();
        log.info("worker apply to offLine, {} tasks to be reDispatched are {}", runningTaskIdList.size(),
            runningTaskIdList);
        long waitStart = System.currentTimeMillis();
        long maxWaitMills = 5000;
        long waitMills;
        do {
            // 1.等待Worker主动下线完成
            ThreadUtils.sleep(100);
            waitMills = System.currentTimeMillis() - waitStart;
        } while (!workerStateMachine.isWorkerOffLineIncludeFail() && waitMills < maxWaitMills);
        // 2.等待File-Gateway内存中存量已调度请求完成
        ThreadUtils.sleep(3000);
        log.info("Worker offLine done");
    }
}
