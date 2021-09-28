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

package com.tencent.bk.job.execute.engine.result;

import com.tencent.bk.job.execute.monitor.ExecuteMetricNames;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToDoubleFunction;

/**
 * 任务结果处理采样
 */
@Slf4j
@Component
public class ResultHandleTaskSampler {
    private final MeterRegistry meterRegistry;
    /**
     * 正在处理任务结果的文件任务数
     */
    private final AtomicLong handlingFileTasks = new AtomicLong(0);
    /**
     * 正在处理任务结果的文件任务数（各业务）
     */
    private final ConcurrentHashMap<Long, AtomicLong> handlingFileTasksMap = new ConcurrentHashMap<>();
    /**
     * 正在处理任务结果的脚本任务数
     */
    private final AtomicLong handlingScriptTasks = new AtomicLong(0);
    /**
     * 正在处理任务结果的脚本任务数（各业务）
     */
    private final ConcurrentHashMap<Long, AtomicLong> handlingScriptTasksMap = new ConcurrentHashMap<>();
    /**
     * 接收的结果处理任务数
     */
    private final AtomicLong receiveTaskCounter = new AtomicLong(0);
    /**
     * 接收的结果处理任务数（各业务）
     */
    private final ConcurrentHashMap<Long, AtomicLong> receiveTaskCounterMap = new ConcurrentHashMap<>();
    /**
     * 完成的文件任务数
     */
    private final AtomicLong finishedFileTaskCounter = new AtomicLong(0);
    /**
     * 完成的文件任务数（各业务）
     */
    private final ConcurrentHashMap<Long, AtomicLong> finishedFileTaskCounterMap = new ConcurrentHashMap<>();
    /**
     * 完成的脚本任务数
     */
    private final AtomicLong finishedScriptTaskCounter = new AtomicLong(0);
    /**
     * 完成的脚本任务数（各业务）
     */
    private final ConcurrentHashMap<Long, AtomicLong> finishedScriptTaskCounterMap = new ConcurrentHashMap<>();

