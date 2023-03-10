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

package com.tencent.bk.job.crontab.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Quartz 设置
 */
@Configuration
@Profile("!test")
public class QuartzConfig {

    private final JobQuartzProperties jobQuartzProperties;

    @Autowired
    public QuartzConfig(JobQuartzProperties quartzProperties) {
        this.jobQuartzProperties = quartzProperties;
    }

    @Bean("quartzTaskExecutor")
    public ThreadPoolTaskExecutor quartzTaskExecutor() {
        JobQuartzProperties.ThreadPool threadPoolConfig = jobQuartzProperties.getThreadPool();
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix(threadPoolConfig.getThreadNamePrefix());
        threadPoolTaskExecutor.setThreadGroupName(threadPoolConfig.getThreadGroupName());
        threadPoolTaskExecutor.setDaemon(threadPoolConfig.isDaemon());
        threadPoolTaskExecutor.setThreadPriority(threadPoolConfig.getThreadPriority());
        threadPoolTaskExecutor.setCorePoolSize(threadPoolConfig.getCorePoolSize());
        threadPoolTaskExecutor.setMaxPoolSize(threadPoolConfig.getMaxPoolSize());
        threadPoolTaskExecutor.setQueueCapacity(threadPoolConfig.getQueueCapacity());
        threadPoolTaskExecutor.setKeepAliveSeconds(threadPoolConfig.getKeepAliveSeconds());
        threadPoolTaskExecutor.setAllowCoreThreadTimeOut(threadPoolConfig.isAllowCoreThreadTimeOut());
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(
            threadPoolConfig.isWaitForTasksToCompleteOnShutdown());
        threadPoolTaskExecutor.setAwaitTerminationSeconds(
            threadPoolConfig.getAwaitTerminationSeconds());

        return threadPoolTaskExecutor;
    }

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(
        @Qualifier("quartzTaskExecutor") ThreadPoolTaskExecutor quartzTaskExecutor) {
        return schedulerFactoryBean -> {
            // 自定义taskExecutor
            schedulerFactoryBean.setTaskExecutor(quartzTaskExecutor);

            // 自定义scheduler
            schedulerFactoryBean.setSchedulerName(jobQuartzProperties.getScheduler().getSchedulerName());
            schedulerFactoryBean.setApplicationContextSchedulerContextKey(
                jobQuartzProperties.getScheduler().getApplicationContextSchedulerContextKey());
            schedulerFactoryBean.setOverwriteExistingJobs(jobQuartzProperties.getScheduler().isOverwriteExistingJobs());
            schedulerFactoryBean.setAutoStartup(jobQuartzProperties.getScheduler().isAutoStartup());
            schedulerFactoryBean.setStartupDelay(jobQuartzProperties.getScheduler().getStartupDelay());
        };

    }
}
