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

import com.tencent.bk.job.backup.archive.impl.FileSourceTaskLogArchivist;
import com.tencent.bk.job.backup.archive.impl.GseFileAgentTaskArchivist;
import com.tencent.bk.job.backup.archive.impl.GseScriptAgentTaskArchivist;
import com.tencent.bk.job.backup.archive.impl.GseTaskArchivist;
import com.tencent.bk.job.backup.archive.impl.GseTaskIpLogArchivist;
import com.tencent.bk.job.backup.archive.impl.GseTaskLogArchivist;
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
import com.tencent.bk.job.backup.config.ArchiveConfig;
import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import com.tencent.bk.job.backup.dao.impl.FileSourceTaskRecordDAO;
import com.tencent.bk.job.backup.dao.impl.GseFileAgentTaskRecordDAO;
import com.tencent.bk.job.backup.dao.impl.GseScriptAgentTaskRecordDAO;
import com.tencent.bk.job.backup.dao.impl.GseTaskIpLogRecordDAO;
import com.tencent.bk.job.backup.dao.impl.GseTaskLogRecordDAO;
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
import com.tencent.bk.job.backup.model.dto.ArchiveProgressDTO;
import com.tencent.bk.job.backup.service.ArchiveProgressService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

@Slf4j
public class JobExecuteArchiveManage implements SmartLifecycle {

    private final ArchiveConfig archiveConfig;

    private final ExecutorService archiveExecutor;

