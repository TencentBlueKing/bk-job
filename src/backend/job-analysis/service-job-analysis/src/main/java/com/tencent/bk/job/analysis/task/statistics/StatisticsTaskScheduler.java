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

package com.tencent.bk.job.analysis.task.statistics;

import com.tencent.bk.job.analysis.config.StatisticConfig;
import com.tencent.bk.job.analysis.task.statistics.task.ClearExpiredStatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.IStatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.PastStatisticsMakeupTask;
import com.tencent.bk.job.analysis.task.statistics.task.TaskInfo;
import com.tencent.bk.job.analysis.task.statistics.task.TaskStatusListener;
import com.tencent.bk.job.analysis.task.statistics.task.WrappedCallable;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @Description
 * @Date 2020/7/31
 * @Version 1.0
 */
@Component
@Slf4j
public class StatisticsTaskScheduler {

    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final String REDIS_KEY_STATISTIC_JOB_LOCK = "statistic-job-lock";
    private static final String REDIS_KEY_STATISTIC_JOB_RUNNING_MACHINE = "statistic-job-running-machine";
    private static final AtomicLong rejectedStatisticsTaskNum = new AtomicLong(0);
    private static final String REDIS_KEY_STATISTIC_JOB_INIT_MACHINE = "statistic-job-init-machine";
    public static List<IStatisticsTask> statisticsTaskList = new ArrayList<>();
    public static Map<String, IStatisticsTask> statisticsTaskMap = new ConcurrentHashMap<>();
    public static Map<String, TaskInfo> currentStatisticTaskFutureMap = new HashMap<>();
    private static ThreadPoolExecutor currentStatisticsTaskExecutor = new ThreadPoolExecutor(
        10, 10, 60L,
        TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(2000), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("statisticsTask runnable rejected! num:{}", rejectedStatisticsTaskNum.incrementAndGet());
        }
    });
    private static ThreadPoolExecutor pastStatisticsTaskExecutor = new ThreadPoolExecutor(
        10, 10, 60L,
        TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(200000), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("pastStatisticsTaskExecutor runnable rejected! num:{}",
                rejectedStatisticsTaskNum.incrementAndGet());
        }
    });

    static {
        List<String> keyList = Collections.singletonList(REDIS_KEY_STATISTIC_JOB_LOCK);
        keyList.forEach(key -> {
            try {
                //进程重启首先尝试释放上次加上的锁避免死锁
                LockUtils.releaseDistributedLock(key, machineIp);
            } catch (Throwable t) {
                log.info("Redis key:" + key + " does not need to be released, ignore");
            }
        });
    }

    private final StatisticConfig statisticConfig;
    private final RedisTemplate<String, String> redisTemplate;
    private final PastStatisticsMakeupTask pastStatisticsMakeupTask;
    private final ClearExpiredStatisticsTask clearExpiredStatisticsTask;
    private int currentCycleHours = 0;

    @Autowired
    public StatisticsTaskScheduler(MeterRegistry meterRegistry, StatisticConfig statisticConfig, RedisTemplate<String
        , String> redisTemplate, PastStatisticsMakeupTask pastStatisticsMakeupTask,
                                   ClearExpiredStatisticsTask clearExpiredStatisticsTask) {
        this.statisticConfig = statisticConfig;
        this.redisTemplate = redisTemplate;
        this.pastStatisticsMakeupTask = pastStatisticsMakeupTask;
        this.clearExpiredStatisticsTask = clearExpiredStatisticsTask;
        meterRegistry.gauge(
            StatisticsConstants.NAME_STATISTICS_TASK_SCHEDULE_POOL_SIZE,
            Collections.singletonList(Tag.of(StatisticsConstants.TAG_MODULE,
                StatisticsConstants.VALUE_MODULE_STATISTICS_TASK)),
            currentStatisticsTaskExecutor,
            ThreadPoolExecutor::getPoolSize
        );
        meterRegistry.gauge(
            StatisticsConstants.NAME_STATISTICS_TASK_SCHEDULE_QUEUE_SIZE,
            Collections.singletonList(Tag.of(StatisticsConstants.TAG_MODULE,
                StatisticsConstants.VALUE_MODULE_STATISTICS_TASK)),
            currentStatisticsTaskExecutor,
            threadPoolExecutor -> threadPoolExecutor.getQueue().size()
        );
    }

    public static synchronized void findStatisticsTask() {
        ApplicationContext context = ApplicationContextRegister.getContext();
        List<String> beanNames = Arrays.asList(context.getBeanDefinitionNames());
        beanNames.forEach(beanName -> {
            Object bean = context.getBean(beanName);
            if (bean instanceof IStatisticsTask) {
                log.debug("add " + beanName);
                IStatisticsTask taskBean = (IStatisticsTask) bean;
                statisticsTaskList.add(taskBean);
                statisticsTaskMap.put(bean.getClass().getSimpleName(), taskBean);
            }
        });
        log.info(String.format("There are %d StatisticsTasks", statisticsTaskList.size()));
    }

    public boolean configThreads(Integer currentStatisticThreadsNum, Integer pastStatisticThreadsNum) {
        boolean reconfiged = false;
        if (currentStatisticThreadsNum != null && currentStatisticThreadsNum > 0) {
            log.info("reconfig currentStatisticsTaskExecutor to {} threads", currentStatisticThreadsNum);
            List<Runnable> canceledRunnableList = currentStatisticsTaskExecutor.shutdownNow();
            if (canceledRunnableList.size() > 0) {
                log.warn("{} threads of  canceled", canceledRunnableList.size());
            }
            currentStatisticsTaskExecutor = new ThreadPoolExecutor(
                currentStatisticThreadsNum,
                currentStatisticThreadsNum,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        log.error("statisticsTask runnable rejected!");
                    }
                });
            reconfiged = true;
        } else {
            log.info("Invalid currentStatisticThreadsNum:{}", currentStatisticThreadsNum);
        }
        if (pastStatisticThreadsNum != null && pastStatisticThreadsNum > 0) {
            log.info("reconfig pastStatisticsTaskExecutor to {} threads", pastStatisticThreadsNum);
            pastStatisticsMakeupTask.setRunFlag(false);
            List<Runnable> canceledRunnableList = pastStatisticsTaskExecutor.shutdownNow();
            if (canceledRunnableList.size() > 0) {
                log.warn("{} threads of pastStatisticsTaskExecutor canceled", canceledRunnableList.size());
            }
            pastStatisticsTaskExecutor = new ThreadPoolExecutor(
                pastStatisticThreadsNum,
                pastStatisticThreadsNum,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(200000),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        log.error("pastStatisticsTaskExecutor runnable rejected!");
                    }
                });
            pastStatisticsMakeupTask.setExecutor(pastStatisticsTaskExecutor);
            reconfiged = true;
        } else {
            log.info("Invalid pastStatisticThreadsNum:{}", pastStatisticThreadsNum);
        }
        return reconfiged;
    }

    public List<String> cancelTasks(List<String> targetTaskNameList) {
        List<String> canceledTaskNames = new ArrayList<>();
        for (String taskName : targetTaskNameList) {
            if (currentStatisticTaskFutureMap.containsKey(taskName)) {
                currentStatisticTaskFutureMap.get(taskName).getFuture().cancel(true);
                canceledTaskNames.add(taskName);
            } else if (pastStatisticsMakeupTask.getPastStatisticTaskFutureMap().containsKey(taskName)) {
                pastStatisticsMakeupTask.getPastStatisticTaskFutureMap()
                    .get(taskName).getFuture().cancel(true);
                canceledTaskNames.add(taskName);
            } else {
                log.info("Task {} not in currentStatisticTaskFutureMap or pastStatisticTaskFutureMap, may finished",
                    taskName);
            }
        }
        return canceledTaskNames;
    }

    public List<String> startTasks(String startDateStr, String endDateStr, List<String> taskNameList) {
        if (StringUtils.isBlank(startDateStr)) {
            startDateStr = TimeUtil.getCurrentTimeStr(StatisticsConstants.DATE_PATTERN);
        }
        if (StringUtils.isBlank(endDateStr)) {
            endDateStr = TimeUtil.getCurrentTimeStr(StatisticsConstants.DATE_PATTERN);
        }
        if (endDateStr.compareTo(startDateStr) < 0) {
            String tmp = startDateStr;
            startDateStr = endDateStr;
            endDateStr = tmp;
        }

        LocalDateTime startDate = TimeUtil.getDayStartTime(startDateStr);
        LocalDateTime targetDate = TimeUtil.getDayStartTime(endDateStr);
        List<String> startedTaskNames = new ArrayList<>();
        if (taskNameList == null || taskNameList.isEmpty()) {
            taskNameList = new ArrayList<>(statisticsTaskMap.keySet());
        }
        int count = 0;
        while (targetDate.compareTo(startDate) >= 0 && count < 1000) {
            for (String taskName : taskNameList) {
                if (statisticsTaskMap.containsKey(taskName)) {
                    IStatisticsTask task = statisticsTaskMap.get(taskName);
                    log.info("OP Start {} of {}", taskName, targetDate);
                    pastStatisticsMakeupTask.startTask(task, targetDate);
                    startedTaskNames.add(taskName);
                } else {
                    log.warn("Cannot find task:{}", taskName);
                }
            }
            targetDate = targetDate.minusDays(1);
            count += 1;
        }
        if (count >= 1000) {
            log.warn("Unexpected OP operations:startTasks for over 1000 days, plz check");
        }
        return startedTaskNames;
    }

    public List<String> cancelAllTasks() {
        List<String> canceledTaskNames = new ArrayList<>();
        for (Map.Entry<String, TaskInfo> entry : currentStatisticTaskFutureMap.entrySet()) {
            entry.getValue().getFuture().cancel(true);
            canceledTaskNames.add(entry.getKey());
        }
        for (Map.Entry<String, TaskInfo> entry : pastStatisticsMakeupTask.getPastStatisticTaskFutureMap().entrySet()) {
            entry.getValue().getFuture().cancel(true);
            canceledTaskNames.add(entry.getKey());
        }
        return canceledTaskNames;
    }

    public List<String> listAllTasks() {
        if (statisticsTaskList == null || statisticsTaskList.isEmpty()) {
            findStatisticsTask();
        }
        return statisticsTaskList.parallelStream()
            .map(it -> it.getClass().getSimpleName()).collect(Collectors.toList());
    }

    public List<Pair<String, Integer>> listArrangedTasks() {
        List<Pair<String, Integer>> taskInfoList = new ArrayList<>();
        for (Map.Entry<String, TaskInfo> entry : currentStatisticTaskFutureMap.entrySet()) {
            taskInfoList.add(Pair.of(entry.getKey(), entry.getValue().getStatus()));
        }
        for (Map.Entry<String, TaskInfo> entry : pastStatisticsMakeupTask.getPastStatisticTaskFutureMap().entrySet()) {
            taskInfoList.add(Pair.of(entry.getKey(), entry.getValue().getStatus()));
        }
        return taskInfoList;
    }

    public Long getRejectedStatisticsTaskNum() {
        return rejectedStatisticsTaskNum.get();
    }

    // 补全历史统计数据
    public void makeupPastStatistics() {
        if (statisticsTaskList.isEmpty()) {
            findStatisticsTask();
        }
        String runningMachine = redisTemplate.opsForValue().get(REDIS_KEY_STATISTIC_JOB_INIT_MACHINE);
        if (StringUtils.isNotBlank(runningMachine)) {
            //已有初始化线程在跑，不再重复初始化
            log.info("statisticInitTask thread already running on {}", runningMachine);
            return;
        }
        // 历史统计数据补全任务
        pastStatisticsMakeupTask.setStatisticsTaskList(statisticsTaskList);
        pastStatisticsMakeupTask.setExecutor(pastStatisticsTaskExecutor);
        new Thread(() -> {
            RedisKeyHeartBeatThread statisticInitTaskRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
                redisTemplate,
                REDIS_KEY_STATISTIC_JOB_INIT_MACHINE,
                machineIp,
                5000L,
                4000L
            );
            // 开一个心跳子线程，维护当前机器正在初始化后台统计任务的状态
            statisticInitTaskRedisKeyHeartBeatThread.setName("statisticInitTaskRedisKeyHeartBeatThread");
            statisticInitTaskRedisKeyHeartBeatThread.start();
            try {
                pastStatisticsMakeupTask.run();
            } catch (Throwable t) {
                log.error("Exception when makeupPastStatistics", t);
            } finally {
                statisticInitTaskRedisKeyHeartBeatThread.setRunFlag(false);
            }
        }).start();
    }

    /**
     * 1h调度一次
     */
    public void schedule() {
        if (!statisticConfig.getEnable()) {
            log.info("StatisticsTaskScheduler not enabled, you can enable it by set job.analysis.statistics" +
                ".enable=true in config file and restart process");
            return;
        }
        if (currentCycleHours % statisticConfig.getIntervalHours() != 0) {
            log.info("Skip, currentCycleHours={}, every {} hours execute one statisticTask", currentCycleHours,
                statisticConfig.getIntervalHours());
            currentCycleHours += 1;
            return;
        } else {
            log.info("Hit, currentCycleHours={}, every {} hours execute one statisticTask", currentCycleHours,
                statisticConfig.getIntervalHours());
            currentCycleHours = 1;
        }

        // 分布式唯一性保证
        boolean lockGotten = LockUtils.tryGetDistributedLock(REDIS_KEY_STATISTIC_JOB_LOCK, machineIp, 50);
        if (!lockGotten) {
            log.info("lock {} gotten by another machine, return", REDIS_KEY_STATISTIC_JOB_LOCK);
            return;
        }
        String runningMachine = redisTemplate.opsForValue().get(REDIS_KEY_STATISTIC_JOB_RUNNING_MACHINE);
        if (StringUtils.isNotBlank(runningMachine)) {
            //已有同步线程在跑，不再同步
            log.info("StatisticsTaskScheduler thread already running on {}", runningMachine);
            return;
        }
        // 开一个心跳子线程，维护当前机器正在执行后台统计任务的状态
        RedisKeyHeartBeatThread statisticTaskSchedulerRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
            redisTemplate,
            REDIS_KEY_STATISTIC_JOB_RUNNING_MACHINE,
            machineIp,
            5000L,
            4000L
        );
        statisticTaskSchedulerRedisKeyHeartBeatThread.setName("statisticTaskSchedulerRedisKeyHeartBeatThread");
        statisticTaskSchedulerRedisKeyHeartBeatThread.start();

        // 统计任务开始
        log.info("start StatisticsTaskScheduler at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
            System.currentTimeMillis());
        List<Future<?>> futureList = new ArrayList<>();
        StopWatch stopWatch = new StopWatch("statisticsWatch");
        try {
            //找到所有的统计任务
            if (statisticsTaskList.isEmpty()) {
                findStatisticsTask();
            }
            //根据任务各自周期进行调度
            stopWatch.start("current all statistic tasks time consuming");
            //调度新线程跑分析任务
            for (IStatisticsTask iStatisticsTask : statisticsTaskList) {
                String taskName =
                    iStatisticsTask.getClass().getSimpleName()
                        + "-" + TimeUtil.getTodayStartTimeStr(StatisticsConstants.DATE_PATTERN);
                WrappedCallable<Boolean> taskCallable = new WrappedCallable<>(taskName, iStatisticsTask);
                log.debug("submit task:{}", taskName);
                taskCallable.setTaskStatusListener(new TaskStatusListener<Boolean>() {
                    @Override
                    public boolean onStart() {
                        currentStatisticTaskFutureMap.get(taskName).setStatus(1);
                        return false;
                    }

                    @Override
                    public boolean onFinish(Boolean result) {
                        if (currentStatisticTaskFutureMap.containsKey(taskName)) {
                            currentStatisticTaskFutureMap.remove(taskName);
                        }
                        return result;
                    }
                });
                currentStatisticTaskFutureMap.put(taskName, new TaskInfo(null, 0));
                Future<?> future = currentStatisticsTaskExecutor.submit(taskCallable);
                futureList.add(future);
                currentStatisticTaskFutureMap.get(taskName).setFuture(future);
            }
            // 清理过期统计数据
            futureList.add(currentStatisticsTaskExecutor.submit(clearExpiredStatisticsTask));
            // 等待所有统计任务结束
            futureList.forEach(future -> {
                try {
                    future.get(10, TimeUnit.MINUTES);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error("Exception in statistic task", e);
                }
            });
            // 向Redis写入统计数据更新时间，不过期
            redisTemplate.opsForValue().set(StatisticsConstants.KEY_DATA_UPDATE_TIME, TimeUtil.getCurrentTimeStr());
        } catch (Throwable t) {
            log.error("Exception in StatisticsTaskScheduler", t);
        } finally {
            statisticTaskSchedulerRedisKeyHeartBeatThread.setRunFlag(false);
            stopWatch.stop();
            log.info(stopWatch.prettyPrint());
        }
    }
}
