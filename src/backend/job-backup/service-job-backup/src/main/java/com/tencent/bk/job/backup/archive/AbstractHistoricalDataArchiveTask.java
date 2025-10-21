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

import com.tencent.bk.job.backup.archive.model.ArchiveTaskContext;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskExecutionDetail;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.IdBasedArchiveProcess;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 作业执行历史归档任务的抽象基类，定义了归档任务的公共方法、生命周期
 * 子类必须实现{@link #backupAndDelete()}, {@link #acquireLock()}和{@link #unlock()} 方法
 */
@Slf4j
public abstract class AbstractHistoricalDataArchiveTask implements JobHistoricalDataArchiveTask{
    protected final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    protected final ArchiveTaskService archiveTaskService;
    protected final String taskId;
    protected ArchiveTaskInfo archiveTaskInfo;

    /**
     * 归档进度
     */
    protected final IdBasedArchiveProcess progress;

    protected boolean isAcquireLock;
    /**
     * 任务终止标识
     */
    protected volatile boolean stopFlag = false;
    /**
     * 任务是否已停止
     */
    protected volatile boolean isStopped = false;
    /**
     * 同步锁
     */
    private final Object stopMonitor = new Object();
    protected ArchiveTaskStopCallback stopCallback = null;
    private volatile ArchiveTaskDoneCallback archiveTaskDoneCallback;
    /**
     * 任务标识-是否已被任务调度强制终止
     */
    protected final AtomicBoolean forceStoppedByScheduler = new AtomicBoolean(false);
    /**
     * 当前归档线程
     */
    protected ArchiveTaskWorker archiveTaskWorker;


    protected AbstractHistoricalDataArchiveTask(ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                                ArchiveTaskInfo archiveTaskInfo,
                                                ArchiveTaskService archiveTaskService) {
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.archiveTaskInfo = archiveTaskInfo;
        this.archiveTaskService = archiveTaskService;
        this.taskId = archiveTaskInfo.buildTaskUniqueId();
        this.progress = archiveTaskInfo.getProcess();
    }

    @Override
    public void execute() {
        archive();
    }

    protected void archive() {
        try {
            // 设置归档任务上下文
            ArchiveTaskContextHolder.set(new ArchiveTaskContext(archiveTaskInfo));
            // 获取分布式锁
            if (!acquireLock()) {
                return;
            }
            log.info("{} [{}] Start archive task", getRuntimeClassName(), taskId);
            // 更新任务信息 - 启动完成
            updateStartedExecuteInfo();
            // 归档
            backupAndDelete();
        } catch (Throwable e) {
            String msg = MessageFormatter.format(
                "{} [{}] Error while execute archive task",
                getRuntimeClassName(),
                taskId
            ).getMessage();
            log.error(msg, e);
            archiveErrorTaskCounter.increment();

            // 更新归档任务状态
            setArchiveTaskExecutionDetail(null, null, e.getMessage());
            updateCompletedExecuteInfo(ArchiveTaskStatusEnum.FAIL, null);
        } finally {
            if (this.isAcquireLock) {
                unlock();
            }
            log.info(
                "{} [{}] Archive finished, result: {}",
                getRuntimeClassName(),
                taskId,
                JsonUtils.toJson(archiveTaskInfo)
            );
            if (archiveTaskDoneCallback != null) {
                archiveTaskDoneCallback.callback();
            }
            if (checkStopFlag()) {
                stopTask();
            }
            ArchiveTaskContextHolder.unset();
        }
    }

    @Override
    public void stop(ArchiveTaskStopCallback callback) {
        synchronized (this) {
            log.info("{} [{}] Set stop flag to true", getRuntimeClassName(), taskId);
            this.stopFlag = true;
            this.stopCallback = callback;
        }
    }

    protected boolean checkStopFlag() {
        synchronized (stopMonitor) {
            return stopFlag;
        }
    }

    protected void stopTask() {
        log.info("{} [{}] Try to stop archive task", getRuntimeClassName(), taskId);
        synchronized (stopMonitor) {
            if (!isStopped) {
                isStopped = true;
                // 更新归档任务状态为暂停，用于后续调度
                updateArchiveTaskSuspended();
                if (stopCallback != null) {
                    stopCallback.callback();
                }
                log.info("{} [{}] Stop archive task successfully", getRuntimeClassName(), taskId);
            } else {
                log.info("{} [{}] Archive task is already stopped", getRuntimeClassName(), taskId);
            }
        }
    }

    private void updateArchiveTaskSuspended() {
        archiveTaskInfo.setStatus(ArchiveTaskStatusEnum.SUSPENDED);
        archiveTaskService.updateArchiveTaskStatus(
            archiveTaskInfo.getTaskType(),
            archiveTaskInfo.getDbDataNode(),
            archiveTaskInfo.getDay(),
            archiveTaskInfo.getHour(),
            ArchiveTaskStatusEnum.SUSPENDED
        );
        log.info("{} [{}] Set archive task status suspended", getRuntimeClassName(), taskId);
    }

    @Override
    public void forceStopAtOnce() {
        log.info("{} Force stop archive task at once. taskId: {}", getRuntimeClassName(), taskId);
        forceStoppedByScheduler.set(true);
        // 更新归档任务状态为“暂停”
        updateArchiveTaskSuspended();
        // 打断当前线程，退出执行
        archiveTaskWorker.interrupt();
    }

    @Override
    public void registerDoneCallback(ArchiveTaskDoneCallback archiveTaskDoneCallback) {
        this.archiveTaskDoneCallback = archiveTaskDoneCallback;
    }

    @Override
    public String getTaskId() {
        return this.taskId;
    }

    @Override
    public void initArchiveTaskWorker(ArchiveTaskWorker archiveTaskWorker) {
        this.archiveTaskWorker = archiveTaskWorker;
    }

    protected void updateStartedExecuteInfo() {
        if (!checkUpdateEnabled()) {
            return;
        }
        Long startTime = System.currentTimeMillis();
        archiveTaskInfo.setTaskStartTime(startTime);
        archiveTaskService.updateStartedExecuteInfo(
            archiveTaskInfo.getTaskType(),
            archiveTaskInfo.getDbDataNode(),
            archiveTaskInfo.getDay(),
            archiveTaskInfo.getHour(),
            startTime
        );
        // 如果该任务是失败任务重新调度的，需要重置执行详情错误信息
        resetIfIsReScheduleAbnormalTask();
    }

    private void resetIfIsReScheduleAbnormalTask() {
        if (archiveTaskInfo.getDetail() != null
            && StringUtils.isNotEmpty(archiveTaskInfo.getDetail().getErrorMsg())) {
            archiveTaskInfo.getDetail().setErrorMsg(null);
            archiveTaskService.updateExecutionDetail(
                archiveTaskInfo.getTaskType(),
                archiveTaskInfo.getDbDataNode(),
                archiveTaskInfo.getDay(),
                archiveTaskInfo.getHour(),
                archiveTaskInfo.getDetail()
            );
        }

        // 进度清空，防止因异常跳过的作业实例ID没法被重调度
        if (archiveTaskInfo.getProcess() != null) {
            archiveTaskInfo.setProcess(null);
            this.progress.setId(0L);
        }
    }

    protected void updateCompletedExecuteInfo(ArchiveTaskStatusEnum status,
                                            IdBasedArchiveProcess process) {
        archiveTaskInfo.setStatus(status);
        if (process != null) {
            archiveTaskInfo.setProcess(process);
        }
        archiveTaskInfo.setTaskEndTime(System.currentTimeMillis());
        archiveTaskInfo.setTaskCost(archiveTaskInfo.getTaskEndTime() - archiveTaskInfo.getTaskStartTime());

        if (!checkUpdateEnabled()) {
            return;
        }

        log.info("{} [{}] Update archive task completed execute info, status: {}, process: {}",
            getRuntimeClassName(), taskId, status, process);
        archiveTaskService.updateCompletedExecuteInfo(
            archiveTaskInfo.getTaskType(),
            archiveTaskInfo.getDbDataNode(),
            archiveTaskInfo.getDay(),
            archiveTaskInfo.getHour(),
            archiveTaskInfo.getStatus(),
            archiveTaskInfo.getProcess(),
            archiveTaskInfo.getTaskEndTime(),
            archiveTaskInfo.getTaskCost(),
            archiveTaskInfo.getDetail()
        );
    }

    protected void setArchiveTaskExecutionDetail(Long archiveRecordSize,
                                               Long costTime,
                                               String errorMsg) {
        ArchiveTaskExecutionDetail executionDetail = archiveTaskInfo.getOrInitExecutionDetail();
        if (archiveRecordSize != null) {
            executionDetail.setArchivedRecordSize(archiveRecordSize);
        }
        if (costTime != null) {
            executionDetail.setCostTime(costTime);
        }
        if (StringUtils.isNotEmpty(errorMsg)) {
            executionDetail.setErrorMsg(StringUtil.substring(errorMsg, 10240));
        }
    }

    protected boolean checkUpdateEnabled() {
        if (forceStoppedByScheduler.get()) {
            // 如果已经被归档任务调度强制终止了，就不能再去更新 db，引起数据不一致
            log.info("{} [{}] Archive task is force stopped by scheduler, do not update archive task again",
                getRuntimeClassName(), taskId);
            return false;
        } else {
            return true;
        }
    }

    /**
     * 获取运行时的类名
     */
    protected String getRuntimeClassName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 归档操作
     */
    protected abstract void backupAndDelete();

    /**
     * 获取锁
     */
    protected abstract boolean acquireLock();

    /**
     * 释放锁
     */
    protected  abstract void unlock();
}
