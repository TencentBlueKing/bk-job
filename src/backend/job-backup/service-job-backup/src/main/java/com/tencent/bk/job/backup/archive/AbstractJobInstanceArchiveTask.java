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

import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.impl.AbstractJobInstanceMainHotRecordDAO;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskContext;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskSummary;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.TimeAndIdBasedArchiveProcess;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveTaskExecuteLock;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.backup.constant.ArchiveModeEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.TableRecord;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 作业实例数据归档任务基础实现
 *
 * @param <T> 表记录
 */
@Slf4j
public abstract class AbstractJobInstanceArchiveTask<T extends TableRecord<?>> implements JobInstanceArchiveTask {

    /**
     * 作业实例主表 DAO
     */
    protected AbstractJobInstanceMainHotRecordDAO<T> jobInstanceMainRecordDAO;
    /**
     * 冷 DB DAO
     */
    protected JobInstanceColdDAO jobInstanceColdDAO;
    protected final ArchiveProperties archiveProperties;
    private final ArchiveTaskExecuteLock archiveTaskExecuteLock;
    protected final ArchiveErrorTaskCounter archiveErrorTaskCounter;
    protected final ArchiveTaskService archiveTaskService;

    protected final ArchiveTablePropsStorage archiveTablePropsStorage;

    protected String taskId;
    protected JobInstanceArchiveTaskInfo archiveTaskInfo;

    /**
     * 归档进度
     */
    private TimeAndIdBasedArchiveProcess progress;

    private final ArchiveTaskSummary archiveTaskSummary;

    private boolean isAcquireLock;
    /**
     * 任务是否已停止
     */
    protected volatile boolean isStopped = false;
    /**
     * 任务终止标识
     */
    protected volatile boolean stopFlag = false;
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


    public AbstractJobInstanceArchiveTask(AbstractJobInstanceMainHotRecordDAO<T> jobInstanceMainRecordDAO,
                                          JobInstanceColdDAO jobInstanceColdDAO,
                                          ArchiveProperties archiveProperties,
                                          ArchiveTaskExecuteLock archiveTaskExecuteLock,
                                          ArchiveErrorTaskCounter archiveErrorTaskCounter,
                                          JobInstanceArchiveTaskInfo archiveTaskInfo,
                                          ArchiveTaskService archiveTaskService,
                                          ArchiveTablePropsStorage archiveTablePropsStorage) {
        this.jobInstanceMainRecordDAO = jobInstanceMainRecordDAO;
        this.jobInstanceColdDAO = jobInstanceColdDAO;
        this.archiveProperties = archiveProperties;
        this.archiveTaskExecuteLock = archiveTaskExecuteLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
        this.archiveTaskInfo = archiveTaskInfo;
        this.archiveTaskService = archiveTaskService;
        this.archiveTablePropsStorage = archiveTablePropsStorage;
        this.progress = archiveTaskInfo.getProcess();
        this.taskId = archiveTaskInfo.buildTaskUniqueId();
        this.archiveTaskSummary = new ArchiveTaskSummary(archiveTaskInfo, archiveProperties.getMode());
    }

    @Override
    public void execute() {
        archive();
    }

