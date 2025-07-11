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

package com.tencent.bk.job.execute.engine.prepare;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.result.ContinuousScheduledTask;
import com.tencent.bk.job.execute.engine.result.ScheduleStrategy;
import com.tencent.bk.job.execute.engine.result.StopTaskCounter;
import com.tencent.bk.job.execute.engine.result.TaskContext;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class FilePrepareControlTask implements ContinuousScheduledTask {

    /**
     * 同步锁
     */
    private final Object stopMonitor = new Object();
    /**
     * 任务是否已停止
     */
    private volatile boolean isStopped = false;
    private volatile boolean isRunning = false;
    volatile AtomicBoolean isDoneWrapper = new AtomicBoolean(false);
    private final FilePrepareService filePrepareService;
    private final TaskInstanceService taskInstanceService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final StepInstanceDTO stepInstance;
    private final CountDownLatch latch;
    private final List<FilePrepareTaskResult> resultList;
    private final FilePrepareTaskResultHandler filePrepareTaskResultHandler;
    private final StepInstanceService stepInstanceService;
    private final long startTimeMills;
    private final TaskContext taskContext;

    public FilePrepareControlTask(
        FilePrepareService filePrepareService,
        TaskInstanceService taskInstanceService,
        TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
        StepInstanceDTO stepInstance,
        CountDownLatch latch,
        List<FilePrepareTaskResult> resultList,
        FilePrepareTaskResultHandler filePrepareTaskResultHandler,
        StepInstanceService stepInstanceService) {
        this.filePrepareService = filePrepareService;
        this.taskInstanceService = taskInstanceService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.stepInstance = stepInstance;
        this.latch = latch;
        this.resultList = resultList;
        this.filePrepareTaskResultHandler = filePrepareTaskResultHandler;
        this.stepInstanceService = stepInstanceService;
        this.startTimeMills = System.currentTimeMillis();
        this.taskContext = new TaskContext(stepInstance.getTaskInstanceId());
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
    public void execute() {
        try {
            isRunning = true;
            doExecute();
        } catch (Throwable t) {
            filePrepareTaskResultHandler.onException(stepInstance, t);
            setDoneStatus();
        } finally {
            isRunning = false;
        }
    }

    private void doExecute() {
        // 强制终止检测
        if (needToStop(stepInstance)) {
            filePrepareService.stopPrepareFile(stepInstance);
            setDoneStatus();
        }
        // 文件准备任务进度监控
        if (latch.getCount() == 0) {
            filePrepareTaskResultHandler.onFinished(stepInstance, resultList);
            setDoneStatus();
        }
        // 超时判定
        // 默认30分钟
        long timeoutMills = 30 * 60 * 1000L;
        if (System.currentTimeMillis() - startTimeMills >= timeoutMills) {
            filePrepareTaskResultHandler.onTimeout(stepInstance);
            setDoneStatus();
        }
    }

    private boolean needToStop(StepInstanceDTO stepInstance) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        // 刷新步骤状态
        stepInstance = stepInstanceService.getStepInstanceDetail(stepInstance.getId());
        // 如果任务处于“终止中”状态，触发任务终止
        if (taskInstance.getStatus() == RunStatusEnum.STOPPING) {
            // 已经发送过停止命令的就不再重复发送了
            return !(RunStatusEnum.STOPPING == stepInstance.getStatus()
                || RunStatusEnum.STOP_SUCCESS == stepInstance.getStatus());
        }
        return false;
    }

    public void setDoneStatus() {
        isDoneWrapper.set(true);
    }

    @Override
    public void stop() {
        synchronized (stopMonitor) {
            if (!isStopped) {
                gracefulStop();
            } else {
                log.info("{} already stopped, ignore", getTaskId());
            }
        }
    }

    private void gracefulStop() {
        if (!this.isRunning) {
            log.info("gracefulStop begin:{}", getTaskId());
            // 1.停止正在进行的所有文件准备任务
            filePrepareService.stopPrepareFile(stepInstance);
            // 2.MQ消息通知其他实例准备文件
            taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.prepareFile(stepInstance.getId()));
            this.isStopped = true;
            StopTaskCounter.getInstance().decrement(getTaskId());
            log.info("gracefulStop end:{}", getTaskId());
        } else {
            log.info("{} is running, wait for next schedule to gracefulStop", getTaskId());
        }
    }

    @Override
    public String getTaskId() {
        return "FilePrepareControlTask-" + stepInstance.getId() + "_" + stepInstance.getExecuteCount();
    }

    @Override
    public TaskContext getTaskContext() {
        return taskContext;
    }

    @Override
    public String toString() {
        return getTaskId();
    }
}
