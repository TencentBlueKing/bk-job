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

import com.tencent.bk.job.backup.archive.impl.FileSourceTaskLogArchivist;
import com.tencent.bk.job.backup.archive.impl.GseFileAgentTaskArchivist;
import com.tencent.bk.job.backup.archive.impl.GseFileExecuteObjTaskArchivist;
import com.tencent.bk.job.backup.archive.impl.GseScriptAgentTaskArchivist;
import com.tencent.bk.job.backup.archive.impl.GseScriptExecuteObjTaskArchivist;
import com.tencent.bk.job.backup.archive.impl.GseTaskArchivist;
import com.tencent.bk.job.backup.archive.impl.OperationLogArchivist;
import com.tencent.bk.job.backup.archive.impl.RollingConfigArchivist;
import com.tencent.bk.job.backup.archive.impl.StepInstanceArchivist;
import com.tencent.bk.job.backup.archive.impl.StepInstanceConfirmArchivist;
import com.tencent.bk.job.backup.archive.impl.StepInstanceFileArchivist;
import com.tencent.bk.job.backup.archive.impl.StepInstanceRollingTaskArchivist;
import com.tencent.bk.job.backup.archive.impl.StepInstanceScriptArchivist;
import com.tencent.bk.job.backup.archive.impl.StepInstanceVariableArchivist;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceArchivist;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceHostArchivist;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceVariableArchivist;
import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import com.tencent.bk.job.backup.dao.ExecuteRecordDAO;
import com.tencent.bk.job.backup.dao.impl.FileSourceTaskLogRecordDAO;
import com.tencent.bk.job.backup.dao.impl.GseFileAgentTaskRecordDAO;
import com.tencent.bk.job.backup.dao.impl.GseFileExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.dao.impl.GseScriptAgentTaskRecordDAO;
import com.tencent.bk.job.backup.dao.impl.GseScriptExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.dao.impl.GseTaskRecordDAO;
import com.tencent.bk.job.backup.dao.impl.OperationLogRecordDAO;
import com.tencent.bk.job.backup.dao.impl.RollingConfigRecordDAO;
import com.tencent.bk.job.backup.dao.impl.StepInstanceConfirmRecordDAO;
import com.tencent.bk.job.backup.dao.impl.StepInstanceFileRecordDAO;
import com.tencent.bk.job.backup.dao.impl.StepInstanceRecordDAO;
import com.tencent.bk.job.backup.dao.impl.StepInstanceRollingTaskRecordDAO;
import com.tencent.bk.job.backup.dao.impl.StepInstanceScriptRecordDAO;
import com.tencent.bk.job.backup.dao.impl.StepInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.dao.impl.TaskInstanceHostRecordDAO;
import com.tencent.bk.job.backup.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.dao.impl.TaskInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.backup.model.dto.ArchiveProgressDTO;
import com.tencent.bk.job.backup.service.ArchiveProgressService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

@Slf4j
public class JobExecuteArchiveManage implements SmartLifecycle {

    private final ArchiveDBProperties archiveDBProperties;
    private final ExecutorService archiveExecutor;
    private final ArchiveProgressService archiveProgressService;
    private final TaskInstanceRecordDAO taskInstanceRecordDAO;
    private final StepInstanceRecordDAO stepInstanceRecordDAO;
    private final StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO;
    private final StepInstanceFileRecordDAO stepInstanceFileRecordDAO;
    private final StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO;
    private final StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO;
    private final TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO;
    private final OperationLogRecordDAO operationLogRecordDAO;
    private final FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO;
    private final GseTaskRecordDAO gseTaskRecordDAO;
    private final GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO;
    private final GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO;
    private final GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO;
    private final GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO;
    private final StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO;
    private final RollingConfigRecordDAO rollingConfigRecordDAO;
    private final TaskInstanceHostRecordDAO taskInstanceHostRecordDAO;
    private final ExecuteArchiveDAO executeArchiveDAO;
    private final ArchiveTaskLock archiveTaskLock;
    private final ArchiveErrorTaskCounter archiveErrorTaskCounter;


