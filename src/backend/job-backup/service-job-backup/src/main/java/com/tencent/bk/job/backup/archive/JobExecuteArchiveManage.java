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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

@Slf4j
public class JobExecuteArchiveManage implements SmartLifecycle {
//
//    private final ArchiveDBProperties archiveDBProperties;
//    private final ExecutorService archiveExecutor;
//    private final ArchiveProgressService archiveProgressService;
//    private final TaskInstanceRecordDAO taskInstanceRecordDAO;
//    private final StepInstanceRecordDAO stepInstanceRecordDAO;
//    private final StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO;
//    private final StepInstanceFileRecordDAO stepInstanceFileRecordDAO;
//    private final StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO;
//    private final StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO;
//    private final TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO;
//    private final OperationLogRecordDAO operationLogRecordDAO;
//    private final FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO;
//    private final GseTaskRecordDAO gseTaskRecordDAO;
//    private final GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO;
//    private final GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO;
//    private final GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO;
//    private final GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO;
//    private final StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO;
//    private final RollingConfigRecordDAO rollingConfigRecordDAO;
//    private final TaskInstanceHostRecordDAO taskInstanceHostRecordDAO;
//    private final JobInstanceColdDAO jobInstanceColdDAO;
//    private final ArchiveTaskLock archiveTaskLock;
//    private final ArchiveErrorTaskCounter archiveErrorTaskCounter;
//
//
//    /**
//     * whether this component is currently running(Spring Lifecycle isRunning method)
//     */
//    private volatile boolean running = false;
//
//    public JobExecuteArchiveManage(TaskInstanceRecordDAO taskInstanceRecordDAO,
//                                   StepInstanceRecordDAO stepInstanceRecordDAO,
//                                   StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO,
//                                   StepInstanceFileRecordDAO stepInstanceFileRecordDAO,
//                                   StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO,
//                                   StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO,
//                                   TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO,
//                                   OperationLogRecordDAO operationLogRecordDAO,
//                                   FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO,
//                                   GseTaskRecordDAO gseTaskRecordDAO,
//                                   GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO,
//                                   GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO,
//                                   GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO,
//                                   GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO,
//                                   StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO,
//                                   RollingConfigRecordDAO rollingConfigRecordDAO,
//                                   TaskInstanceHostRecordDAO taskInstanceHostRecordDAO,
//                                   JobInstanceColdDAO jobInstanceColdDAO,
//                                   ArchiveProgressService archiveProgressService,
//                                   ArchiveDBProperties archiveDBProperties,
//                                   ExecutorService archiveExecutor,
//                                   ArchiveTaskLock archiveTaskLock,
//                                   ArchiveErrorTaskCounter archiveErrorTaskCounter) {
//        log.info("Init JobExecuteArchiveManage! archiveConfig: {}", archiveDBProperties);
//        this.archiveDBProperties = archiveDBProperties;
//        this.archiveProgressService = archiveProgressService;
//        this.archiveExecutor = archiveExecutor;
//        this.taskInstanceRecordDAO = taskInstanceRecordDAO;
//        this.stepInstanceRecordDAO = stepInstanceRecordDAO;
//        this.stepInstanceScriptRecordDAO = stepInstanceScriptRecordDAO;
//        this.stepInstanceFileRecordDAO = stepInstanceFileRecordDAO;
//        this.stepInstanceConfirmRecordDAO = stepInstanceConfirmRecordDAO;
//        this.stepInstanceVariableRecordDAO = stepInstanceVariableRecordDAO;
//        this.taskInstanceVariableRecordDAO = taskInstanceVariableRecordDAO;
//        this.operationLogRecordDAO = operationLogRecordDAO;
//        this.fileSourceTaskLogRecordDAO = fileSourceTaskLogRecordDAO;
//        this.gseTaskRecordDAO = gseTaskRecordDAO;
//        this.gseScriptAgentTaskRecordDAO = gseScriptAgentTaskRecordDAO;
//        this.gseFileAgentTaskRecordDAO = gseFileAgentTaskRecordDAO;
//        this.gseScriptExecuteObjTaskRecordDAO = gseScriptExecuteObjTaskRecordDAO;
//        this.gseFileExecuteObjTaskRecordDAO = gseFileExecuteObjTaskRecordDAO;
//        this.stepInstanceRollingTaskRecordDAO = stepInstanceRollingTaskRecordDAO;
//        this.rollingConfigRecordDAO = rollingConfigRecordDAO;
//        this.taskInstanceHostRecordDAO = taskInstanceHostRecordDAO;
//        this.jobInstanceColdDAO = jobInstanceColdDAO;
//        this.archiveTaskLock = archiveTaskLock;
//        this.archiveErrorTaskCounter = archiveErrorTaskCounter;
//    }
//
//    @Scheduled(cron = "${job.backup.archive.execute.cron:0 0 4 * * *}")
//    public void cronArchive() {
//        archive(archiveDBProperties);
//    }
//
//    public void archive(ArchiveDBProperties archiveDBProperties) {
//        try {
//            new ArchiveThread(archiveDBProperties).start();
//        } catch (Throwable e) {
//            log.error("Error while archive job_execute data", e);
//        }
//    }
//
//    @Override
//    public void start() {
//        this.running = true;
//    }
//
//    @Override
//    public void stop() {
//        log.info("Stop JobExecuteArchiveManage!");
//        archiveTaskLock.unlockAll();
//        log.info("Release all archive locks when stop!");
//        this.running = false;
//    }
//
//    @Override
//    public boolean isRunning() {
//        return this.running;
//    }
//
//
//    class ArchiveThread extends Thread {
//        private final ArchiveDBProperties archiveDBProperties;
//
//
//        ArchiveThread(ArchiveDBProperties archiveDBProperties) {
//            this.archiveDBProperties = archiveDBProperties;
//            this.setName("Data-Archive-Thread");
//        }
//
//        @Override
//        public void run() {
////            try {
////                log.info("Job Execute archive task begin, archiveConfig: {}", archiveDBProperties);
////                if (archiveDBProperties.isEnabled()) {
////                    doArchive(getEndTime(archiveDBProperties.getKeepDays()));
////                } else {
////                    log.info("Archive tasks are disabled, skip archive");
////                }
////                log.info("Job Execute archive task finished");
////            } catch (InterruptedException e) {
////                log.warn("Thread interrupted!");
////            }
//        }
//
//        private Long getEndTime(int archiveDays) {
//            DateTime now = DateTime.now();
//            // 置为前一天天 24:00:00
//            long todayMaxMills = now.minusMillis(now.getMillisOfDay()).getMillis();
//
//            //减掉当前xx天后
//            long archiveMills = archiveDays * 24 * 3600 * 1000L;
//            return todayMaxMills - archiveMills;
//        }
//
//
//        private void doArchive(Long endTime) throws InterruptedException {
//            try {
//                log.info("Start job execute archive before {}", endTime);
//
//                long maxNeedArchiveTaskInstanceId = computeMaxNeedArchiveTaskInstanceId(endTime);
//                long maxNeedArchiveStepInstanceId =
//                    computeMaxNeedArchiveStepInstanceId(maxNeedArchiveTaskInstanceId);
//
//                log.info("Compute archive instance id range, maxNeedArchiveTaskInstanceId: {}, " +
//                        "maxNeedArchiveStepInstanceId: {}", maxNeedArchiveTaskInstanceId,
//                    maxNeedArchiveStepInstanceId);
//
//                ArchiveSummaryHolder.getInstance().init(endTime);
//                archive(maxNeedArchiveTaskInstanceId, maxNeedArchiveStepInstanceId);
//                ArchiveSummaryHolder.getInstance().print();
//
//                log.info("Job execute archive before {} success", endTime);
//            } catch (InterruptedException e) {
//                throw e;
//            } catch (Throwable e) {
//                String msg = MessageFormatter.format(
//                    "Error while do archive!|{}",
//                    endTime
//                ).getMessage();
//                log.error(msg, e);
//            }
//        }
//
//        public long computeMaxNeedArchiveTaskInstanceId(Long endTime) {
//            long lastArchivedId = getLastArchiveId(taskInstanceRecordDAO);
//            long maxId = taskInstanceRecordDAO.getMaxId(endTime);
//            return Math.max(lastArchivedId, maxId);
//        }
//
//        public long computeMaxNeedArchiveStepInstanceId(Long taskInstanceId) {
//            long lastArchivedId = getLastArchiveId(stepInstanceRecordDAO);
//            long maxId = stepInstanceRecordDAO.getMaxId(taskInstanceId);
//            return Math.max(lastArchivedId, maxId);
//        }
//
//        private long getLastArchiveId(JobInstanceHotRecordDAO<?> jobInstanceHotRecordDAO) {
//            String tableName = jobInstanceHotRecordDAO.getTable().getName().toLowerCase();
//            ArchiveProgressDTO archiveProgress =
//                archiveProgressService.queryArchiveProgress(tableName);
//            return archiveProgress != null ? archiveProgress.getLastBackupId() : 0L;
//        }
//
//        private void archive(long maxNeedArchiveTaskInstanceId, long maxNeedArchiveStepInstanceId)
//            throws InterruptedException {
//            CountDownLatch countDownLatch = new CountDownLatch(17);
//            log.info("Submitting archive task...");
//
//            // task_instance
//            addTaskInstanceArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
//            // step_instance
//            addStepInstanceArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // step_instance_confirm
//            addStepInstanceConfirmArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // step_instance_file
//            addStepInstanceFileArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // step_instance_script
//            addStepInstanceScriptArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // file_source_task_log
//            addFileSourceTaskLogArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // task_instance_variable
//            addTaskInstanceVariableArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
//            // step_instance_variable
//            addStepInstanceVariableArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // operation_log
//            addOperationLogArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
//            // gse_task
//            addGseTaskArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // gse_script_agent_task
//            addGseScriptAgentTaskArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // gse_file_agent_task
//            addGseFileAgentTaskArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // step_instance_rolling_task
//            addStepInstanceRollingTaskArchiveTask(maxNeedArchiveStepInstanceId, countDownLatch);
//            // rolling_config
//            addRollingConfigArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
//            // task_instance_host
//            addTaskInstanceHostArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
//            // gse_script_execute_obj_task
//            addGseScriptExecuteObjTaskArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
//            // gse_file_execute_obj_task
//            addGseFileExecuteObjTaskArchiveTask(maxNeedArchiveTaskInstanceId, countDownLatch);
//
//            log.info("Archive task submitted. Waiting for complete...");
//            countDownLatch.await();
//
//            log.info("Archive task execute completed.");
//        }
//
//        private void addTaskInstanceArchiveTask(Long maxNeedArchiveTaskInstanceId,
//                                                CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new TaskInstanceArchivist(
//                    taskInstanceRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveTaskInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter
//                ).archive());
//        }
//
//        private void addStepInstanceArchiveTask(Long maxNeedArchiveStepInstanceId,
//                                                CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new StepInstanceArchivist(
//                    stepInstanceRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addStepInstanceConfirmArchiveTask(Long maxNeedArchiveStepInstanceId,
//                                                       CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new StepInstanceConfirmArchivist(
//                    stepInstanceConfirmRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addStepInstanceFileArchiveTask(Long maxNeedArchiveStepInstanceId,
//                                                    CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new StepInstanceFileArchivist(
//                    stepInstanceFileRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addStepInstanceScriptArchiveTask(Long maxNeedArchiveStepInstanceId,
//                                                      CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new StepInstanceScriptArchivist(
//                    stepInstanceScriptRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addFileSourceTaskLogArchiveTask(Long maxNeedArchiveStepInstanceId,
//                                                     CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new FileSourceTaskLogArchivist(
//                    fileSourceTaskLogRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addTaskInstanceVariableArchiveTask(Long maxNeedArchiveTaskInstanceId,
//                                                        CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new TaskInstanceVariableArchivist(
//                    taskInstanceVariableRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveTaskInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter
//                ).archive());
//        }
//
//        private void addStepInstanceVariableArchiveTask(Long maxNeedArchiveStepInstanceId,
//                                                        CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new StepInstanceVariableArchivist(
//                    stepInstanceVariableRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addOperationLogArchiveTask(Long maxNeedArchiveTaskInstanceId,
//                                                CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new OperationLogArchivist(
//                    operationLogRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveTaskInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter
//                ).archive());
//        }
//
//        private void addGseTaskArchiveTask(Long maxNeedArchiveStepInstanceId, CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new GseTaskArchivist(
//                    gseTaskRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addGseScriptAgentTaskArchiveTask(Long maxNeedArchiveStepInstanceId,
//                                                      CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new GseScriptAgentTaskArchivist(
//                    gseScriptAgentTaskRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addGseFileAgentTaskArchiveTask(Long maxNeedArchiveStepInstanceId,
//                                                    CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new GseFileAgentTaskArchivist(
//                    gseFileAgentTaskRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addStepInstanceRollingTaskArchiveTask(Long maxNeedArchiveStepInstanceId,
//                                                           CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new StepInstanceRollingTaskArchivist(
//                    stepInstanceRollingTaskRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveStepInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addGseScriptExecuteObjTaskArchiveTask(Long maxNeedArchiveTaskInstanceId,
//                                                           CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new GseScriptExecuteObjTaskArchivist(
//                    gseScriptExecuteObjTaskRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveTaskInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addGseFileExecuteObjTaskArchiveTask(Long maxNeedArchiveTaskInstanceId,
//                                                         CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new GseFileExecuteObjTaskArchivist(
//                    gseFileExecuteObjTaskRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveTaskInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter)
//                    .archive());
//        }
//
//        private void addRollingConfigArchiveTask(Long maxNeedArchiveTaskInstanceId,
//                                                 CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new RollingConfigArchivist(
//                    rollingConfigRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveTaskInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter
//                ).archive());
//        }
//
//        private void addTaskInstanceHostArchiveTask(Long maxNeedArchiveTaskInstanceId,
//                                                    CountDownLatch countDownLatch) {
//            archiveExecutor.execute(() ->
//                new TaskInstanceHostArchivist(
//                    taskInstanceHostRecordDAO,
//                    jobInstanceColdDAO,
//                    archiveProgressService,
//                    archiveDBProperties,
//                    archiveTaskLock,
//                    maxNeedArchiveTaskInstanceId,
//                    countDownLatch,
//                    archiveErrorTaskCounter
//                ).archive());
//        }
//    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