    private final FileSourceTaskLogArchivist fileSourceTaskLogArchivist;
    private final StepInstanceArchivist stepInstanceArchivist;
    private final StepInstanceConfirmArchivist stepInstanceConfirmArchivist;
    private final StepInstanceFileArchivist stepInstanceFileArchivist;
    private final StepInstanceScriptArchivist stepInstanceScriptArchivist;
    private final StepInstanceVariableArchivist stepInstanceVariableArchivist;
    private final GseTaskLogArchivist gseTaskLogArchivist;
    private final GseTaskIpLogArchivist gseTaskIpLogArchivist;
    private final TaskInstanceArchivist taskInstanceArchivist;
    private final TaskInstanceVariableArchivist taskInstanceVariableArchivist;
    private final OperationLogArchivist operationLogArchivist;
    private final GseTaskArchivist gseTaskArchivist;
    private final GseScriptAgentTaskArchivist gseScriptAgentTaskArchivist;
    private final GseFileAgentTaskArchivist gseFileAgentTaskArchivist;
    private final StepInstanceRollingTaskArchivist stepInstanceRollingTaskArchivist;
    private final RollingConfigArchivist rollingConfigArchivist;
    private final TaskInstanceHostArchivist taskInstanceHostArchivist;
    private final ArchiveProgressService archiveProgressService;


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
                                   GseTaskLogRecordDAO gseTaskLogRecordDAO,
                                   GseTaskIpLogRecordDAO gseTaskIpLogRecordDAO,
                                   FileSourceTaskRecordDAO fileSourceTaskRecordDAO,
                                   GseTaskRecordDAO gseTaskRecordDAO,
                                   GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO,
                                   GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO,
                                   StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO,
                                   RollingConfigRecordDAO rollingConfigRecordDAO,
                                   TaskInstanceHostRecordDAO taskInstanceHostRecordDAO,
                                   ExecuteArchiveDAO executeArchiveDAO,
                                   ArchiveProgressService archiveProgressService,
                                   ArchiveConfig archiveConfig,
                                   ExecutorService archiveExecutor) {
        log.info("Init JobExecuteArchiveManage! archiveConfig: {}", archiveConfig);
        this.archiveConfig = archiveConfig;
        this.archiveProgressService = archiveProgressService;
        this.fileSourceTaskLogArchivist = new FileSourceTaskLogArchivist(fileSourceTaskRecordDAO, executeArchiveDAO,
            archiveProgressService);
        this.stepInstanceArchivist = new StepInstanceArchivist(stepInstanceRecordDAO, executeArchiveDAO,
            archiveProgressService);
        this.stepInstanceConfirmArchivist = new StepInstanceConfirmArchivist(stepInstanceConfirmRecordDAO,
            executeArchiveDAO, archiveProgressService);
        this.stepInstanceFileArchivist = new StepInstanceFileArchivist(stepInstanceFileRecordDAO, executeArchiveDAO,
            archiveProgressService);
        this.stepInstanceScriptArchivist = new StepInstanceScriptArchivist(stepInstanceScriptRecordDAO,
            executeArchiveDAO, archiveProgressService);
        this.stepInstanceVariableArchivist = new StepInstanceVariableArchivist(stepInstanceVariableRecordDAO,
            executeArchiveDAO, archiveProgressService);
        this.gseTaskLogArchivist = new GseTaskLogArchivist(gseTaskLogRecordDAO, executeArchiveDAO,
            archiveProgressService);
        this.gseTaskIpLogArchivist = new GseTaskIpLogArchivist(gseTaskIpLogRecordDAO, executeArchiveDAO,
            archiveProgressService);
        this.taskInstanceArchivist = new TaskInstanceArchivist(taskInstanceRecordDAO, executeArchiveDAO,
            archiveProgressService);
        this.taskInstanceVariableArchivist = new TaskInstanceVariableArchivist(taskInstanceVariableRecordDAO,
            executeArchiveDAO, archiveProgressService);
        this.operationLogArchivist = new OperationLogArchivist(operationLogRecordDAO, executeArchiveDAO,
            archiveProgressService);
        this.gseTaskArchivist = new GseTaskArchivist(gseTaskRecordDAO, executeArchiveDAO, archiveProgressService);
        this.gseScriptAgentTaskArchivist = new GseScriptAgentTaskArchivist(gseScriptAgentTaskRecordDAO,
            executeArchiveDAO, archiveProgressService);
        this.gseFileAgentTaskArchivist = new GseFileAgentTaskArchivist(gseFileAgentTaskRecordDAO,
            executeArchiveDAO, archiveProgressService);
        this.stepInstanceRollingTaskArchivist = new StepInstanceRollingTaskArchivist(stepInstanceRollingTaskRecordDAO,
            executeArchiveDAO, archiveProgressService);
        this.rollingConfigArchivist = new RollingConfigArchivist(rollingConfigRecordDAO, executeArchiveDAO,
            archiveProgressService);
        this.taskInstanceHostArchivist = new TaskInstanceHostArchivist(taskInstanceHostRecordDAO, executeArchiveDAO,
            archiveProgressService);
        this.archiveExecutor = archiveExecutor;
    }

    @Scheduled(cron = "${job.execute.archive.cron:0 0 4 * * *}")
    public void cronArchive() {
        archive(archiveConfig);
    }

    public void archive(ArchiveConfig archiveConfig) {
        try {
            new ArchiveThread(archiveConfig).start();
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
        ArchiveTaskLock.getInstance().unlockAll();
        log.info("Release all archive locks when stop!");
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }


    class ArchiveThread extends Thread {
        private final ArchiveConfig archiveConfig;


        ArchiveThread(ArchiveConfig archiveConfig) {
            this.archiveConfig = archiveConfig;
            this.setName("Data-Archive-Thread");
        }

        @Override
        public void run() {
            try {
                log.info("Job Execute archive task begin|{}", System.currentTimeMillis());
                log.info("Archive days : {}", archiveConfig.getDataKeepDays());
                if (archiveConfig.isArchiveEnabled() || archiveConfig.isDeleteEnabled()) {
                    doArchive(getEndTime(archiveConfig.getDataKeepDays()));
                } else {
                    log.info("Archive and delete tasks are disabled, skip archive");
                }
                log.info("Job Execute archive task finished|{}", System.currentTimeMillis());
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
                log.info("Start job execute archive before {} at {}", endTime, System.currentTimeMillis());

                long maxNeedArchiveTaskInstanceId = computeMaxNeedArchiveTaskInstanceId(endTime);
                long maxNeedArchiveStepInstanceId = computeMaxNeedArchiveStepInstanceId(maxNeedArchiveTaskInstanceId);

                log.info("Compute archive instance id range, maxNeedArchiveTaskInstanceId: {}, " +
                    "maxNeedArchiveStepInstanceId: {}", maxNeedArchiveTaskInstanceId, maxNeedArchiveStepInstanceId);

                ArchiveSummaryHolder.getInstance().init(endTime);
                archive(maxNeedArchiveTaskInstanceId, maxNeedArchiveStepInstanceId);
                ArchiveSummaryHolder.getInstance().print();

                log.info("Job execute archive before {} success at {}", endTime, System.currentTimeMillis());
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                log.error("Error while do archive!|{}", endTime, e);
            }
        }

        public long computeMaxNeedArchiveTaskInstanceId(Long endTime) {
            ArchiveProgressDTO archiveProgress =
                archiveProgressService.queryArchiveProgress(taskInstanceArchivist.getTableName());
            long lastArchivedId = archiveProgress != null ? archiveProgress.getLastArchivedId() : 0L;
            long maxId = taskInstanceArchivist.getMaxId(endTime);
            return Math.max(lastArchivedId, maxId);
        }

        public long computeMaxNeedArchiveStepInstanceId(Long taskInstanceId) {
            ArchiveProgressDTO archiveProgress =
                archiveProgressService.queryArchiveProgress(stepInstanceArchivist.getTableName());
            long lastArchivedId = archiveProgress != null ? archiveProgress.getLastArchivedId() : 0L;
            long maxId = stepInstanceArchivist.getMaxId(taskInstanceId);
            return Math.max(lastArchivedId, maxId);
        }

        private void archive(long maxNeedArchiveTaskInstanceId, long maxNeedArchiveStepInstanceId)
            throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(16);
            log.info("Submitting archive task...");

            // task_instance
            archiveExecutor.execute(() -> taskInstanceArchivist.archive(archiveConfig,
                maxNeedArchiveTaskInstanceId, countDownLatch));
            // step_instance
            archiveExecutor.execute(() -> stepInstanceArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // step_instance_confirm
            archiveExecutor.execute(() -> stepInstanceConfirmArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // step_instance_file
            archiveExecutor.execute(() -> stepInstanceFileArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // step_instance_script
            archiveExecutor.execute(() -> stepInstanceScriptArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // gse_task_log
            archiveExecutor.execute(() -> gseTaskLogArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // file_source_task 非正式发布功能，暂时不开启
//            ARCHIVE_THREAD_POOL_EXECUTOR.execute(() -> fileSourceTaskLogArchivist.archive(archiveConfig,
//                maxNeedArchiveStepInstanceId, countDownLatch));
            // gse_task_ip_log
            archiveExecutor.execute(() -> gseTaskIpLogArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // task_instance_variable
            archiveExecutor.execute(() -> taskInstanceVariableArchivist.archive(archiveConfig,
                maxNeedArchiveTaskInstanceId, countDownLatch));
            // step_instance_variable
            archiveExecutor.execute(() -> stepInstanceVariableArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // operation_log
            archiveExecutor.execute(() -> operationLogArchivist.archive(archiveConfig,
                maxNeedArchiveTaskInstanceId, countDownLatch));
            // gse_task
            archiveExecutor.execute(() -> gseTaskArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // gse_script_agent_task
            archiveExecutor.execute(() -> gseScriptAgentTaskArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // gse_file_agent_task
            archiveExecutor.execute(() -> gseFileAgentTaskArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // step_instance_rolling_task
            archiveExecutor.execute(() -> stepInstanceRollingTaskArchivist.archive(archiveConfig,
                maxNeedArchiveStepInstanceId, countDownLatch));
            // rolling_config
            archiveExecutor.execute(() -> rollingConfigArchivist.archive(archiveConfig,
                maxNeedArchiveTaskInstanceId, countDownLatch));
            // task_instance_host
            archiveExecutor.execute(() -> taskInstanceHostArchivist.archive(archiveConfig,
                maxNeedArchiveTaskInstanceId, countDownLatch));

            log.info("Archive task submitted. Waiting for complete...");
            countDownLatch.await();

            log.info("Archive task execute completed.");
        }
    }
}
