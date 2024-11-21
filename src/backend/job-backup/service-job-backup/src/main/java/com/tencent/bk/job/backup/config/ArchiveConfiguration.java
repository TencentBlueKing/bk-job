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

package com.tencent.bk.job.backup.config;

import com.tencent.bk.job.backup.archive.ArchiveTablePropsStorage;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveCronJobs;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveTaskGenerator;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveTaskScheduler;
import com.tencent.bk.job.backup.archive.JobInstanceSubTableArchivers;
import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.impl.FileSourceTaskLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseFileAgentTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseFileExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseScriptAgentTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseScriptExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.OperationLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.RollingConfigRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceConfirmRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceFileRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceRollingTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceScriptRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceHostRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.archive.impl.FileSourceTaskLogArchiver;
import com.tencent.bk.job.backup.archive.impl.GseFileAgentTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseFileExecuteObjTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseScriptAgentTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseScriptExecuteObjTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.OperationLogArchiver;
import com.tencent.bk.job.backup.archive.impl.RollingConfigArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceConfirmArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceFileArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceRollingTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceScriptArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceVariableArchiver;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceArchiver;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceHostArchiver;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceVariableArchiver;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveTaskExecuteLock;
import com.tencent.bk.job.backup.archive.util.lock.JobInstanceArchiveTaskGenerateLock;
import com.tencent.bk.job.backup.archive.util.lock.JobInstanceArchiveTaskScheduleLock;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * job-execute 模块数据归档配置
 */
@Configuration
@EnableScheduling
@Slf4j
@EnableConfigurationProperties(ArchiveProperties.class)
@Import({ExecuteHotDbConfiguration.class, ExecuteColdDbConfiguration.class})
@ConditionalOnExpression("${job.backup.archive.execute.enabled:false}")
public class ArchiveConfiguration {

    /**
     * job-execute DB 配置
     */
    @Configuration
    public static class ExecuteDaoAutoConfig {

        @Bean(name = "taskInstanceRecordDAO")
        public TaskInstanceRecordDAO taskInstanceRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init TaskInstanceRecordDAO");
            return new TaskInstanceRecordDAO(dslContextProvider);
        }

