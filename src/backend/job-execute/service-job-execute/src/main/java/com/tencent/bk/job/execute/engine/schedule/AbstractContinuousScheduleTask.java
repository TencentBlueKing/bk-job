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

package com.tencent.bk.job.execute.engine.schedule;

import com.tencent.bk.job.execute.engine.schedule.ha.ScheduleTaskKeepaliveManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 持续调度任务抽象实现
 */
@Slf4j
public abstract class AbstractContinuousScheduleTask implements ContinuousScheduleTask {

    private final ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager;

    /**
     * 同步锁
     */
    private final Object stopMonitor = new Object();
    /**
     * 任务是否在运行中
     */
    private volatile boolean isRunning = false;
    /**
     * 任务是否已停止
     */
    private volatile boolean isStopped = false;
    /**
     * 任务是否启用
     */
    private volatile boolean isActive = true;

    public AbstractContinuousScheduleTask(ScheduleTaskKeepaliveManager scheduleTaskKeepaliveManager) {
        this.scheduleTaskKeepaliveManager = scheduleTaskKeepaliveManager;
    }

    @Override
    public final void execute() {
        boolean isLockGotten = false;
        try {
            if (!checkTaskActiveAndSetRunningStatus()) {
                return;
            }
            if (checkEvict()) {
                return;
            }
            isLockGotten = acquireTaskLock();
            if (!isLockGotten) {
                log.error("[{}] Get task lock failed, skip task!", getTaskId());
                return;
            }
            executeTask();
        } finally {
            this.isRunning = false;
            if (isLockGotten) {
                releaseTaskLock();
            }
        }
    }

    @Override
    public void stop() {
        synchronized (stopMonitor) {
            if (!isStopped) {
                this.isActive = false;
                tryStopImmediately();
            } else {
                log.info("[{}] Task is stopped", getTaskId());
            }
        }
    }

    private void tryStopImmediately() {
        if (!this.isRunning) {
            log.info("[{}] ScheduleTask-onStop start", getTaskId());
            scheduleTaskKeepaliveManager.stopKeepaliveInfoTask(getTaskId());
            resumeTask();
            this.isStopped = true;
            StopTaskCounter.getInstance().decrement(getTaskId());
            log.info("[{}] ScheduleTask-onStop end", getTaskId());
        } else {
            log.info("[{}] ScheduleTask-onStop, task is running now, will stop when idle", getTaskId());
        }
    }

    private boolean checkTaskActiveAndSetRunningStatus() {
        if (!isActive) {
            log.info("Task is inactive, task: {}", getTaskId());
            return false;
        }
        this.isRunning = true;
        // 二次确认，防止isActive在设置this.isRunning=true期间发生变化
        if (!isActive) {
            log.info("Task is inactive, task: {}", getTaskId());
            return false;
        }
        return true;
    }

    /**
     * 执行任务
     */
    protected abstract void executeTask();

    /**
     * 判断任务是否需要被驱逐
     *
     * @return true: 任务需要被驱逐；false：任务正常执行
     */
    protected abstract boolean checkEvict();

    /**
     * 获取任务执行锁，独占任务
     *
     * @return true: 获取锁成功
     */
    protected abstract boolean acquireTaskLock();

    /**
     * 释放任务执行锁
     */
    protected abstract void releaseTaskLock();

    /**
     * 任务恢复（服务关闭或者任务异常终止场景下）
     */
    protected abstract void resumeTask();
}
