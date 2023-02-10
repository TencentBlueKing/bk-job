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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

/**
 * Job 扩展的quartz配置
 */
@Data
@Profile({"prod", "dev"})
@ConfigurationProperties(prefix = "spring.quartz")
public class JobQuartzProperties {
    /**
     * 线程池配置
     */
    private ThreadPool threadPool = new ThreadPool();

    /**
     * Scheduler 配置
     */
    private Scheduler scheduler = new Scheduler();

    public JobQuartzProperties() {

    }

    @Data
    public static class ThreadPool {
        private String threadNamePrefix;

        private int threadPriority = 5;

        private boolean daemon = false;

        private String threadGroupName;

        private int corePoolSize = 10;

        private int maxPoolSize = Integer.MAX_VALUE;

        private int keepAliveSeconds = 60;

        private int queueCapacity = Integer.MAX_VALUE;

        private boolean allowCoreThreadTimeOut = false;

        private boolean waitForTasksToCompleteOnShutdown = false;

        private int awaitTerminationSeconds = 0;
    }

    @Data
    public static class Scheduler {

        private String schedulerName;

        private String applicationContextSchedulerContextKey = "applicationContext";

        private boolean overwriteExistingJobs = true;

        private boolean autoStartup = true;

        private int startupDelay = 5;
    }
}
