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
import com.tencent.bk.job.manage.config.BackGroundTaskProperties;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
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
public class BackGroundTaskBalancer {

    private final ResourceCostCalculator resourceCostCalculator;
    private final IBackGroundTaskRegistry backGroundTaskRegistry;
    private final BackGroundTaskListenerController backGroundTaskListenerController;
    private final BackGroundTaskDispatcher backGroundTaskDispatcher;
    private final BackGroundTaskProperties backGroundTaskProperties;

    private volatile boolean isBalancerRunning = false;

    @Autowired
    public BackGroundTaskBalancer(ResourceCostCalculator resourceCostCalculator,
                                  IBackGroundTaskRegistry backGroundTaskRegistry,
                                  BackGroundTaskListenerController backGroundTaskListenerController,
                                  BackGroundTaskDispatcher backGroundTaskDispatcher,
                                  BackGroundTaskProperties backGroundTaskProperties) {
        this.resourceCostCalculator = resourceCostCalculator;
        this.backGroundTaskRegistry = backGroundTaskRegistry;
        this.backGroundTaskListenerController = backGroundTaskListenerController;
        this.backGroundTaskDispatcher = backGroundTaskDispatcher;
        this.backGroundTaskProperties = backGroundTaskProperties;
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
        int averageResourceCost = resourceCostCalculator.calcAverageResourceCostForOneInstance();
        // 2.计算当前实例资源占用值
        int currentResourceCost = calcCurrentResourceCost();
        List<IBackGroundTask> sortedTaskList = getSortedByTenantTaskList();
        // 3.如果当前实例的资源占用高于平均值，则将任务均衡到其他实例
        if (needToBalance(averageResourceCost, currentResourceCost, sortedTaskList)) {
            log.info(
                "averageResourceCost={}, currentResourceCost={}, start to balance",
                averageResourceCost,
                currentResourceCost
            );
            StopWatch watch = new StopWatch();

            // 关闭任务监听器
            watch.start("closeTaskListener");
            stopTaskListener();
            watch.stop();

            // 选取一批任务优雅终止
            watch.start("chooseTask");
            int deltaResourceCost = currentResourceCost - averageResourceCost;
            List<IBackGroundTask> choosedTaskList = chooseTaskForResourceCostFromTail(
                sortedTaskList,
                deltaResourceCost
            );
            watch.stop();

            List<IBackGroundTask> successList = new ArrayList<>();
            List<IBackGroundTask> failedList = new ArrayList<>();
            for (IBackGroundTask taskToTransfer : choosedTaskList) {
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
        } else if (currentResourceCost <= averageResourceCost) {
            log.info(
                "averageResourceCost={}, currentResourceCost={}, start task listener",
                averageResourceCost,
                currentResourceCost
            );
            startTaskListener();
        } else {
            // 临界状态，刚好满负载
            log.debug(
                "averageResourceCost={}, currentResourceCost={}, balanced, stop task listener",
                averageResourceCost,
                currentResourceCost
            );
            stopTaskListener();
        }
        return true;
    }

    /**
     * 根据当前实例的任务情况判断是否需要执行负载均衡
     *
     * @param averageResourceCost 每个实例平均应承担的资源消耗
     * @param currentResourceCost 当前实例的资源消耗值
     * @param sortedTaskList      已排序的当前任务列表
     * @return 是否需要执行负载均衡
     */
    private boolean needToBalance(int averageResourceCost,
                                  int currentResourceCost,
                                  List<IBackGroundTask> sortedTaskList) {
        if (currentResourceCost <= averageResourceCost) {
            return false;
        }
        if (Collections.isEmpty(sortedTaskList)) {
            return false;
        }
        IBackGroundTask lastTask = sortedTaskList.get(sortedTaskList.size() - 1);
        // 如果当前资源消耗减去最后一个任务的资源消耗值后，依然高于平均值，则需要执行负载均衡
        return currentResourceCost - lastTask.getResourceCost() >= averageResourceCost;
    }

    /**
     * 尝试转移任务
     *
     * @param taskToTransfer 待转移的任务
     * @return 是否转移成功
     */
    private boolean tryToTransferTask(IBackGroundTask taskToTransfer) {
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
     * 从列表尾部选取一些任务，使其总资源消耗值>=目标资源消耗值
     *
     * @param sortedTaskList     有序任务列表
     * @param targetResourceCost 目标资源消耗值
     * @return 选取的任务列表
     */
    private List<IBackGroundTask> chooseTaskForResourceCostFromTail(List<IBackGroundTask> sortedTaskList,
                                                                    int targetResourceCost) {
        List<IBackGroundTask> choosedTaskList = new ArrayList<>();
        int accumulatedCost = 0;
        int size = sortedTaskList.size();
        for (int i = size - 1; i >= 0; i--) {
            IBackGroundTask task = sortedTaskList.get(i);
            choosedTaskList.add(task);
            accumulatedCost += task.getResourceCost();
            if (accumulatedCost == targetResourceCost) {
                break;
            } else if (accumulatedCost > targetResourceCost) {
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
    private List<IBackGroundTask> getSortedByTenantTaskList() {
        List<IBackGroundTask> sortedTaskList = new ArrayList<>(backGroundTaskRegistry.getTaskMap().values());
        sortedTaskList.sort(Comparator.comparing(IBackGroundTask::getTenantId));
        return sortedTaskList;
    }

    /**
     * 计算当前实例的资源占用值
     *
     * @return 当前实例的资源占用值
     */
    private int calcCurrentResourceCost() {
        Map<String, IBackGroundTask> taskMap = backGroundTaskRegistry.getTaskMap();
        int resourceCost = 0;
        for (IBackGroundTask task : taskMap.values()) {
            resourceCost += task.getResourceCost();
        }
        return resourceCost;
    }

    /**
     * 将负载均衡结果打印到日志
     *
     * @param successList 成功的任务列表
     * @param failedList  失败的任务列表
     * @param watch       计时器
     */
    private void logBalanceResult(List<IBackGroundTask> successList,
                                  List<IBackGroundTask> failedList,
                                  StopWatch watch) {
        if (watch.isRunning()) {
            watch.stop();
        }
        if (failedList.isEmpty()) {
            log.info(
                "Balance done, {} tasks hava been balanced: {}, timeConsuming: {}",
                successList.size(),
                successList.stream().map(IBackGroundTask::getUniqueCode).collect(Collectors.toList()),
                watch.prettyPrint()
            );
        } else {
            log.warn(
                "Balance done, {} tasks hava been balanced: {}, {} tasks failed: {}, timeConsuming: {}",
                successList.size(),
                successList.stream().map(IBackGroundTask::getUniqueCode).collect(Collectors.toList()),
                failedList.size(),
                failedList.stream().map(IBackGroundTask::getUniqueCode).collect(Collectors.toList()),
                watch.prettyPrint()
            );
        }
    }

    /**
     * 开启任务监听器
     */
    private void startTaskListener() {
        backGroundTaskListenerController.start();
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
