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

package com.tencent.bk.job.manage.background.ha;

import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskDispatcher;
import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskListenerController;
import com.tencent.bk.job.manage.common.constants.SmartLifecycleOrder;
import com.tencent.bk.job.manage.config.BackGroundTaskProperties;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台任务均衡器
 */
@Slf4j
@Service
public class BackGroundTaskBalancer implements SmartLifecycle {

    private final ThreadCostCalculator threadCostCalculator;
    private final BackGroundTaskRegistry backGroundTaskRegistry;
    private final BackGroundTaskListenerController backGroundTaskListenerController;
    private final BackGroundTaskDispatcher backGroundTaskDispatcher;
    private final BackGroundTaskProperties backGroundTaskProperties;
    /**
     * 均衡器是否活跃
     */
    private volatile boolean isActive = false;
    /**
     * 均衡器是否正在执行均衡操作
     */
    private volatile boolean isBalancerRunning = false;

    @Autowired
    public BackGroundTaskBalancer(ThreadCostCalculator threadCostCalculator,
                                  BackGroundTaskRegistry backGroundTaskRegistry,
                                  BackGroundTaskListenerController backGroundTaskListenerController,
                                  BackGroundTaskDispatcher backGroundTaskDispatcher,
                                  BackGroundTaskProperties backGroundTaskProperties) {
        this.threadCostCalculator = threadCostCalculator;
        this.backGroundTaskRegistry = backGroundTaskRegistry;
        this.backGroundTaskListenerController = backGroundTaskListenerController;
        this.backGroundTaskDispatcher = backGroundTaskDispatcher;
        this.backGroundTaskProperties = backGroundTaskProperties;
    }

    @Override
    public void start() {
        isActive = true;
    }

    @Override
    public void stop() {
        log.debug("BackGroundTaskBalancer stop");
        isActive = false;
    }

    @Override
    public boolean isRunning() {
        return isActive;
    }

    @Override
    public int getPhase() {
        return SmartLifecycleOrder.BACK_GROUND_TASK_BALANCER;
    }

    /**
     * 对当前实例正在运行的后台任务做一次负载均衡
     *
     * @return 是否执行成功
     */
    public boolean balance() {
        if (!backGroundTaskProperties.getBalancer().getEnabled()) {
            log.info("background-task balancer not enabled, you can enable it using config");
            return false;
        }
        if (!isActive) {
            log.info("balancer is not active, ignore");
            return false;
        }
        if (isBalancerRunning) {
            log.info("last balance is running, ignore");
            return false;
        }
        try {
            isBalancerRunning = true;
            return doBalance();
        } catch (Throwable t) {
            log.error("Fail to balance background-task", t);
            return false;
        } finally {
            isBalancerRunning = false;
        }
    }

    /**
     * 执行负载均衡
     *
     * @return 是否执行成功
     */
    private boolean doBalance() {
        // 1.计算每个实例应该承担的平均值
        int averageThreadCost = threadCostCalculator.calcAverageThreadCostForOneInstance();
        // 2.计算当前实例资源占用值
        int currentThreadCost = calcCurrentThreadCost();
        List<BackGroundTask> sortedTaskList = getSortedByTenantTaskList();
        // 3.如果当前实例的资源占用高于平均值，则将任务均衡到其他实例
        if (needToBalance(averageThreadCost, currentThreadCost, sortedTaskList)) {
            log.info(
                "averageThreadCost={}, currentThreadCost={}, start to balance",
                averageThreadCost,
                currentThreadCost
            );
            StopWatch watch = new StopWatch();

            // 关闭任务监听器
            watch.start("closeTaskListener");
            stopTaskListener();
            watch.stop();

            // 选取一批待转移任务
            watch.start("chooseTasks");
            int deltaThreadCost = currentThreadCost - averageThreadCost;
            List<BackGroundTask> choosedTaskList = chooseTaskForThreadCostFromTail(
                sortedTaskList,
                deltaThreadCost
            );
            watch.stop();

            // 将选取的任务优雅终止后转移到别的实例
            transferTasks(choosedTaskList, watch);
        } else if (currentThreadCost <= averageThreadCost) {
            log.info(
                "averageThreadCost={}, currentThreadCost={}, start task listener",
                averageThreadCost,
                currentThreadCost
            );
            startTaskListener();
        } else {
            // 临界状态，刚好满负载
            log.debug(
                "averageThreadCost={}, currentThreadCost={}, balanced, stop task listener",
                averageThreadCost,
                currentThreadCost
            );
            stopTaskListener();
        }
        return true;
    }

    /**
     * 根据当前实例的任务情况判断是否需要执行负载均衡
     *
     * @param averageThreadCost 每个实例平均应承担的线程消耗
     * @param currentThreadCost 当前实例的线程消耗值
     * @param sortedTaskList    已排序的当前任务列表
     * @return 是否需要执行负载均衡
     */
    boolean needToBalance(int averageThreadCost,
                          int currentThreadCost,
                          List<BackGroundTask> sortedTaskList) {
        if (currentThreadCost <= averageThreadCost) {
            return false;
        }
        if (Collections.isEmpty(sortedTaskList)) {
            return false;
        }
        BackGroundTask lastTask = sortedTaskList.get(sortedTaskList.size() - 1);
        // 如果当前线程消耗减去最后一个任务的线程消耗值后，依然于大于等于平均值，则需要执行负载均衡
        return currentThreadCost - lastTask.getThreadCost() >= averageThreadCost;
    }