    /**
     * whether this component is currently running(Spring Lifecycle isRunning method)
     */
    private volatile boolean running = false;

    public JobExecuteArchiveManage(TaskInstanceRecordDAO taskInstanceRecordDAO,
                                   StepInstanceRecordDAO stepInstanceRecordDAO,
                                   StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO,
                                   StepInstanceFileRecordDAO stepInstanceFileRecordDAO,
                                   StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO,
                                   StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO,
                                   TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO,
                                   OperationLogRecordDAO operationLogRecordDAO,
                                   FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO,
                                   GseTaskRecordDAO gseTaskRecordDAO,
                                   GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO,
                                   GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO,
                                   GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO,
                                   GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO,
                                   StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO,
                                   RollingConfigRecordDAO rollingConfigRecordDAO,
                                   TaskInstanceHostRecordDAO taskInstanceHostRecordDAO,
                                   ExecuteArchiveDAO executeArchiveDAO,
                                   ArchiveProgressService archiveProgressService,
                                   ArchiveDBProperties archiveDBProperties,
                                   ExecutorService archiveExecutor,
                                   ArchiveTaskLock archiveTaskLock,
                                   ArchiveErrorTaskCounter archiveErrorTaskCounter) {
        log.info("Init JobExecuteArchiveManage! archiveConfig: {}", archiveDBProperties);
        this.archiveDBProperties = archiveDBProperties;
        this.archiveProgressService = archiveProgressService;
        this.archiveExecutor = archiveExecutor;
        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
        this.stepInstanceRecordDAO = stepInstanceRecordDAO;
        this.stepInstanceScriptRecordDAO = stepInstanceScriptRecordDAO;
        this.stepInstanceFileRecordDAO = stepInstanceFileRecordDAO;
        this.stepInstanceConfirmRecordDAO = stepInstanceConfirmRecordDAO;
        this.stepInstanceVariableRecordDAO = stepInstanceVariableRecordDAO;
        this.taskInstanceVariableRecordDAO = taskInstanceVariableRecordDAO;
        this.operationLogRecordDAO = operationLogRecordDAO;
        this.fileSourceTaskLogRecordDAO = fileSourceTaskLogRecordDAO;
        this.gseTaskRecordDAO = gseTaskRecordDAO;
        this.gseScriptAgentTaskRecordDAO = gseScriptAgentTaskRecordDAO;
        this.gseFileAgentTaskRecordDAO = gseFileAgentTaskRecordDAO;
        this.gseScriptExecuteObjTaskRecordDAO = gseScriptExecuteObjTaskRecordDAO;
        this.gseFileExecuteObjTaskRecordDAO = gseFileExecuteObjTaskRecordDAO;
        this.stepInstanceRollingTaskRecordDAO = stepInstanceRollingTaskRecordDAO;
        this.rollingConfigRecordDAO = rollingConfigRecordDAO;
        this.taskInstanceHostRecordDAO = taskInstanceHostRecordDAO;
        this.executeArchiveDAO = executeArchiveDAO;
        this.archiveTaskLock = archiveTaskLock;
        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
    }

    @Scheduled(cron = "${job.backup.archive.execute.cron:0 0 4 * * *}")
    public void cronArchive() {
        archive(archiveDBProperties);
    }

