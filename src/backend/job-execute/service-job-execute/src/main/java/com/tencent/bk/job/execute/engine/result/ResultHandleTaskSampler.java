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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.Getter;
import lombok.Setter;
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
     * 正在处理任务结果的文件任务数（各业务）
     */
    private final ConcurrentHashMap<Long, StatisticsUnit> handlingFileTasksMap = new ConcurrentHashMap<>();
    /**
     * 正在处理任务结果的脚本任务数（各业务）
     */
    private final ConcurrentHashMap<Long, StatisticsUnit> handlingScriptTasksMap = new ConcurrentHashMap<>();
    /**
     * 接收的结果处理任务数（各业务）
     */
    private final ConcurrentHashMap<Long, AtomicLong> receiveTaskCounterMap = new ConcurrentHashMap<>();
    /**
     * 完成的文件任务数（各业务）
     */
    private final ConcurrentHashMap<Long, AtomicLong> finishedFileTaskCounterMap = new ConcurrentHashMap<>();
    /**
     * 完成的脚本任务数（各业务）
     */
    private final ConcurrentHashMap<Long, AtomicLong> finishedScriptTaskCounterMap = new ConcurrentHashMap<>();

    @Autowired
    public ResultHandleTaskSampler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Getter
    @Setter
    static
    class StatisticsUnit {
        private AtomicLong counter;
        private Gauge gauge;

        StatisticsUnit(AtomicLong counter, Gauge gauge) {
            this.counter = counter;
            this.gauge = gauge;
        }
    }

    class UpdateStatisticsThread extends Thread {

        boolean runFlag = true;

        private ToDoubleFunction<AtomicLong> debugFunctionByCounter(AtomicLong counter) {
            return new ToDoubleFunction() {
                @Override
                public double applyAsDouble(Object value) {
                    return counter.doubleValue();
                }
            };
        }

        private ToDoubleFunction<AtomicLong> getFunctionByCounter(AtomicLong counter) {
            return new ToDoubleFunction() {
                @Override
                public double applyAsDouble(Object value) {
                    return counter.doubleValue();
                }
            };
        }

        private void measureHandlingFileTasksByApp() {
            handlingFileTasksMap.forEach((appId, statisticsUnit) -> {
                AtomicLong counter = statisticsUnit.getCounter();
                Gauge handlingFileTasksGauge = Gauge.builder(
                    ExecuteMetricNames.GSE_RUNNING_TASKS,
                    counter,
                    getFunctionByCounter(counter)
                ).tags(Arrays.asList(
                    Tag.of("appId", appId.toString()),
                    Tag.of("stage", "result-handle"),
                    Tag.of("type", "file")
                )).register(meterRegistry);
                statisticsUnit.setGauge(handlingFileTasksGauge);
            });
        }

        private void measureHandlingScriptTasksByApp() {
            handlingScriptTasksMap.forEach((appId, statisticsUnit) -> {
                AtomicLong counter = statisticsUnit.getCounter();
                Gauge handlingScriptTasksGauge = Gauge.builder(
                    ExecuteMetricNames.GSE_RUNNING_TASKS,
                    counter,
                    debugFunctionByCounter(counter)
                ).tags(Arrays.asList(
                    Tag.of("appId", appId.toString()),
                    Tag.of("stage", "result-handle"),
                    Tag.of("type", "script")
                )).register(meterRegistry);
                statisticsUnit.setGauge(handlingScriptTasksGauge);
            });
        }

        private void measureReceiveTasksByApp() {
            receiveTaskCounterMap.forEach((appId, counter) -> {
                meterRegistry.gauge(
                    ExecuteMetricNames.GSE_RUNNING_TASKS,
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
                    ExecuteMetricNames.GSE_FINISHED_TASKS_TOTAL,
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
                    ExecuteMetricNames.GSE_FINISHED_TASKS_TOTAL,
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
                try {
                    sleep(1000);
                    log.debug("UpdateStatisticsThread run");
                    measureHandlingFileTasksByApp();
                    measureHandlingScriptTasksByApp();
                    measureReceiveTasksByApp();
                    measureFinishedFileTasksByApp();
                    measureFinishedScriptTasksByApp();
                } catch (InterruptedException e) {
                    log.info("sleep interrupted");
                } catch (Exception e) {
                    log.warn("UpdateStatisticsThread error", e);
                }
            }
        }
    }

    @PostConstruct
    private void init() {
        log.debug("UpdateStatisticsThread init");
        new UpdateStatisticsThread().start();
    }

    private void incrementAppReceiveTask(long appId) {
        AtomicLong appTaskCounter = receiveTaskCounterMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        appTaskCounter.incrementAndGet();
    }

    private void incrementAppHandlingFileTask(long appId) {
        StatisticsUnit statisticsUnit = handlingFileTasksMap.computeIfAbsent(
            appId, pAppId -> new StatisticsUnit(new AtomicLong(0), null)
        );
        AtomicLong appFileTaskCounter = statisticsUnit.getCounter();
        appFileTaskCounter.incrementAndGet();
    }

    private void incrementAppHandlingScriptTask(long appId) {
        StatisticsUnit statisticsUnit = handlingScriptTasksMap.computeIfAbsent(
            appId, pAppId -> new StatisticsUnit(new AtomicLong(0), null)
        );
        AtomicLong appTaskCounter = statisticsUnit.getCounter();
        appTaskCounter.incrementAndGet();
    }

    private void decrementAppHandlingFileTask(long appId) {
        StatisticsUnit statisticsUnit = handlingFileTasksMap.computeIfAbsent(
            appId, pAppId -> new StatisticsUnit(new AtomicLong(0), null)
        );
        AtomicLong appFileTaskCounter = statisticsUnit.getCounter();
        long value = appFileTaskCounter.decrementAndGet();
        if (value == 0) {
            Gauge gauge = statisticsUnit.getGauge();
            if (gauge != null) {
                handlingFileTasksMap.remove(appId);
                meterRegistry.remove(gauge);
            }
        }
    }

    private void decrementAppHandlingScriptTask(long appId) {
        StatisticsUnit statisticsUnit = handlingScriptTasksMap.computeIfAbsent(
            appId, pAppId -> new StatisticsUnit(new AtomicLong(0), null)
        );
        AtomicLong appTaskCounter = statisticsUnit.getCounter();
        long value = appTaskCounter.decrementAndGet();
        if (value == 0) {
            Gauge gauge = statisticsUnit.getGauge();
            if (gauge != null) {
                handlingScriptTasksMap.remove(appId);
                meterRegistry.remove(gauge);
            }
        }
    }

    private void incrementAppFinishedFileTask(long appId) {
        AtomicLong appFinishedFileTaskCounter = finishedFileTaskCounterMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        appFinishedFileTaskCounter.incrementAndGet();
    }

    private void incrementAppFinishedScriptTask(long appId) {
        AtomicLong appFinishedScriptTaskCounter = finishedScriptTaskCounterMap.computeIfAbsent(
            appId, pAppId -> new AtomicLong(0)
        );
        appFinishedScriptTaskCounter.incrementAndGet();
    }

    /**
     * 文件任务计数+1
     *
     * @return 当前运行的文件任务数
     */
    public void incrementFileTask(long appId) {
        incrementAppReceiveTask(appId);
        incrementAppHandlingFileTask(appId);
    }

    /**
     * 脚本任务计数+1
     *
     * @return 当前运行的脚本任务数
     */
    public void incrementScriptTask(long appId) {
        incrementAppReceiveTask(appId);
        incrementAppHandlingScriptTask(appId);
    }

    /**
     * 文件任务计数-1
     *
     * @return 当前运行的文件任务数
     */
    public void decrementFileTask(long appId) {
        incrementAppFinishedFileTask(appId);
        decrementAppHandlingFileTask(appId);
    }

    /**
     * 脚本任务计数-1
     *
     * @return 当前运行的脚本任务数
     */
    public void decrementScriptTask(long appId) {
        incrementAppFinishedScriptTask(appId);
        decrementAppHandlingScriptTask(appId);
    }

    public MeterRegistry getMeterRegistry() {
        return this.meterRegistry;
    }
}
