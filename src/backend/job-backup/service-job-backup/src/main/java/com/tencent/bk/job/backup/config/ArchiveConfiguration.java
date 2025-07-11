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

package com.tencent.bk.job.backup.config;

import com.tencent.bk.job.backup.archive.ArchiveTaskLock;
import com.tencent.bk.job.backup.archive.JobExecuteArchiveManage;
import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import com.tencent.bk.job.backup.dao.impl.ExecuteArchiveDAOImpl;
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
import com.tencent.bk.job.backup.service.ArchiveProgressService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;

/**
 * job-execute 模块数据归档配置
 */
@Configuration
@EnableScheduling
@Slf4j
@EnableConfigurationProperties(ArchiveDBProperties.class)
@Import({ExecuteDbConfiguration.class, ExecuteBackupDbConfiguration.class})
public class ArchiveConfiguration {

    /**
     * job-execute DB 配置
     */
    @Configuration
    @ConditionalOnExpression("${job.backup.archive.execute.enabled:false}")
    public static class ExecuteDaoAutoConfig {

        @Bean(name = "taskInstanceRecordDAO")
        public TaskInstanceRecordDAO taskInstanceRecordDAO(@Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init TaskInstanceRecordDAO");
            return new TaskInstanceRecordDAO(context);
        }

        @Bean(name = "stepInstanceRecordDAO")
        public StepInstanceRecordDAO stepInstanceRecordDAO(@Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init StepInstanceRecordDAO");
            return new StepInstanceRecordDAO(context);
        }

        @Bean(name = "stepInstanceScriptRecordDAO")
        public StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init StepInstanceScriptRecordDAO");
            return new StepInstanceScriptRecordDAO(context);
        }