    @Autowired
    public ResultHandleTaskSampler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    class UpdateStatisticsThread extends Thread {

        boolean runFlag = true;

        private ToDoubleFunction<AtomicLong> getFunctionByCounter(AtomicLong counter) {
            return new ToDoubleFunction() {
                @Override
                public double applyAsDouble(Object value) {
                    return counter.doubleValue();
                }
            };
        }

        private void measureHandlingFileTasksByApp() {
            handlingFileTasksMap.forEach((appId, counter) -> {
                meterRegistry.gauge(
                    ExecuteMetricNames.GSE_RUNNING_TASKS_APP,
                    Arrays.asList(
                        Tag.of("appId", appId.toString()),
                        Tag.of("stage", "result-handle"),
                        Tag.of("type", "file")
                    ),
                    counter,
                    getFunctionByCounter(counter)
                );
            });
        }

        private void measureHandlingScriptTasksByApp() {
            handlingScriptTasksMap.forEach((appId, counter) -> {
                meterRegistry.gauge(
                    ExecuteMetricNames.GSE_RUNNING_TASKS_APP,
                    Arrays.asList(
                        Tag.of("appId", appId.toString()),
                        Tag.of("stage", "result-handle"),
                        Tag.of("type", "script")
                    ),
                    counter,
                    getFunctionByCounter(counter)
                );
            });
        }

        private void measureReceiveTasksByApp() {
            receiveTaskCounterMap.forEach((appId, counter) -> {
                meterRegistry.gauge(
                    ExecuteMetricNames.GSE_RUNNING_TASKS_APP,
                    Arrays.asList(
                        Tag.of("appId", appId.toString()),
                        Tag.of("stage", "result-handle"),
                        Tag.of("type", "all-since-job-boot")
                    ),
                    counter,
                    getFunctionByCounter(counter)
                );
            });
        }

        private void measureFinishedFileTasksByApp() {
            finishedFileTaskCounterMap.forEach((appId, counter) -> {
                meterRegistry.gauge(
                    ExecuteMetricNames.GSE_FINISHED_TASKS_APP,
                    Arrays.asList(
                        Tag.of("appId", appId.toString()),
                        Tag.of("type", "file")
                    ),
                    counter,
                    getFunctionByCounter(counter)
                );
            });
        }

        private void measureFinishedScriptTasksByApp() {
            finishedScriptTaskCounterMap.forEach((appId, counter) -> {
                meterRegistry.gauge(
                    ExecuteMetricNames.GSE_FINISHED_TASKS_APP,
                    Arrays.asList(
                        Tag.of("appId", appId.toString()),
                        Tag.of("type", "script")
                    ),
                    counter,
                    getFunctionByCounter(counter)
                );
            });
        }

        @Override
        public void run() {
            while (runFlag) {
                measureHandlingFileTasksByApp();
                measureHandlingScriptTasksByApp();
                measureReceiveTasksByApp();
                measureFinishedFileTasksByApp();
                measureFinishedScriptTasksByApp();
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    log.info("sleep interrupted");
                }
            }
        }
    }

    @PostConstruct
    private void init() {
        new UpdateStatisticsThread().start();
    }

    private long incrementAppReceiveTask(long appId) {
        AtomicLong appTaskCounter = receiveTaskCounterMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        return appTaskCounter.incrementAndGet();
    }

    private long incrementAppHandlingFileTask(long appId) {
        AtomicLong appFileTaskCounter = handlingFileTasksMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        return appFileTaskCounter.incrementAndGet();
    }

    private long incrementAppHandlingScriptTask(long appId) {
        AtomicLong appTaskCounter = handlingScriptTasksMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        return appTaskCounter.incrementAndGet();
    }

    private long decrementAppHandlingFileTask(long appId) {
        AtomicLong appFileTaskCounter = handlingFileTasksMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        return appFileTaskCounter.decrementAndGet();
    }

    private long decrementAppHandlingScriptTask(long appId) {
        AtomicLong appTaskCounter = handlingScriptTasksMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        return appTaskCounter.decrementAndGet();
    }

    private long incrementAppFinishedFileTask(long appId) {
        AtomicLong appFinishedFileTaskCounter = finishedFileTaskCounterMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        return appFinishedFileTaskCounter.incrementAndGet();
    }

    private long incrementAppFinishedScriptTask(long appId) {
        AtomicLong appFinishedScriptTaskCounter = finishedScriptTaskCounterMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        return appFinishedScriptTaskCounter.incrementAndGet();
    }

    /**
     * 文件任务计数+1
     *
     * @return 当前运行的文件任务数
     */
    public long incrementFileTask(long appId) {
        incrementAppReceiveTask(appId);
        receiveTaskCounter.incrementAndGet();
        incrementAppHandlingFileTask(appId);
        return this.handlingFileTasks.incrementAndGet();
    }

    /**
     * 脚本任务计数+1
     *
     * @return 当前运行的脚本任务数
     */
    public long incrementScriptTask(long appId) {
        incrementAppReceiveTask(appId);
        receiveTaskCounter.incrementAndGet();
        incrementAppHandlingScriptTask(appId);
        return this.handlingScriptTasks.incrementAndGet();
    }

    /**
     * 文件任务计数-1
     *
     * @return 当前运行的文件任务数
     */
    public long decrementFileTask(long appId) {
        incrementAppFinishedFileTask(appId);
        finishedFileTaskCounter.incrementAndGet();
        decrementAppHandlingFileTask(appId);
        return this.handlingFileTasks.decrementAndGet();
    }

    /**
     * 脚本任务计数-1
     *
     * @return 当前运行的脚本任务数
     */
    public long decrementScriptTask(long appId) {
        incrementAppFinishedScriptTask(appId);
        finishedScriptTaskCounter.incrementAndGet();
        decrementAppHandlingScriptTask(appId);
        return this.handlingScriptTasks.decrementAndGet();
    }

    /**
     * 获取任务结果处理调度超时的次数
     *
     * @return 超时次数
     */
    public long getHandlingFileTaskCount() {
        return this.handlingFileTasks.get();
    }

    /**
     * 获取正在处理任务结果的脚本任务数
     *
     * @return 任务数
     */
    public long getHandlingScriptTaskCount() {
        return this.handlingScriptTasks.get();
    }

    public long getFinishedFileTaskCount() {
        return finishedFileTaskCounter.get();
    }

    public long getFinishedScriptTaskCount() {
        return finishedScriptTaskCounter.get();
    }

    public MeterRegistry getMeterRegistry() {
        return this.meterRegistry;
    }
}
