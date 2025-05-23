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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.tencent.bk.job.backup.archive.AbnormalArchiveTaskReScheduler;
import com.tencent.bk.job.backup.archive.ArchiveTablePropsStorage;
import com.tencent.bk.job.backup.archive.JobLogArchiveTaskGenerator;
import com.tencent.bk.job.backup.archive.JobLogArchiveTaskScheduler;
import com.tencent.bk.job.backup.archive.JobLogArchivers;
import com.tencent.bk.job.backup.archive.ArchiveCronJobs;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveTaskGenerator;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveTaskScheduler;
import com.tencent.bk.job.backup.archive.JobInstanceSubTableArchivers;
import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.metrics.ArchiveTasksGauge;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveLogTaskExecuteLock;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveTaskExecuteLock;
import com.tencent.bk.job.backup.archive.util.lock.FailedArchiveTaskRescheduleLock;
import com.tencent.bk.job.backup.archive.util.lock.JobLogArchiveTaskGenerateLock;
import com.tencent.bk.job.backup.archive.util.lock.JobLogArchiveTaskScheduleLock;
import com.tencent.bk.job.backup.archive.util.lock.JobInstanceArchiveTaskGenerateLock;
import com.tencent.bk.job.backup.archive.util.lock.JobInstanceArchiveTaskScheduleLock;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.WatchableThreadPoolExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * job-execute 模块数据归档配置
 */
@Configuration
@EnableScheduling
@Slf4j
@EnableConfigurationProperties({ArchiveProperties.class, JobLogArchiveProperties.class})
@Import({ExecuteHotDbConfiguration.class, ExecuteColdDbConfiguration.class, ExecuteMongoDBConfiguration.class})
public class ArchiveConfiguration {

    @Bean
    @ConditionalOnHistoricDataArchiveEnabled
    public ArchiveTaskExecuteLock archiveTaskLock(StringRedisTemplate redisTemplate) {
        log.info("Init ArchiveTaskExecuteLock");
        return new ArchiveTaskExecuteLock(redisTemplate);
    }

    @Bean
    @ConditionalOnExecuteLogArchiveEnabled
    public ArchiveLogTaskExecuteLock archiveLogTaskLock(StringRedisTemplate redisTemplate) {
        log.info("Init ArchiveLogTaskExecuteLock");
        return new ArchiveLogTaskExecuteLock(redisTemplate);
    }

    @Bean
    @ConditionalOnHistoricDataArchiveEnabled
    public JobInstanceArchiveTaskGenerateLock jobInstanceArchiveTaskGenerateLock(StringRedisTemplate redisTemplate) {
        log.info("Init JobInstanceArchiveTaskGenerateLock");
        return new JobInstanceArchiveTaskGenerateLock(redisTemplate);
    }

    @Bean
    @ConditionalOnAnyArchiveEnabled
    public FailedArchiveTaskRescheduleLock failedArchiveTaskRescheduleLock(StringRedisTemplate redisTemplate) {
        log.info("Init FailedArchiveTaskRescheduleLock");
        return new FailedArchiveTaskRescheduleLock(redisTemplate);
    }

    @Bean
    @ConditionalOnExecuteLogArchiveEnabled
    public JobLogArchiveTaskGenerateLock jobLogArchiveTaskGenerateLock(
        StringRedisTemplate redisTemplate) {
        log.info("Init JobLogArchiveTaskGenerateLock");
        return new JobLogArchiveTaskGenerateLock(redisTemplate);
    }

    @Bean
    @ConditionalOnHistoricDataArchiveEnabled
    public JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator(
        ArchiveTaskService archiveTaskService,
        JobInstanceHotRecordDAO taskInstanceRecordDAO,
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
    @ConditionalOnExecuteLogArchiveEnabled
    public JobLogArchiveTaskGenerator jobLogArchiveTaskGenerator(
        ArchiveTaskService archiveTaskService,
        JobLogArchiveProperties archiveProperties,
        JobLogArchiveTaskGenerateLock jobLogArchiveTaskGenerateLock,
        @Nullable MongoTemplate mongoTemplate) {

        log.info("Init JobLogArchiveTaskGenerator");
        return new JobLogArchiveTaskGenerator(
            archiveTaskService,
            archiveProperties,
            jobLogArchiveTaskGenerateLock,
            mongoTemplate
        );
    }

    @Bean("archiveTaskStopExecutor")
    @ConditionalOnHistoricDataArchiveEnabled
    public ThreadPoolExecutor archiveTaskStopExecutor(MeterRegistry meterRegistry) {
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "archiveTaskStopExecutor",
            5,
            20,
            120L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("archive-task-stop-thread-pool-%d").build()
        );
    }

    @Bean("jobLogArchiveTaskStopExecutor")
    @ConditionalOnExecuteLogArchiveEnabled
    public ThreadPoolExecutor jobLogArchiveTaskStopExecutor(MeterRegistry meterRegistry) {
        return new WatchableThreadPoolExecutor(
            meterRegistry,
            "jobLogArchiveTaskStopExecutor",
            5,
            20,
            120L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("execute-log-archive-task-stop-thread-pool-%d").build()
        );
    }

