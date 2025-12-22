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

package com.tencent.bk.job.analysis.task.statistics.task;

import com.tencent.bk.job.analysis.api.consts.StatisticsConstants;
import com.tencent.bk.job.analysis.config.StatisticConfig;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.tenant.TenantDTO;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 历史数据补全任务
 */
@Slf4j
@Service
public class PastStatisticsMakeupTask {
    protected final TenantService tenantService;
    private final StatisticConfig statisticConfig;
    @Getter
    public Map<String, TaskInfo> pastStatisticTaskFutureMap = new HashMap<>();
    @Setter
    private List<IStatisticsTask> statisticsTaskList;
    @Setter
    private volatile boolean runFlag = true;
    private volatile boolean isRunning = false;
    @Setter
    private ThreadPoolExecutor executor;

    @Autowired
    public PastStatisticsMakeupTask(TenantService tenantService, StatisticConfig statisticConfig) {
        this.tenantService = tenantService;
        this.statisticConfig = statisticConfig;
    }

    public WatchableTask<Boolean> getCallableOfTask(IStatisticsTask statisticsTask, LocalDateTime targetDate) {
        Callable<Boolean> callable = () -> {
            try {
                List<TenantDTO> tenantDTOList = tenantService.listEnabledTenant();
                for (TenantDTO tenantDTO : tenantDTOList) {
                    genStatisticsByDayForTenant(tenantDTO.getId(), statisticsTask, targetDate);
                }
                return true;
            } catch (Throwable t) {
                log.error("Fail to genStatisticsByDay({})", targetDate, t);
                return false;
            }
        };
        String targetDateStr = TimeUtil.getTimeStr(targetDate, StatisticsConstants.DATE_PATTERN);
        String taskName = statisticsTask.getClass().getSimpleName() + "-" + targetDateStr;
        WatchableTask<Boolean> taskCallable = new WatchableTask<>(taskName, callable);
        taskCallable.setTaskStatusListener(new DefaultTaskStatusListener(pastStatisticTaskFutureMap, taskName));
        return taskCallable;
    }

    private void genStatisticsByDayForTenant(String tenantId,
                                             IStatisticsTask statisticsTask,
                                             LocalDateTime targetDate) {
        try {
            // 设置当前租户ID
            JobContextUtil.setUser(new User(tenantId, null, null));
            statisticsTask.genStatisticsByDay(targetDate);
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "Fail to genStatisticsByDay({}), tenantId={}",
                targetDate,
                tenantId
            ).getMessage();
            log.warn(message);
            throw t;
        } finally {
            // 清理租户ID
            JobContextUtil.setUser(null);
        }
    }

    public List<WatchableTask<Boolean>> getCallableList(LocalDateTime targetDate) {
        List<WatchableTask<Boolean>> callableList = new ArrayList<>();
        log.debug("check targetDate={}", targetDate);
        String targetDateStr = TimeUtil.getTimeStr(targetDate, StatisticsConstants.DATE_PATTERN);
        for (IStatisticsTask statisticsTask : statisticsTaskList) {
            if (!statisticsTask.isDataComplete(targetDateStr)) {
                log.info("gen {} statistics for past date {}", statisticsTask.getName(), targetDateStr);
                WatchableTask<Boolean> taskCallable = getCallableOfTask(statisticsTask, targetDate);
                callableList.add(taskCallable);
            } else {
                log.info("Data of {} exists for date {}, skip to makeup", statisticsTask.getName(), targetDateStr);
            }
        }
        return callableList;
    }

    public Future<Boolean> startTask(IStatisticsTask statisticsTask, LocalDateTime targetDate) {
        runFlag = true;
        WatchableTask<Boolean> taskCallable = getCallableOfTask(statisticsTask, targetDate);
        return startTask(taskCallable);
    }

    private Future<Boolean> startTask(WatchableTask<Boolean> taskCallable) {
        pastStatisticTaskFutureMap.put(taskCallable.getName(), new TaskInfo(null, TaskInfo.STATUS_WAITING));
        Future<Boolean> future = executor.submit(taskCallable);
        pastStatisticTaskFutureMap.get(taskCallable.getName()).setFuture(future);
        return future;
    }

    public void run() {
        if (isRunning) {
            log.info("PastStatisticsMakeupTask already running, ignore");
            return;
        }
        StopWatch stopWatch = new StopWatch("PastStatisticsMakeupTask");
        stopWatch.start("PastStatisticsMakeupTask time consuming");
        isRunning = true;
        if (statisticsTaskList == null || statisticsTaskList.isEmpty()) {
            log.warn("statisticsTaskList is {}, ignore", statisticsTaskList);
        }
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= statisticConfig.getExpireDays(); i++) {
            if (!runFlag) {
                log.info("runFlag set to false, break");
                break;
            }
            // 把一天的数据跑完再来下一天
            LocalDateTime targetDate = now.plusDays(-i);
            List<WatchableTask<Boolean>> callableList = getCallableList(targetDate);
            List<Future<Boolean>> futureList = new ArrayList<>();
            if (executor != null) {
                for (WatchableTask<Boolean> callable : callableList) {
                    Future<Boolean> future = startTask(callable);
                    futureList.add(future);
                }
                for (Future<Boolean> future : futureList) {
                    try {
                        int timeoutMinutes = 10;
                        future.get(timeoutMinutes, TimeUnit.MINUTES);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        log.warn("exception when make up past statistics", e);
                    }
                }
            } else {
                log.info("Use current single thread to run makeup tasks");
                for (WatchableTask<Boolean> callable : callableList) {
                    try {
                        pastStatisticTaskFutureMap.put(
                            callable.getName(), new TaskInfo(null, TaskInfo.STATUS_RUNNING)
                        );
                        callable.call();
                    } catch (Exception e) {
                        log.warn("exception when make up past statistics", e);
                    }
                }
            }
        }
        isRunning = false;
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
    }
}