        @Bean
        public TaskInstanceArchiver taskInstanceArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            TaskInstanceRecordDAO taskInstanceRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new TaskInstanceArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                taskInstanceRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceRecordDAO")
        public StepInstanceRecordDAO stepInstanceRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceRecordDAO");
            return new StepInstanceRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceArchiver stepInstanceArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceRecordDAO stepInstanceRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceScriptRecordDAO")
        public StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceScriptRecordDAO");
            return new StepInstanceScriptRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceScriptArchiver stepInstanceScriptArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceScriptArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceScriptRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceFileRecordDAO")
        public StepInstanceFileRecordDAO stepInstanceFileRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceFileRecordDAO");
            return new StepInstanceFileRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceFileArchiver stepInstanceFileArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceFileRecordDAO stepInstanceFileRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceFileArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceFileRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceConfirmRecordDAO")
        public StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceConfirmRecordDAO");
            return new StepInstanceConfirmRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceConfirmArchiver stepInstanceConfirmArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceConfirmArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceConfirmRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceVariableRecordDAO")
        public StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceVariableRecordDAO");
            return new StepInstanceVariableRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceVariableArchiver stepInstanceVariableArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceVariableArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceVariableRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "taskInstanceVariableRecordDAO")
        public TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init TaskInstanceVariableRecordDAO");
            return new TaskInstanceVariableRecordDAO(dslContextProvider);
        }

        @Bean
        public TaskInstanceVariableArchiver taskInstanceVariableArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new TaskInstanceVariableArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                taskInstanceVariableRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "operationLogRecordDAO")
        public OperationLogRecordDAO operationLogRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init OperationLogRecordDAO");
            return new OperationLogRecordDAO(dslContextProvider);
        }

        @Bean
        public OperationLogArchiver operationLogArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            OperationLogRecordDAO operationLogRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new OperationLogArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                operationLogRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "fileSourceTaskLogRecordDAO")
        public FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init FileSourceTaskRecordDAO");
            return new FileSourceTaskLogRecordDAO(dslContextProvider);
        }

        @Bean
        public FileSourceTaskLogArchiver fileSourceTaskLogArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new FileSourceTaskLogArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                fileSourceTaskLogRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseTaskRecordDAO")
        public GseTaskRecordDAO gseTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseTaskRecordDAO");
            return new GseTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseTaskArchiver gseTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseTaskRecordDAO gseTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseScriptAgentTaskRecordDAO")
        public GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseScriptAgentTaskRecordDAO");
            return new GseScriptAgentTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseScriptAgentTaskArchiver gseScriptAgentTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseScriptAgentTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseScriptAgentTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseFileAgentTaskRecordDAO")
        public GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseFileAgentTaskRecordDAO");
            return new GseFileAgentTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseFileAgentTaskArchiver gseFileAgentTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseFileAgentTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseFileAgentTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseScriptExecuteObjTaskRecordDAO")
        public GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseScriptExecuteObjTaskRecordDAO");
            return new GseScriptExecuteObjTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseScriptExecuteObjTaskArchiver gseScriptExecuteObjTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseScriptExecuteObjTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseScriptExecuteObjTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseFileExecuteObjTaskRecordDAO")
        public GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseFileExecuteObjTaskRecordDAO");
            return new GseFileExecuteObjTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseFileExecuteObjTaskArchiver gseFileExecuteObjTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseFileExecuteObjTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseFileExecuteObjTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceRollingTaskRecordDAO")
        public StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceRollingTaskRecordDAO");
            return new StepInstanceRollingTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceRollingTaskArchiver stepInstanceRollingTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceRollingTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceRollingTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "rollingConfigRecordDAO")
        public RollingConfigRecordDAO rollingConfigRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init RollingConfigRecordDAO");
            return new RollingConfigRecordDAO(dslContextProvider);
        }

        @Bean
        public RollingConfigArchiver rollingConfigArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            RollingConfigRecordDAO rollingConfigRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new RollingConfigArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                rollingConfigRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "taskInstanceHostRecordDAO")
        public TaskInstanceHostRecordDAO taskInstanceHostRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init TaskInstanceHostRecordDAO");
            return new TaskInstanceHostRecordDAO(dslContextProvider);
        }

        @Bean
        public TaskInstanceHostArchiver taskInstanceHostArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            TaskInstanceHostRecordDAO taskInstanceHostRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new TaskInstanceHostArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                taskInstanceHostRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean
        public JobInstanceSubTableArchivers jobInstanceSubTableArchivers(
            FileSourceTaskLogArchiver fileSourceTaskLogArchiver,
            GseFileAgentTaskArchiver gseFileAgentTaskArchiver,
            GseFileExecuteObjTaskArchiver gseFileExecuteObjTaskArchiver,
            GseScriptAgentTaskArchiver gseScriptAgentTaskArchiver,
            GseScriptExecuteObjTaskArchiver gseScriptExecuteObjTaskArchiver,
            GseTaskArchiver gseTaskArchiver,
            OperationLogArchiver operationLogArchiver,
            RollingConfigArchiver rollingConfigArchiver,
            StepInstanceArchiver stepInstanceArchiver,
            StepInstanceConfirmArchiver stepInstanceConfirmArchiver,
            StepInstanceFileArchiver stepInstanceFileArchiver,
            StepInstanceScriptArchiver stepInstanceScriptArchiver,
            StepInstanceRollingTaskArchiver stepInstanceRollingTaskArchiver,
            StepInstanceVariableArchiver stepInstanceVariableArchiver,
            TaskInstanceHostArchiver taskInstanceHostArchiver,
            TaskInstanceVariableArchiver taskInstanceVariableArchiver) {
            return new JobInstanceSubTableArchivers(
                fileSourceTaskLogArchiver,
                gseFileAgentTaskArchiver,
                gseFileExecuteObjTaskArchiver,
                gseScriptAgentTaskArchiver,
                gseScriptExecuteObjTaskArchiver,
                gseTaskArchiver,
                operationLogArchiver,
                rollingConfigArchiver,
                stepInstanceArchiver,
                stepInstanceConfirmArchiver,
                stepInstanceFileArchiver,
                stepInstanceScriptArchiver,
                stepInstanceRollingTaskArchiver,
                stepInstanceVariableArchiver,
                taskInstanceHostArchiver,
                taskInstanceVariableArchiver);
        }

    }


    @Bean
    public ArchiveTaskExecuteLock archiveTaskLock(StringRedisTemplate redisTemplate) {
        log.info("Init ArchiveTaskExecuteLock");
        return new ArchiveTaskExecuteLock(redisTemplate);
    }

    @Bean
    public JobInstanceArchiveTaskGenerateLock jobInstanceArchiveTaskGenerateLock(StringRedisTemplate redisTemplate) {
        return new JobInstanceArchiveTaskGenerateLock(redisTemplate);
    }

    @Bean
    public JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator(
        ArchiveTaskService archiveTaskService,
        TaskInstanceRecordDAO taskInstanceRecordDAO,
        ArchiveProperties archiveProperties,
        JobInstanceArchiveTaskGenerateLock jobInstanceArchiveTaskGenerateLock) {

        log.info("Init JobInstanceArchiveTaskGenerator");
        return new JobInstanceArchiveTaskGenerator(
            archiveTaskService,
            taskInstanceRecordDAO,
            archiveProperties,
            jobInstanceArchiveTaskGenerateLock
        );
    }

    @Bean
    public JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler(
        ArchiveTaskService archiveTaskService,
        TaskInstanceRecordDAO taskInstanceRecordDAO,
        ArchiveProperties archiveProperties,
        JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock,
        JobInstanceSubTableArchivers jobInstanceSubTableArchivers,
        ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
        ArchiveTaskExecuteLock archiveTaskExecuteLock,
        ArchiveErrorTaskCounter archiveErrorTaskCounter,
        ArchiveTablePropsStorage archiveTablePropsStorage,
        Tracer tracer) {

        log.info("Init JobInstanceArchiveTaskScheduler");
        return new JobInstanceArchiveTaskScheduler(
            archiveTaskService,
            taskInstanceRecordDAO,
            archiveProperties,
            jobInstanceArchiveTaskScheduleLock,
            jobInstanceSubTableArchivers,
            jobInstanceColdDAOObjectProvider.getIfAvailable(),
            archiveTaskExecuteLock,
            archiveErrorTaskCounter,
            archiveTablePropsStorage,
            tracer
        );
    }

    @Bean
    public JobInstanceArchiveCronJobs jobInstanceArchiveCronJobs(
        JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator,
        JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler,
        ArchiveProperties archiveProperties) {
        log.info("Init JobInstanceArchiveCronJobs");
        return new JobInstanceArchiveCronJobs(
            jobInstanceArchiveTaskGenerator,
            jobInstanceArchiveTaskScheduler,
            archiveProperties
        );
    }

    @Bean
    public JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock() {
        log.info("Init JobInstanceArchiveTaskScheduleLock");
        return new JobInstanceArchiveTaskScheduleLock();
    }
}