    @Bean
    @ConditionalOnHistoricDataArchiveEnabled
    public JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler(
        ArchiveTaskService archiveTaskService,
        JobInstanceHotRecordDAO taskInstanceRecordDAO,
        ArchiveProperties archiveProperties,
        JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock,
        JobInstanceSubTableArchivers jobInstanceSubTableArchivers,
        ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
        ArchiveTaskExecuteLock archiveTaskExecuteLock,
        ArchiveErrorTaskCounter archiveErrorTaskCounter,
        ArchiveTablePropsStorage archiveTablePropsStorage,
        Tracer tracer,
        @Qualifier("archiveTaskStopExecutor") ThreadPoolExecutor archiveTaskStopExecutor) {

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
            tracer,
            archiveTaskStopExecutor
        );
    }

    @Bean
    @ConditionalOnExecuteLogArchiveEnabled
    public JobLogArchiveTaskScheduler jobLogArchiveTaskScheduler(
        ArchiveTaskService archiveTaskService,
        JobLogArchiveProperties archiveProperties,
        JobLogArchiveTaskScheduleLock jobLogArchiveTaskScheduleLock,
        JobLogArchivers jobLogArchivers,
        ArchiveLogTaskExecuteLock archiveLogTaskExecuteLock,
        ArchiveErrorTaskCounter archiveErrorTaskCounter,
        Tracer tracer,
        @Qualifier("jobLogArchiveTaskStopExecutor") ThreadPoolExecutor archiveTaskStopExecutor) {
        log.info("Init JobLogArchiveTaskScheduler");
        return new JobLogArchiveTaskScheduler(
            archiveTaskService,
            archiveProperties,
            jobLogArchiveTaskScheduleLock,
            jobLogArchivers,
            archiveLogTaskExecuteLock,
            archiveErrorTaskCounter,
            tracer,
            archiveTaskStopExecutor
        );
    }

    @Bean
    @ConditionalOnAnyArchiveEnabled
    public ArchiveCronJobs archiveCronJobs(
        @Nullable JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator,
        @Nullable JobLogArchiveTaskGenerator jobLogArchiveTaskGenerator,
        @Nullable JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler,
        @Nullable JobLogArchiveTaskScheduler jobLogArchiveTaskScheduler,
        ArchiveProperties archiveProperties,
        JobLogArchiveProperties jobLogArchiveProperties,
        AbnormalArchiveTaskReScheduler abnormalArchiveTaskReScheduler) {
        log.info("Init ArchiveCronJobs");
        return new ArchiveCronJobs(
            jobInstanceArchiveTaskGenerator,
            jobLogArchiveTaskGenerator,
            jobInstanceArchiveTaskScheduler,
            jobLogArchiveTaskScheduler,
            archiveProperties,
            jobLogArchiveProperties,
            abnormalArchiveTaskReScheduler
        );
    }

    @Bean
    @ConditionalOnHistoricDataArchiveEnabled
    public JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock() {
        log.info("Init JobInstanceArchiveTaskScheduleLock");
        return new JobInstanceArchiveTaskScheduleLock();
    }

    @Bean
    @ConditionalOnExecuteLogArchiveEnabled
    public JobLogArchiveTaskScheduleLock jobLogArchiveTaskScheduleLock() {
        log.info("Init JobLogArchiveTaskScheduleLock");
        return new JobLogArchiveTaskScheduleLock();
    }

    @Bean
    @ConditionalOnAnyArchiveEnabled
    public AbnormalArchiveTaskReScheduler failArchiveTaskReScheduler(
        ArchiveTaskService archiveTaskService,
        FailedArchiveTaskRescheduleLock failedArchiveTaskRescheduleLock) {
        log.info("Init FailArchiveTaskReScheduler");
        return new AbnormalArchiveTaskReScheduler(archiveTaskService, failedArchiveTaskRescheduleLock);
    }

    @Bean
    @ConditionalOnHistoricDataArchiveEnabled
    public ArchiveTasksGauge archiveTasksGauge(MeterRegistry meterRegistry,
                                               ArchiveTaskService archiveTaskService) {
        return new ArchiveTasksGauge(meterRegistry, archiveTaskService);
    }

    @Bean
    @ConditionalOnHistoricDataArchiveEnabled
    public ArchiveTablePropsStorage archiveTablePropsStorage(ArchiveProperties archiveProperties) {
        log.info("Init ArchiveTablePropsStorage");
        return new ArchiveTablePropsStorage(archiveProperties);
    }

    /**
     * 条件注解：控制历史数据归档相关bean的注入
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @ConditionalOnProperty(
        value = "job.backup.archive.execute.enabled",
        havingValue = "true"
    )
    public @interface ConditionalOnHistoricDataArchiveEnabled {
    }

    /**
     * 条件注解：控制执行日志归档相关bean的注入
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @ConditionalOnProperty(
        value = "job.backup.archive.execute-log.enabled",
        havingValue = "true"
    )
    public @interface ConditionalOnExecuteLogArchiveEnabled {
    }

    /**
     * 条件注解：控制归档任务公共bean的注入
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Conditional(AnyArchiveEnabledCondition.class)
    public @interface ConditionalOnAnyArchiveEnabled {
    }
    static class AnyArchiveEnabledCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment env = context.getEnvironment();
            return env.getProperty("job.backup.archive.execute.enabled", Boolean.class, false)
                || env.getProperty("job.backup.archive.execute-log.enabled", Boolean.class, false);
        }
    }
}
