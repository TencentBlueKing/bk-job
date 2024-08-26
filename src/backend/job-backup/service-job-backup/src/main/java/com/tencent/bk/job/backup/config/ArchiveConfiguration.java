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

import com.tencent.bk.job.backup.archive.ArchiveTaskLock;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveTaskScheduleLock;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveTaskGenerator;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveTaskScheduler;
import com.tencent.bk.job.backup.archive.dao.ArchiveTaskDAO;
import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.impl.FileSourceTaskLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseScriptAgentTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseScriptExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceColdDAOImpl;
import com.tencent.bk.job.backup.archive.dao.impl.OperationLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.RollingConfigRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceConfirmRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceFileRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceRollingTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceScriptRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceVariableRecordDAO;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingProperties;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
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
@Import({ExecuteDbConfiguration.class, ExecuteBackupDbConfiguration.class})
public class ArchiveConfiguration {

    /**
     * job-execute DB 配置
     */
    @Configuration
    @ConditionalOnExpression("${job.backup.archive.execute.enabled:false}")
    public static class ExecuteDaoAutoConfig {

        @Bean(name = "jobExecuteDSLContext")
        public DSLContext jobExecuteDSLContext(
            @Autowired ObjectProvider<ShardingProperties> shardingPropertiesObjectProperty,
            @Autowired(required = false)
            @Qualifier("job-execute-dsl-context") DSLContext noShardingDSLContext,
            @Autowired(required = false)
            @Qualifier("job-sharding-dsl-context") DSLContext shardingDSLContext
        ) {
            log.info("Init JobExecuteDSLContext");
            ShardingProperties shardingProperties =
                shardingPropertiesObjectProperty.getIfAvailable();
            boolean shardingEnabled = shardingProperties != null && shardingProperties.isEnabled();

            if (shardingEnabled) {
                if (shardingDSLContext == null) {
                    log.error("JobExecuteShardingDSLContext empty");
                    throw new BeanCreationException("JobExecuteShardingDSLContextEmpty");
                }
                return shardingDSLContext;
            } else {
                if (noShardingDSLContext == null) {
                    log.error("JobExecuteNoShardingDSLContext empty");
                    throw new BeanCreationException("JobExecuteNoShardingDSLContextEmpty");
                }
                return noShardingDSLContext;
            }
        }

        @Bean(name = "taskInstanceRecordDAO")
        public TaskInstanceRecordDAO taskInstanceRecordDAO(@Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init TaskInstanceRecordDAO");
            return new TaskInstanceRecordDAO(context);
        }

        @Bean(name = "stepInstanceRecordDAO")
        public StepInstanceRecordDAO stepInstanceRecordDAO(@Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init StepInstanceRecordDAO");
            return new StepInstanceRecordDAO(context);
        }

        @Bean(name = "stepInstanceScriptRecordDAO")
        public StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init StepInstanceScriptRecordDAO");
            return new StepInstanceScriptRecordDAO(context);
        }