    /**
     * 转移任务至别的实例
     *
     * @param targetTaskList 目标任务列表
     * @param watch          计时器
     */
    private void transferTasks(List<BackGroundTask> targetTaskList, StopWatch watch) {
        List<BackGroundTask> successList = new ArrayList<>();
        List<BackGroundTask> failedList = new ArrayList<>();
        for (BackGroundTask taskToTransfer : targetTaskList) {
            watch.start("tryToTransferTask:" + taskToTransfer.getUniqueCode());
            boolean success = tryToTransferTask(taskToTransfer);
            if (success) {
                successList.add(taskToTransfer);
            } else {
                failedList.add(taskToTransfer);
            }
            watch.stop();
        }
        logBalanceResult(successList, failedList, watch);
    }

    /**
     * 尝试转移任务
     *
     * @param taskToTransfer 待转移的任务
     * @return 是否转移成功
     */
    private boolean tryToTransferTask(BackGroundTask taskToTransfer) {
        try {
            // 优雅停止
            taskToTransfer.shutdownGracefully();
            // 发送任务至队列，让其他实例接收并处理
            TaskEntity taskEntity = taskToTransfer.getTaskEntity();
            sendTaskToQueue(taskEntity);
            return true;
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "Fail to transfer task:{}",
                taskToTransfer.getUniqueCode()
            ).getMessage();
            log.warn(message, t);
            return false;
        }
    }

    /**
     * 从列表尾部选取一些任务，使其总线程消耗值>=目标线程消耗值
     *
     * @param sortedTaskList   有序任务列表
     * @param targetThreadCost 目标线程消耗值
     * @return 选取的任务列表
     */
    List<BackGroundTask> chooseTaskForThreadCostFromTail(List<BackGroundTask> sortedTaskList,
                                                         int targetThreadCost) {
        List<BackGroundTask> choosedTaskList = new ArrayList<>();
        int accumulatedCost = 0;
        int size = sortedTaskList.size();
        for (int i = size - 1; i >= 0; i--) {
            BackGroundTask task = sortedTaskList.get(i);
            choosedTaskList.add(task);
            accumulatedCost += task.getThreadCost();
            if (accumulatedCost == targetThreadCost) {
                break;
            } else if (accumulatedCost > targetThreadCost) {
                // 超过目标值，移除最后一个任务，临界点任务不做均衡
                choosedTaskList.remove(choosedTaskList.size() - 1);
                break;
            }
        }
        return choosedTaskList;
    }

    /**
     * 获取根据租户ID排序的任务列表
     *
     * @return 有序任务列表
     */
    private List<BackGroundTask> getSortedByTenantTaskList() {
        List<BackGroundTask> sortedTaskList = new ArrayList<>(backGroundTaskRegistry.getTaskMap().values());
        sortedTaskList.sort(Comparator.comparing(BackGroundTask::getTenantId));
        return sortedTaskList;
    }

    /**
     * 计算当前实例的资源占用值
     *
     * @return 当前实例的资源占用值
     */
    private int calcCurrentThreadCost() {
        Map<String, BackGroundTask> taskMap = backGroundTaskRegistry.getTaskMap();
        int ThreadCost = 0;
        for (BackGroundTask task : taskMap.values()) {
            ThreadCost += task.getThreadCost();
        }
        return ThreadCost;
    }

    /**
     * 将负载均衡结果打印到日志
     *
     * @param successList 成功的任务列表
     * @param failedList  失败的任务列表
     * @param watch       计时器
     */
    private void logBalanceResult(List<BackGroundTask> successList,
                                  List<BackGroundTask> failedList,
                                  StopWatch watch) {
        if (watch.isRunning()) {
            watch.stop();
        }
        if (failedList.isEmpty()) {
            log.info(
                "Balance done, {} tasks hava been balanced: {}, timeConsuming: {}",
                successList.size(),
                successList.stream().map(BackGroundTask::getUniqueCode).collect(Collectors.toList()),
                watch.prettyPrint()
            );
        } else {
            log.warn(
                "Balance done, {} tasks hava been balanced: {}, {} tasks failed: {}, timeConsuming: {}",
                successList.size(),
                successList.stream().map(BackGroundTask::getUniqueCode).collect(Collectors.toList()),
                failedList.size(),
                failedList.stream().map(BackGroundTask::getUniqueCode).collect(Collectors.toList()),
                watch.prettyPrint()
            );
        }
    }

    /**
     * 开启任务监听器
     */
    private void startTaskListener() {
        if (isActive) {
            backGroundTaskListenerController.start();
        } else {
            log.info("balancer is not active, ignore startTaskListener");
        }
    }

    /**
     * 关闭任务监听器
     */
    private void stopTaskListener() {
        backGroundTaskListenerController.stop();
    }

    /**
     * 发送任务至队列
     */
    private void sendTaskToQueue(TaskEntity taskEntity) {
        backGroundTaskDispatcher.dispatch(taskEntity);
    }
}
