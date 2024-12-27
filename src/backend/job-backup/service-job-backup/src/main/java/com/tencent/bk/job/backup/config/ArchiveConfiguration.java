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
import com.tencent.bk.job.backup.archive.JobInstanceArchiveCronJobs;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveTaskGenerator;
import com.tencent.bk.job.backup.archive.JobInstanceArchiveTaskScheduler;
import com.tencent.bk.job.backup.archive.JobInstanceSubTableArchivers;
import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.metrics.ArchiveTasksGauge;
import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.archive.util.lock.ArchiveTaskExecuteLock;
import com.tencent.bk.job.backup.archive.util.lock.FailedArchiveTaskRescheduleLock;
import com.tencent.bk.job.backup.archive.util.lock.JobInstanceArchiveTaskGenerateLock;
import com.tencent.bk.job.backup.archive.util.lock.JobInstanceArchiveTaskScheduleLock;
import com.tencent.bk.job.backup.metrics.ArchiveErrorTaskCounter;
import com.tencent.bk.job.common.WatchableThreadPoolExecutor;
import io.micrometer.core.instrument.MeterRegistry;
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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    @Bean
    public ArchiveTaskExecuteLock archiveTaskLock(StringRedisTemplate redisTemplate) {
        log.info("Init ArchiveTaskExecuteLock");
        return new ArchiveTaskExecuteLock(redisTemplate);
    }

    @Bean
    public JobInstanceArchiveTaskGenerateLock jobInstanceArchiveTaskGenerateLock(StringRedisTemplate redisTemplate) {
        log.info("Init JobInstanceArchiveTaskGenerateLock");
        return new JobInstanceArchiveTaskGenerateLock(redisTemplate);
    }

    @Bean
    public FailedArchiveTaskRescheduleLock failedArchiveTaskRescheduleLock(StringRedisTemplate redisTemplate) {
        log.info("Init FailedArchiveTaskRescheduleLock");
        return new FailedArchiveTaskRescheduleLock(redisTemplate);
    }

    @Bean
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

    @Bean("archiveTaskStopExecutor")
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

    @Bean
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
    public JobInstanceArchiveCronJobs jobInstanceArchiveCronJobs(
        JobInstanceArchiveTaskGenerator jobInstanceArchiveTaskGenerator,
        JobInstanceArchiveTaskScheduler jobInstanceArchiveTaskScheduler,
        ArchiveProperties archiveProperties,
        AbnormalArchiveTaskReScheduler abnormalArchiveTaskReScheduler) {
        log.info("Init JobInstanceArchiveCronJobs");
        return new JobInstanceArchiveCronJobs(
            jobInstanceArchiveTaskGenerator,
            jobInstanceArchiveTaskScheduler,
            archiveProperties,
            abnormalArchiveTaskReScheduler
        );
    }

    @Bean
    public JobInstanceArchiveTaskScheduleLock jobInstanceArchiveTaskScheduleLock() {
        log.info("Init JobInstanceArchiveTaskScheduleLock");
        return new JobInstanceArchiveTaskScheduleLock();
    }

    @Bean
    public AbnormalArchiveTaskReScheduler failArchiveTaskReScheduler(
        ArchiveTaskService archiveTaskService,
        FailedArchiveTaskRescheduleLock failedArchiveTaskRescheduleLock) {
        log.info("Init FailArchiveTaskReScheduler");
        return new AbnormalArchiveTaskReScheduler(archiveTaskService, failedArchiveTaskRescheduleLock);
    }

    @Bean
    public ArchiveTasksGauge archiveTasksGauge(MeterRegistry meterRegistry,
                                               ArchiveTaskService archiveTaskService) {
        return new ArchiveTasksGauge(meterRegistry, archiveTaskService);
    }

    @Bean
    public ArchiveTablePropsStorage archiveTablePropsStorage(ArchiveProperties archiveProperties) {
        log.info("Init ArchiveTablePropsStorage");
        return new ArchiveTablePropsStorage(archiveProperties);
    }
}
