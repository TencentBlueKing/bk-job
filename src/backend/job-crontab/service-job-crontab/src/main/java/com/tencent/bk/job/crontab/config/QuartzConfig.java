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

import com.tencent.bk.job.crontab.timer.AutowiredSpringBeanJobFactory;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * @since 14/1/2020 16:57
 */
@Configuration
@Profile({"prod", "dev"})
public class QuartzConfig {

    private final PlatformTransactionManager transactionManager;
    private final QuartzProperties quartzProperties;
    private final DataSource dataSource;
    private final AutowiredSpringBeanJobFactory autowiredSpringBeanJobFactory;

    @Autowired
    public QuartzConfig(PlatformTransactionManager transactionManager, QuartzProperties quartzProperties,
                        @Qualifier("job-crontab-data-source") DataSource dataSource,
                        AutowiredSpringBeanJobFactory autowiredSpringBeanJobFactory) {
        this.transactionManager = transactionManager;
        this.quartzProperties = quartzProperties;
        this.dataSource = dataSource;
        this.autowiredSpringBeanJobFactory = autowiredSpringBeanJobFactory;
    }

    @Bean
    public ThreadPoolTaskExecutor quartzTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(quartzProperties.getThreadPool().getCorePoolSize());
        threadPoolTaskExecutor.setMaxPoolSize(quartzProperties.getThreadPool().getMaxPoolSize());
        threadPoolTaskExecutor.setQueueCapacity(quartzProperties.getThreadPool().getQueueCapacity());
        threadPoolTaskExecutor.setKeepAliveSeconds(quartzProperties.getThreadPool().getKeepAliveSeconds());
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(
            quartzProperties.getThreadPool().isWaitForTasksToCompleteOnShutdown());
        threadPoolTaskExecutor.setAwaitTerminationSeconds(
            quartzProperties.getThreadPool().getAwaitTerminationSeconds());

        return threadPoolTaskExecutor;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setConfigLocation(quartzProperties.getScheduler().getConfigLocation());
        // 此处设置数据源之后，会覆盖quartz.properties中的myDS数据源
        schedulerFactoryBean.setDataSource(dataSource);
        schedulerFactoryBean.setJobFactory(autowiredSpringBeanJobFactory);
        schedulerFactoryBean.setSchedulerName(quartzProperties.getScheduler().getSchedulerName());
        schedulerFactoryBean.setTaskExecutor(quartzTaskExecutor());
        schedulerFactoryBean.setTransactionManager(transactionManager);
        schedulerFactoryBean.setApplicationContextSchedulerContextKey(
            quartzProperties.getScheduler().getApplicationContextSchedulerContextKey());
        schedulerFactoryBean.setOverwriteExistingJobs(quartzProperties.getScheduler().isOverwriteExistingJobs());
        schedulerFactoryBean.setAutoStartup(quartzProperties.getScheduler().isAutoStartup());
        schedulerFactoryBean.setStartupDelay(quartzProperties.getScheduler().getStartupDelay());
        schedulerFactoryBean.setQuartzProperties(asProperties(quartzProperties.getProperties()));

        return schedulerFactoryBean;
    }

    @Bean
    public Scheduler scheduler() {
        return schedulerFactoryBean().getObject();
    }

    private Properties asProperties(Map<String, String> source) {
        Properties properties = new Properties();
        properties.putAll(source);
        return properties;
    }
}
