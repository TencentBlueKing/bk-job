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

package com.tencent.bk.job.execute.engine.prepare;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.result.ContinuousScheduledTask;
import com.tencent.bk.job.execute.engine.result.ScheduleStrategy;
import com.tencent.bk.job.execute.engine.result.StopTaskCounter;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.TaskInstanceService;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class FilePrepareControlTask implements ContinuousScheduledTask {

    /**
     * 同步锁
     */
    private final Object stopMonitor = new Object();
    /**
     * 任务是否已停止
     */
    private volatile boolean isStopped = false;
    volatile AtomicBoolean isDoneWrapper = new AtomicBoolean(false);
    private final FilePrepareService filePrepareService;
    private final TaskInstanceService taskInstanceService;
    private final StepInstanceDTO stepInstance;
    private final CountDownLatch latch;
    private final List<FilePrepareTaskResult> resultList;
    private final FilePrepareTaskResultHandler filePrepareTaskResultHandler;
    private final long startTimeMills;
    // 默认30分钟
    private long timeoutMills = 30 * 60 * 1000L;

    public FilePrepareControlTask(
        FilePrepareService filePrepareService,
        TaskInstanceService taskInstanceService,
        StepInstanceDTO stepInstance,
        CountDownLatch latch,
        List<FilePrepareTaskResult> resultList,
        FilePrepareTaskResultHandler filePrepareTaskResultHandler
    ) {
        this.startTimeMills = System.currentTimeMillis();
        this.filePrepareService = filePrepareService;
        this.taskInstanceService = taskInstanceService;
        this.stepInstance = stepInstance;
        this.latch = latch;
        this.resultList = resultList;
        this.filePrepareTaskResultHandler = filePrepareTaskResultHandler;
    }

    public void setDoneStatus() {
        isDoneWrapper.set(true);
    }

    public void setTimeoutMills(long timeoutMills) {
        this.timeoutMills = timeoutMills;
    }

    @Override
    public boolean isFinished() {
        return isDoneWrapper.get();
    }

    @Override
    public ScheduleStrategy getScheduleStrategy() {
        // 每秒检查一次是否需要停止
        return () -> 1000;
    }

    @Override
    public void stop() {
        synchronized (stopMonitor) {
            if (!isStopped) {
                StopTaskCounter.getInstance().decrement(getTaskId());
                this.isStopped = true;
            }
        }
    }

    private boolean needToStop(StepInstanceDTO stepInstance) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        // 刷新步骤状态
        stepInstance = taskInstanceService.getStepInstanceDetail(stepInstance.getId());
        // 如果任务处于“终止中”状态，触发任务终止
        if (taskInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue())) {
            // 已经发送过停止命令的就不再重复发送了
            return !RunStatusEnum.STOPPING.getValue().equals(stepInstance.getStatus())
                && !RunStatusEnum.STOP_SUCCESS.getValue().equals(stepInstance.getStatus());
        }
        return false;
    }

    private void doExecute() {
        // 强制终止检测
        if (needToStop(stepInstance)) {
            filePrepareService.stopPrepareFile(stepInstance.getId());
            setDoneStatus();
        }
        // 文件准备任务进度监控
        if (latch.getCount() == 0) {
            filePrepareTaskResultHandler.onFinished(stepInstance, resultList);
            setDoneStatus();
        }
        // 超时判定
        if (System.currentTimeMillis() - startTimeMills >= timeoutMills) {
            filePrepareTaskResultHandler.onTimeout(stepInstance);
            setDoneStatus();
        }
    }

    @Override
    public void execute() {
        try {
            doExecute();
        } catch (Throwable t) {
            filePrepareTaskResultHandler.onException(stepInstance, t);
            setDoneStatus();
        }
    }

    @Override
    public String getTaskId() {
        return "FilePrepareControlTask-" + stepInstance.getId() + "_" + stepInstance.getExecuteCount();
    }
}