    @Override
    public void stop(ArchiveTaskStopCallback stopCallback) {
        synchronized (stopMonitor) {
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
        synchronized (stopMonitor) {
            if (!isStopped) {
                isStopped = true;
                // 更新归档任务状态为暂停，用于后续调度
                archiveTaskService.updateArchiveTaskSuspendedStatus(archiveTaskInfo);
                if (stopCallback != null) {
                    stopCallback.callback();
                }
                log.info("[{}] Stop archive task successfully", taskId);
            } else {
                log.info("[{}] Archive task is stopped", taskId);
            }
        }
    }

    @Override
    public void forceStopAtOnce() {
        log.info("Force stop archive task at once. taskId: {}", taskId);
        forceStoppedByScheduler.set(true);
        // 更新归档任务状态为“暂停”
        archiveTaskService.updateArchiveTaskSuspendedStatus(archiveTaskInfo);
        // 打断当前线程，退出执行
        archiveTaskWorker.interrupt();
    }

    @Override
    public void registerDoneCallback(ArchiveTaskDoneCallback archiveTaskDoneCallback) {
        this.archiveTaskDoneCallback = archiveTaskDoneCallback;
    }

    private void archive() {
        try {
            // 设置归档任务上下文
            ArchiveTaskContextHolder.set(new ArchiveTaskContext(archiveTaskInfo));

            // 获取分布式锁
            if (!acquireLock()) {
                archiveTaskSummary.setSkip(!isAcquireLock);
                return;
            }

            log.info("[{}] Start archive task", taskId);
            updateArchiveTask(ArchiveTaskStatusEnum.RUNNING, null);
            // 归档
            backupAndDelete();
        } catch (Throwable e) {
            String msg = MessageFormatter.format(
                "[{}] Error while execute archive task",
                taskId
            ).getMessage();
            log.error(msg, e);
            archiveTaskSummary.setMessage(e.getMessage());

            archiveErrorTaskCounter.increment();

            // 更新归档任务状态
            updateArchiveTask(ArchiveTaskStatusEnum.FAIL, null);
        } finally {
            if (this.isAcquireLock) {
                archiveTaskExecuteLock.unlock(taskId);
            }
            log.info(
                "[{}] Archive finished, result: {}",
                taskId,
                JsonUtils.toJson(archiveTaskSummary)
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
        boolean backupEnabled = isBackupEnable();
        boolean deleteEnabled = isDeleteEnable();
        // 获取主表对应的 readRowLimit
        int readLimit = archiveTablePropsStorage.getReadRowLimit(jobInstanceMainRecordDAO.getTable().getName());
        long archivedJobInstanceCount = 0;

        long startTime = System.currentTimeMillis();
        log.info("[{}] Archive task mode: {}, backupEnabled: {}, deleteEnabled: {}",
            taskId, archiveProperties.getMode(), backupEnabled, deleteEnabled);
        try {
            List<T> jobInstanceRecords;
            do {
                // 检查任务终止标识
                if (checkStopFlag()) {
                    stopTask();
                }

                jobInstanceRecords = readJobInstanceRecords(readLimit);
                if (CollectionUtils.isEmpty(jobInstanceRecords)) {
                    updateArchiveTask(ArchiveTaskStatusEnum.SUCCESS, null);
                    return;
                }
                archivedJobInstanceCount += jobInstanceRecords.size();

                List<Long> jobInstanceIds =
                    jobInstanceRecords.stream().map(this::extractJobInstanceId).collect(Collectors.toList());

                // 写入数据到冷 db
                if (backupEnabled) {
                    long backupStartTime = System.currentTimeMillis();
                    backupJobInstanceToColdDb(jobInstanceRecords);
                    log.info("[{}] Backup to cold db, jobInstanceRecordSize: {}, cost: {}",
                        taskId, jobInstanceRecords.size(), System.currentTimeMillis() - backupStartTime);
                }
                // 从热 db 删除数据
                if (deleteEnabled) {
                    long deleteStartTime = System.currentTimeMillis();
                    deleteJobInstanceHotData(jobInstanceIds);
                    log.info("[{}] Delete hot db, jobInstanceRecordSize: {}, cost: {}",
                        taskId, jobInstanceRecords.size(), System.currentTimeMillis() - deleteStartTime);
                }

                // 更新归档进度
                T lastRecord = jobInstanceRecords.get(jobInstanceRecords.size() - 1);
                Long lastTimestamp = extractJobInstanceCreateTime(lastRecord);
                Long lastJobInstanceId = extractJobInstanceId(lastRecord);
                progress = new TimeAndIdBasedArchiveProcess(lastTimestamp, lastJobInstanceId);
                boolean isFinished = jobInstanceRecords.size() < readLimit;
                updateArchiveTask(isFinished ? ArchiveTaskStatusEnum.SUCCESS : ArchiveTaskStatusEnum.RUNNING, progress);
            } while (jobInstanceRecords.size() == readLimit);
        } finally {
            long archiveCost = System.currentTimeMillis() - startTime;
            archiveTaskSummary.setArchivedRecordSize(archivedJobInstanceCount);
            archiveTaskSummary.setArchiveCost(archiveCost);
        }
    }

    private List<T> readJobInstanceRecords(int readLimit) {
        List<T> jobInstanceRecords;
        long readStartTime = System.currentTimeMillis();
        Long fromTime = progress == null ? archiveTaskInfo.getFromTimestamp() : progress.getTimestamp();
        Long fromTaskInstanceId = progress != null ? progress.getId() : null;
        jobInstanceRecords = jobInstanceMainRecordDAO.readSortedJobInstanceFromHotDB(fromTime,
            archiveTaskInfo.getToTimestamp(), fromTaskInstanceId, readLimit);
        log.info("[{}] Read sorted job instance from hot db, fromJobCreateTime: {}, toJobCreatTime: {}, " +
                "fromJobInstanceId: {}, resultSize: {}, cost: {} ms",
            taskId, fromTime, archiveTaskInfo.getToTimestamp(), fromTaskInstanceId,
            jobInstanceRecords.size(), System.currentTimeMillis() - readStartTime);
        return jobInstanceRecords;
    }

    /**
     * 备份作业实例数据到冷存储
     *
     * @param jobInstances 作业实例列表
     */
    protected abstract void backupJobInstanceToColdDb(List<T> jobInstances);

    /**
     * 删除作业实例热数据
     *
     * @param jobInstanceIds 作业实例 ID 列表
     */
    protected abstract void deleteJobInstanceHotData(List<Long> jobInstanceIds);

    protected boolean isBackupEnable() {
        ArchiveModeEnum archiveMode = ArchiveModeEnum.valOf(archiveProperties.getMode());
        return archiveProperties.isEnabled()
            && (ArchiveModeEnum.BACKUP_THEN_DELETE == archiveMode || ArchiveModeEnum.BACKUP_ONLY == archiveMode);
    }

    protected boolean isDeleteEnable() {
        ArchiveModeEnum archiveMode = ArchiveModeEnum.valOf(archiveProperties.getMode());
        return archiveProperties.isEnabled()
            && (ArchiveModeEnum.BACKUP_THEN_DELETE == archiveMode || ArchiveModeEnum.DELETE_ONLY == archiveMode);
    }

    private boolean acquireLock() {
        this.isAcquireLock = archiveTaskExecuteLock.lock(taskId);
        if (!isAcquireLock) {
            log.info("[{}] Acquire archive task lock fail", taskId);
        }
        return isAcquireLock;
    }

    private void updateArchiveTask(ArchiveTaskStatusEnum status,
                                   TimeAndIdBasedArchiveProcess progress) {
        if (status != null) {
            archiveTaskInfo.setStatus(status);
        }
        if (progress != null) {
            archiveTaskInfo.setProcess(progress);
        }
        if (forceStoppedByScheduler.get()) {
            // 如果已经被归档任务调度强制终止了，就不能再去更新 db，引起数据不一致
            log.info("[{}] Archive task is force stopped by scheduler, do not update archive task again", taskId);
            return;
        }

        if (archiveTaskInfo.getStatus() != ArchiveTaskStatusEnum.RUNNING) {
            log.info("[{}] Update archive task, taskStatus: {}, process: {}",
                taskId, archiveTaskInfo.getStatus(), progress);
        }
        archiveTaskService.updateTask(archiveTaskInfo);
    }

    @Override
    public String getTaskId() {
        return this.taskId;
    }

    /**
     * 从作业实例记录中提取作业实例 ID
     *
     * @param record 作业实例记录
     */
    protected Long extractJobInstanceId(T record) {
        return record.get(jobInstanceMainRecordDAO.getJobInstanceIdField());
    }

    /**
     * 从作业实例记录中提取作业实例创建时间
     *
     * @param record 作业实例记录
     */
    protected Long extractJobInstanceCreateTime(T record) {
        return record.get(jobInstanceMainRecordDAO.getJobInstanceCreateTimeField());
    }

    @Override
    public void initArchiveTaskWorker(ArchiveTaskWorker archiveTaskWorker) {
        this.archiveTaskWorker = archiveTaskWorker;
    }
}