        @Bean(name = "stepInstanceFileRecordDAO")
        public StepInstanceFileRecordDAO stepInstanceFileRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init StepInstanceFileRecordDAO");
            return new StepInstanceFileRecordDAO(context);
        }

        @Bean(name = "stepInstanceConfirmRecordDAO")
        public StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init StepInstanceConfirmRecordDAO");
            return new StepInstanceConfirmRecordDAO(context);
        }

        @Bean(name = "stepInstanceVariableRecordDAO")
        public StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init StepInstanceVariableRecordDAO");
            return new StepInstanceVariableRecordDAO(context);
        }

        @Bean(name = "taskInstanceVariableRecordDAO")
        public TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init TaskInstanceVariableRecordDAO");
            return new TaskInstanceVariableRecordDAO(context);
        }

        @Bean(name = "operationLogRecordDAO")
        public OperationLogRecordDAO operationLogRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init OperationLogRecordDAO");
            return new OperationLogRecordDAO(context);
        }

        @Bean(name = "fileSourceTaskLogRecordDAO")
        public FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init FileSourceTaskRecordDAO");
            return new FileSourceTaskLogRecordDAO(context);
        }

        @Bean(name = "gseTaskRecordDAO")
        public GseTaskRecordDAO gseTaskRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init GseTaskRecordDAO");
            return new GseTaskRecordDAO(context);
        }

        @Bean(name = "gseScriptAgentTaskRecordDAO")
        public GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init GseScriptAgentTaskRecordDAO");
            return new GseScriptAgentTaskRecordDAO(context);
        }

        @Bean(name = "gseFileAgentTaskRecordDAO")
        public GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init GseFileAgentTaskRecordDAO");
            return new GseFileAgentTaskRecordDAO(context);
        }

        @Bean(name = "gseScriptExecuteObjTaskRecordDAO")
        public GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init GseScriptExecuteObjTaskRecordDAO");
            return new GseScriptExecuteObjTaskRecordDAO(context);
        }

        @Bean(name = "gseFileExecuteObjTaskRecordDAO")
        public GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init GseFileExecuteObjTaskRecordDAO");
            return new GseFileExecuteObjTaskRecordDAO(context);
        }

        @Bean(name = "stepInstanceRollingTaskRecordDAO")
        public StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init StepInstanceRollingTaskRecordDAO");
            return new StepInstanceRollingTaskRecordDAO(context);
        }

        @Bean(name = "rollingConfigRecordDAO")
        public RollingConfigRecordDAO rollingConfigRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init RollingConfigRecordDAO");
            return new RollingConfigRecordDAO(context);
        }

        @Bean(name = "taskInstanceHostRecordDAO")
        public TaskInstanceHostRecordDAO taskInstanceHostRecordDAO(
            @Qualifier("job-execute-dsl-context") DSLContext context) {
            log.info("Init TaskInstanceHostRecordDAO");
            return new TaskInstanceHostRecordDAO(context);
        }

    }

    /**
     * job-execute 归档数据备份 DB 配置
     */
    @Configuration
    @Conditional(ExecuteBackupDbConfiguration.JobExecuteBackupDbInitCondition.class)
    public static class ExecuteBackupDAOConfig {
        @Bean(name = "execute-archive-dao")
        public ExecuteArchiveDAO executeArchiveDAO(@Qualifier("job-execute-archive-dsl-context") DSLContext context) {
            log.info("Init ExecuteArchiveDAO");
            return new ExecuteArchiveDAOImpl(context);
        }
    }

    @Bean
    @ConditionalOnExpression("${job.backup.archive.execute.enabled:false}")
    public ArchiveTaskLock archiveTaskLock(StringRedisTemplate redisTemplate) {
        log.info("Init ArchiveTaskLock");
        return new ArchiveTaskLock(redisTemplate);
    }


    @Bean
    @ConditionalOnExpression("${job.backup.archive.execute.enabled:false}")
    public JobExecuteArchiveManage jobExecuteArchiveManage(
        ObjectProvider<TaskInstanceRecordDAO> taskInstanceRecordDAOObjectProvider,
        ObjectProvider<StepInstanceRecordDAO> stepInstanceRecordDAOObjectProvider,
        ObjectProvider<StepInstanceScriptRecordDAO> stepInstanceScriptRecordDAOObjectProvider,
        ObjectProvider<StepInstanceFileRecordDAO> stepInstanceFileRecordDAOObjectProvider,
        ObjectProvider<StepInstanceConfirmRecordDAO> stepInstanceConfirmRecordDAOObjectProvider,
        ObjectProvider<StepInstanceVariableRecordDAO> stepInstanceVariableRecordDAOObjectProvider,
        ObjectProvider<TaskInstanceVariableRecordDAO> taskInstanceVariableRecordDAOObjectProvider,
        ObjectProvider<OperationLogRecordDAO> operationLogRecordDAOObjectProvider,
        ObjectProvider<FileSourceTaskLogRecordDAO> fileSourceTaskLogRecordDAOObjectProvider,
        ObjectProvider<GseTaskRecordDAO> gseTaskRecordDAOObjectProvider,
        ObjectProvider<GseScriptAgentTaskRecordDAO> gseScriptAgentTaskRecordDAOObjectProvider,
        ObjectProvider<GseFileAgentTaskRecordDAO> gseFileAgentTaskRecordDAOObjectProvider,
        ObjectProvider<GseScriptExecuteObjTaskRecordDAO> gseScriptExecuteObjTaskRecordDAOObjectProvider,
        ObjectProvider<GseFileExecuteObjTaskRecordDAO> gseFileExecuteObjTaskRecordDAOObjectProvider,
        ObjectProvider<StepInstanceRollingTaskRecordDAO> stepInstanceRollingTaskRecordDAOObjectProvider,
        ObjectProvider<RollingConfigRecordDAO> rollingConfigRecordDAOObjectProvider,
        ObjectProvider<TaskInstanceHostRecordDAO> taskInstanceHostRecordDAOObjectProvider,
        ObjectProvider<ExecuteArchiveDAO> executeArchiveDAOObjectProvider,
        ArchiveProgressService archiveProgressService,
        @Qualifier("archiveExecutor") ExecutorService archiveExecutor,
        ArchiveDBProperties archiveDBProperties,
        ArchiveTaskLock archiveTaskLock,
        ArchiveErrorTaskCounter archiveErrorTaskCounter) {

        log.info("Init JobExecuteArchiveManage");
        return new JobExecuteArchiveManage(
            taskInstanceRecordDAOObjectProvider.getIfAvailable(),
            stepInstanceRecordDAOObjectProvider.getIfAvailable(),
            stepInstanceScriptRecordDAOObjectProvider.getIfAvailable(),
            stepInstanceFileRecordDAOObjectProvider.getIfAvailable(),
            stepInstanceConfirmRecordDAOObjectProvider.getIfAvailable(),
            stepInstanceVariableRecordDAOObjectProvider.getIfAvailable(),
            taskInstanceVariableRecordDAOObjectProvider.getIfAvailable(),
            operationLogRecordDAOObjectProvider.getIfAvailable(),
            fileSourceTaskLogRecordDAOObjectProvider.getIfAvailable(),
            gseTaskRecordDAOObjectProvider.getIfAvailable(),
            gseScriptAgentTaskRecordDAOObjectProvider.getIfAvailable(),
            gseFileAgentTaskRecordDAOObjectProvider.getIfAvailable(),
            gseScriptExecuteObjTaskRecordDAOObjectProvider.getIfAvailable(),
            gseFileExecuteObjTaskRecordDAOObjectProvider.getIfAvailable(),
            stepInstanceRollingTaskRecordDAOObjectProvider.getIfAvailable(),
            rollingConfigRecordDAOObjectProvider.getIfAvailable(),
            taskInstanceHostRecordDAOObjectProvider.getIfAvailable(),
            executeArchiveDAOObjectProvider.getIfAvailable(),
            archiveProgressService,
            archiveDBProperties,
            archiveExecutor,
            archiveTaskLock,
            archiveErrorTaskCounter);
    }
}