    public void archive(ArchiveDBProperties archiveDBProperties) {
        try {
            new ArchiveThread(archiveDBProperties).start();
        } catch (Throwable e) {
            log.error("Error while archive job_execute data", e);
        }
    }

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        log.info("Stop JobExecuteArchiveManage!");
        archiveTaskLock.unlockAll();
        log.info("Release all archive locks when stop!");
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }


    class ArchiveThread extends Thread {
        private final ArchiveDBProperties archiveDBProperties;


        ArchiveThread(ArchiveDBProperties archiveDBProperties) {
            this.archiveDBProperties = archiveDBProperties;
            this.setName("Data-Archive-Thread");
        }

        @Override
        public void run() {
            try {
                log.info("Job Execute archive task begin, archiveConfig: {}", archiveDBProperties);
                if (archiveDBProperties.isEnabled()) {
                    doArchive(getEndTime(archiveDBProperties.getKeepDays()));
                } else {
                    log.info("Archive tasks are disabled, skip archive");
                }
                log.info("Job Execute archive task finished");
            } catch (InterruptedException e) {
                log.warn("Thread interrupted!");
            }
        }

        private Long getEndTime(int archiveDays) {
            DateTime now = DateTime.now();
            // 置为前一天天 24:00:00
            long todayMaxMills = now.minusMillis(now.getMillisOfDay()).getMillis();

            //减掉当前xx天后
            long archiveMills = archiveDays * 24 * 3600 * 1000L;
            return todayMaxMills - archiveMills;
        }


        private void doArchive(Long endTime) throws InterruptedException {
            try {
                log.info("Start job execute archive before {}", endTime);

                long maxNeedArchiveTaskInstanceId = computeMaxNeedArchiveTaskInstanceId(endTime);
                long maxNeedArchiveStepInstanceId =
                    computeMaxNeedArchiveStepInstanceId(maxNeedArchiveTaskInstanceId);

                log.info("Compute archive instance id range, maxNeedArchiveTaskInstanceId: {}, " +
                        "maxNeedArchiveStepInstanceId: {}", maxNeedArchiveTaskInstanceId,
                    maxNeedArchiveStepInstanceId);

                ArchiveSummaryHolder.getInstance().init(endTime);
                archive(maxNeedArchiveTaskInstanceId, maxNeedArchiveStepInstanceId);
                ArchiveSummaryHolder.getInstance().print();

                log.info("Job execute archive before {} success", endTime);
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                String msg = MessageFormatter.format(
                    "Error while do archive!|{}",
                    endTime
                ).getMessage();
                log.error(msg, e);
            }
        }

        public long computeMaxNeedArchiveTaskInstanceId(Long endTime) {
            long lastArchivedId = getLastArchiveId(taskInstanceRecordDAO);
            long maxId = taskInstanceRecordDAO.getMaxId(endTime);
            return Math.max(lastArchivedId, maxId);
        }

        public long computeMaxNeedArchiveStepInstanceId(Long taskInstanceId) {
            long lastArchivedId = getLastArchiveId(stepInstanceRecordDAO);
            long maxId = stepInstanceRecordDAO.getMaxId(taskInstanceId);
            return Math.max(lastArchivedId, maxId);
        }

        private long getLastArchiveId(ExecuteRecordDAO<?> executeRecordDAO) {
            String tableName = executeRecordDAO.getTable().getName().toLowerCase();
            ArchiveProgressDTO archiveProgress =
                archiveProgressService.queryArchiveProgress(tableName);
            return archiveProgress != null ? archiveProgress.getLastBackupId() : 0L;
        }

        private void archive(long maxNeedArchiveTaskInstanceId, long maxNeedArchiveStepInstanceId)
            throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(17);
            log.info("Submitting archive task...");

            // task_instance
            addTaskInstanceArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
            // step_instance
            addStepInstanceArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // step_instance_confirm
            addStepInstanceConfirmArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // step_instance_file
            addStepInstanceFileArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // step_instance_script
            addStepInstanceScriptArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // file_source_task_log
            addFileSourceTaskLogArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // task_instance_variable
            addTaskInstanceVariableArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
            // step_instance_variable
            addStepInstanceVariableArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // operation_log
            addOperationLogArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
            // gse_task
            addGseTaskArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // gse_script_agent_task
            addGseScriptAgentTaskArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // gse_file_agent_task
            addGseFileAgentTaskArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // step_instance_rolling_task
            addStepInstanceRollingTaskArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
            // rolling_config
            addRollingConfigArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
            // task_instance_host
            addTaskInstanceHostArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
            // gse_script_execute_obj_task
            addGseScriptExecuteObjTaskArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
            // gse_file_execute_obj_task
            addGseFileExecuteObjTaskArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);

            log.info("Archive task submitted. Waiting for complete...");
            countDownLatch.await();

            log.info("Archive task execute completed.");
        }

        private void addTaskInstanceArchiveTask(Long maxNeedArchiveTaskInstanceId,
                                                CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new TaskInstanceArchivist(
                    taskInstanceRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveTaskInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter
                ).archive());
        }

        private void addStepInstanceArchiveTask(Long maxNeedArchiveStepInstanceId,
                                                CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new StepInstanceArchivist(
                    stepInstanceRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addStepInstanceConfirmArchiveTask(Long maxNeedArchiveStepInstanceId,
                                                       CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new StepInstanceConfirmArchivist(
                    stepInstanceConfirmRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addStepInstanceFileArchiveTask(Long maxNeedArchiveStepInstanceId,
                                                    CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new StepInstanceFileArchivist(
                    stepInstanceFileRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addStepInstanceScriptArchiveTask(Long maxNeedArchiveStepInstanceId,
                                                      CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new StepInstanceScriptArchivist(
                    stepInstanceScriptRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addFileSourceTaskLogArchiveTask(Long maxNeedArchiveStepInstanceId,
                                                     CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new FileSourceTaskLogArchivist(
                    fileSourceTaskLogRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addTaskInstanceVariableArchiveTask(Long maxNeedArchiveTaskInstanceId,
                                                        CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new TaskInstanceVariableArchivist(
                    taskInstanceVariableRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveTaskInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter
                ).archive());
        }

        private void addStepInstanceVariableArchiveTask(Long maxNeedArchiveStepInstanceId,
                                                        CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new StepInstanceVariableArchivist(
                    stepInstanceVariableRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addOperationLogArchiveTask(Long maxNeedArchiveTaskInstanceId,
                                                CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new OperationLogArchivist(
                    operationLogRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveTaskInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter
                ).archive());
        }

        private void addGseTaskArchiveTask(Long maxNeedArchiveStepInstanceId, CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new GseTaskArchivist(
                    gseTaskRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addGseScriptAgentTaskArchiveTask(Long maxNeedArchiveStepInstanceId,
                                                      CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new GseScriptAgentTaskArchivist(
                    gseScriptAgentTaskRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addGseFileAgentTaskArchiveTask(Long maxNeedArchiveStepInstanceId,
                                                    CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new GseFileAgentTaskArchivist(
                    gseFileAgentTaskRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addStepInstanceRollingTaskArchiveTask(Long maxNeedArchiveStepInstanceId,
                                                           CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new StepInstanceRollingTaskArchivist(
                    stepInstanceRollingTaskRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveStepInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addGseScriptExecuteObjTaskArchiveTask(Long maxNeedArchiveTaskInstanceId,
                                                           CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new GseScriptExecuteObjTaskArchivist(
                    gseScriptExecuteObjTaskRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveTaskInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addGseFileExecuteObjTaskArchiveTask(Long maxNeedArchiveTaskInstanceId,
                                                         CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new GseFileExecuteObjTaskArchivist(
                    gseFileExecuteObjTaskRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveTaskInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter)
                    .archive());
        }

        private void addRollingConfigArchiveTask(Long maxNeedArchiveTaskInstanceId,
                                                 CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new RollingConfigArchivist(
                    rollingConfigRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveTaskInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter
                ).archive());
        }

        private void addTaskInstanceHostArchiveTask(Long maxNeedArchiveTaskInstanceId,
                                                    CountDownLatch countDownLatch) {
            archiveExecutor.execute(() ->
                new TaskInstanceHostArchivist(
                    taskInstanceHostRecordDAO,
                    executeArchiveDAO,
                    archiveProgressService,
                    archiveDBProperties,
                    archiveTaskLock,
                    maxNeedArchiveTaskInstanceId,
                    countDownLatch,
                    archiveErrorTaskCounter
                ).archive());
        }
    }
}
