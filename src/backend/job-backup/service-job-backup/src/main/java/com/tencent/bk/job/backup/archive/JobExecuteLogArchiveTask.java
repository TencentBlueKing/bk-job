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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.archive.model.ArchiveTaskContext;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskExecutionDetail;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.IdBasedArchiveProcess;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveLogTaskExecuteLock;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 作业执行日志归档任务实现
 */
@Slf4j
public class JobExecuteLogArchiveTask implements JobHistoricalDataArchiveTask {

    protected final ArchiveProperties archiveProperties;
    private final ArchiveLogTaskExecuteLock archiveLogTaskExecuteLock;
    protected final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    protected final ArchiveTaskService archiveTaskService;
    private String taskId;
    private ArchiveTaskInfo archiveTaskInfo;
    private final JobExecuteLogArchivers jobExecuteLogArchivers;

    /**
     * 归档进度
     */
    private final IdBasedArchiveProcess progress;

    private boolean isAcquireLock;
    /**
     * 任务是否已停止
     */
    private volatile boolean isStopped = false;
    /**
     * 任务终止标识
     */
    private volatile boolean stopFlag = false;
    /**
     * 同步锁
     */
    private final Object stopMonitor = new Object();
    private volatile ArchiveTaskStopCallback stopCallback = null;
    private volatile ArchiveTaskDoneCallback archiveTaskDoneCallback;
    /**
     * 任务标识-是否已被任务调度强制终止
     */
    private final AtomicBoolean forceStoppedByScheduler = new AtomicBoolean(false);
    /**
     * 当前归档线程
     */
    private ArchiveTaskWorker archiveTaskWorker;


    public JobExecuteLogArchiveTask(ArchiveProperties archiveProperties,
                                    ArchiveLogTaskExecuteLock archiveLogTaskExecuteLock,
                                    ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                    ArchiveTaskInfo archiveTaskInfo,
                                    ArchiveTaskService archiveTaskService,
                                    JobExecuteLogArchivers jobExecuteLogArchivers) {
        this.archiveProperties = archiveProperties;
        this.archiveLogTaskExecuteLock = archiveLogTaskExecuteLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.archiveTaskInfo = archiveTaskInfo;
        this.archiveTaskService = archiveTaskService;
        this.progress = archiveTaskInfo.getProcess();
        this.jobExecuteLogArchivers = jobExecuteLogArchivers;
        this.taskId = archiveTaskInfo.buildTaskUniqueId();
    }

    @Override
    public void execute() {
        archive();
    }

    @Override
    public void stop(ArchiveTaskStopCallback stopCallback) {
        synchronized (stopMonitor) {
            log.info("[{}] Set stop flag to true", taskId);
            this.stopFlag = true;
            this.stopCallback = stopCallback;
        }
    }

    private boolean checkStopFlag() {
        synchronized (stopMonitor) {
            return stopFlag;
        }
    }

    private void stopTask() {
        log.info("[{}] Try to stop archive log task", taskId);
        synchronized (stopMonitor) {
            if (!isStopped) {
                isStopped = true;
                // 更新归档任务状态为暂停，用于后续调度
                updateArchiveTaskSuspended();
                if (stopCallback != null) {
                    stopCallback.callback();
                }
                log.info("[{}] Stop archive log task successfully", taskId);
            } else {
                log.info("[{}] Archive task log is already stopped", taskId);
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
        log.info("[{}] Set archive log task status suspended", taskId);
    }

    @Override
    public void forceStopAtOnce() {
        log.info("Force stop archive log task at once. taskId: {}", taskId);
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

    private void archive() {
        try {
            ArchiveTaskContextHolder.set(new ArchiveTaskContext(archiveTaskInfo));
            if (!acquireLock()) {
                return;
            }
            log.info("[{}] Start archive log task", taskId);
            updateStartedExecuteInfo();
            backupAndDelete();
        } catch (Throwable e) {
            String msg = MessageFormatter.format(
                "[{}] Error while execute archive log task",
                taskId
            ).getMessage();
            log.error(msg, e);
            archiveErrorTaskCounter.increment();
            // 更新归档任务状态
            setArchiveTaskExecutionDetail(null, null, e.getMessage());
            updateCompletedExecuteInfo(ArchiveTaskStatusEnum.FAIL, null);
        } finally {
            if (this.isAcquireLock) {
                archiveLogTaskExecuteLock.unlock(taskId);
            }
            log.info(
                "[{}] Archive log finished, result: {}",
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

    private void backupAndDelete() {
        long startTime = System.currentTimeMillis();
        log.info("[{}] archive log task mode: {}", taskId, archiveProperties.getMode());
        // 备份、删除
        jobExecuteLogArchivers.getAll().forEach(archiver -> {
            archiver.backupRecords(archiveTaskInfo.getDay());
            archiver.deleteRecords(archiveTaskInfo.getDay());
        });

        long archiveCost = System.currentTimeMillis() - startTime;
        setArchiveTaskExecutionDetail(null, archiveCost, null);
        updateCompletedExecuteInfo(ArchiveTaskStatusEnum.SUCCESS, progress);
    }

    private void setArchiveTaskExecutionDetail(Long archiveRecordSize,
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

    private boolean acquireLock() {
        this.isAcquireLock = archiveLogTaskExecuteLock.lock(taskId);
        if (!isAcquireLock) {
            log.info("[{}] Acquire archive log task lock fail", taskId);
        }
        return isAcquireLock;
    }


    private void updateStartedExecuteInfo() {
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
        if (archiveTaskInfo.getDetail() != null && StringUtils.isNotEmpty(archiveTaskInfo.getDetail().getErrorMsg())) {
            archiveTaskInfo.getDetail().setErrorMsg(null);
            archiveTaskService.updateExecutionDetail(
                archiveTaskInfo.getTaskType(),
                archiveTaskInfo.getDbDataNode(),
                archiveTaskInfo.getDay(),
                archiveTaskInfo.getHour(),
                archiveTaskInfo.getDetail()
            );
        }
    }

    private boolean checkUpdateEnabled() {
        if (forceStoppedByScheduler.get()) {
            // 如果已经被归档任务调度强制终止了，就不能再去更新 db，引起数据不一致
            log.info("[{}] Archive log task is force stopped by scheduler, do not update archive task again", taskId);
            return false;
        } else {
            return true;
        }
    }


    private void updateCompletedExecuteInfo(ArchiveTaskStatusEnum status,
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

        log.info("[{}] Update archive log task completed execute info, status: {}, process: {}",
            taskId, status, process);
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

    @Override
    public String getTaskId() {
        return this.taskId;
    }

    @Override
    public void initArchiveTaskWorker(ArchiveTaskWorker archiveTaskWorker) {
        this.archiveTaskWorker = archiveTaskWorker;
    }
}