        @Bean(name = "stepInstanceFileRecordDAO")
        public StepInstanceFileRecordDAO stepInstanceFileRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init StepInstanceFileRecordDAO");
            return new StepInstanceFileRecordDAO(context);
        }

        @Bean(name = "stepInstanceConfirmRecordDAO")
        public StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init StepInstanceConfirmRecordDAO");
            return new StepInstanceConfirmRecordDAO(context);
        }

        @Bean(name = "stepInstanceVariableRecordDAO")
        public StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init StepInstanceVariableRecordDAO");
            return new StepInstanceVariableRecordDAO(context);
        }

        @Bean(name = "taskInstanceVariableRecordDAO")
        public TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init TaskInstanceVariableRecordDAO");
            return new TaskInstanceVariableRecordDAO(context);
        }

        @Bean(name = "operationLogRecordDAO")
        public OperationLogRecordDAO operationLogRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init OperationLogRecordDAO");
            return new OperationLogRecordDAO(context);
        }

        @Bean(name = "fileSourceTaskLogRecordDAO")
        public FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init FileSourceTaskRecordDAO");
            return new FileSourceTaskLogRecordDAO(context);
        }

        @Bean(name = "gseTaskRecordDAO")
        public GseTaskRecordDAO gseTaskRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init GseTaskRecordDAO");
            return new GseTaskRecordDAO(context);
        }

        @Bean(name = "gseScriptAgentTaskRecordDAO")
        public GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init GseScriptAgentTaskRecordDAO");
            return new GseScriptAgentTaskRecordDAO(context);
        }

        @Bean(name = "gseFileAgentTaskRecordDAO")
        public GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init GseFileAgentTaskRecordDAO");
            return new GseFileAgentTaskRecordDAO(context);
        }

        @Bean(name = "gseScriptExecuteObjTaskRecordDAO")
        public GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init GseScriptExecuteObjTaskRecordDAO");
            return new GseScriptExecuteObjTaskRecordDAO(context);
        }

        @Bean(name = "gseFileExecuteObjTaskRecordDAO")
        public GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init GseFileExecuteObjTaskRecordDAO");
            return new GseFileExecuteObjTaskRecordDAO(context);
        }

        @Bean(name = "stepInstanceRollingTaskRecordDAO")
        public StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init StepInstanceRollingTaskRecordDAO");
            return new StepInstanceRollingTaskRecordDAO(context);
        }

        @Bean(name = "rollingConfigRecordDAO")
        public RollingConfigRecordDAO rollingConfigRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
            log.info("Init RollingConfigRecordDAO");
            return new RollingConfigRecordDAO(context);
        }

        @Bean(name = "taskInstanceHostRecordDAO")
        public TaskInstanceHostRecordDAO taskInstanceHostRecordDAO(
            @Qualifier("jobExecuteDSLContext") DSLContext context) {
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
        public JobInstanceColdDAO executeArchiveDAO(@Qualifier("job-execute-archive-dsl-context") DSLContext context) {
            log.info("Init JobInstanceColdDAO");
            return new JobInstanceColdDAOImpl(context);
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
    public JobInstanceArchiveTaskScheduleLock archiveTaskScheduleLock() {
        log.info("Init ArchiveTaskScheduleLock");
        return new JobInstanceArchiveTaskScheduleLock();
    }

    @Bean
    @ConditionalOnExpression("${job.backup.archive.execute.enabled:false}")
    public JobInstanceArchiveTaskGenerator archiveTaskGenerator(
        ArchiveTaskDAO archiveTaskDAO,
        TaskInstanceRecordDAO taskInstanceRecordDAO,
        ArchiveProperties archiveProperties,
        ObjectProvider<ShardingProperties> shardingPropertiesObjectProvider) {
        log.info("Init ArchiveTaskGenerator");
        return new JobInstanceArchiveTaskGenerator(
            archiveTaskDAO,
            taskInstanceRecordDAO,
            archiveProperties,
            shardingPropertiesObjectProvider.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnExpression("${job.backup.archive.execute.enabled:false}")
    public JobInstanceArchiveTaskScheduler archiveTaskScheduler(
        ArchiveTaskDAO archiveTaskDAO,
        TaskInstanceRecordDAO taskInstanceRecordDAO,
        ArchiveProperties archiveProperties,
        ObjectProvider<ShardingProperties> shardingPropertiesObjectProvider,
        JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock,
        JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator) {
        log.info("Init ArchiveTaskScheduler");
        return new JobInstanceArchiveTaskScheduler(
            archiveTaskDAO,
            taskInstanceRecordDAO,
            archiveProperties,
            shardingPropertiesObjectProvider.getIfAvailable(),
            jobInstanceArchiveTaskScheduleLock,
            jobInstanceArchiveTaskGenerator
        );
    }


    @Bean
    @ConditionalOnExpression("${job.backup.archive.execute.enabled:false}")
    public JobInstanceArchiveTaskScheduler jobInstanceHotDataArchiveTaskScheduler(
        ArchiveTaskDAO archiveTaskDAO,
        TaskInstanceRecordDAO taskInstanceRecordDAO,
        ArchiveProperties archiveProperties,
        ShardingProperties shardingProperties,
        JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock,
        JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator) {

        log.info("Init JobExecuteArchiveManage");
        return new JobInstanceArchiveTaskScheduler(
            archiveTaskDAO,
            taskInstanceRecordDAO,
            archiveProperties,
            shardingProperties,
            jobInstanceArchiveTaskScheduleLock,
            jobInstanceArchiveTaskGenerator);
    }
}
