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

package com.tencent.bk.job.common.service.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 定制 ThreadPoolTaskScheduler 的拒绝策略，
 * 避免停机阶段 Spring Integration 等框架内部组件提交任务被拒绝时产生 ERROR 日志。
 * <p>
 * ScheduledThreadPoolExecutor 使用无界延迟队列，正常运行期间不会触发拒绝策略，
 * 仅在 executor shutdown 后提交的任务才会被拒绝，此时以 INFO 级别记录后丢弃。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class JobTaskSchedulerAutoConfiguration {

    @Bean
    public ThreadPoolTaskSchedulerCustomizer gracefulShutdownTaskSchedulerCustomizer() {
        return scheduler -> scheduler.setRejectedExecutionHandler(new ShutdownAwareDiscardPolicy());
    }

    /**
     * 停机感知的拒绝策略：executor 已关闭时以 INFO 级别记录并丢弃任务，未关闭时沿用默认的 AbortPolicy 行为。
     */
    static class ShutdownAwareDiscardPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (executor.isShutdown()) {
                log.info("Task rejected after scheduler shutdown, discarded: {}", r);
                return;
            }
            new ThreadPoolExecutor.AbortPolicy().rejectedExecution(r, executor);
        }
    }
}
