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

package com.tencent.bk.job.manage.background.ha;

import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskDispatcher;
import com.tencent.bk.job.manage.background.ha.mq.BackGroundTaskListenerController;
import com.tencent.bk.job.manage.config.BackGroundTaskProperties;
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

    private final IBackGroundTaskRegistry backGroundTaskRegistry;
    private final BackGroundTaskListenerController backGroundTaskListenerController;
    private final BackGroundTaskDispatcher backGroundTaskDispatcher;
    private final BackGroundTaskProperties backGroundTaskProperties;

    @Autowired
    public BackGroundTaskBalancer(IBackGroundTaskRegistry backGroundTaskRegistry,
                                  BackGroundTaskListenerController backGroundTaskListenerController,
                                  BackGroundTaskDispatcher backGroundTaskDispatcher,
                                  BackGroundTaskProperties backGroundTaskProperties) {
        this.backGroundTaskRegistry = backGroundTaskRegistry;
        this.backGroundTaskListenerController = backGroundTaskListenerController;
        this.backGroundTaskDispatcher = backGroundTaskDispatcher;
        this.backGroundTaskProperties = backGroundTaskProperties;
    }

    public boolean balance() {
        if (!backGroundTaskProperties.getBalancer().getEnabled()) {
            log.info("background-task balancer not enabled, you can enable it using config");
            return false;
        }
        // 1.统计所有任务的占用资源（线程等）总数
        // TODO
        // 2.统计所有实例数量
        // TODO
        // 3.计算每个实例应该承担的平均值
        // TODO
        int averageResourceCost = 30;
        // 4.计算当前实例资源占用值
        int currentResourceCost = calcCurrentResourceCost();
        // 5.如果当前实例的资源占用高于平均值，则将任务均衡到其他实例
        if (currentResourceCost > averageResourceCost) {
            StopWatch watch = new StopWatch();

            // 关闭任务监听器
            watch.start("closeTaskListener");
            closeTaskListener();
            watch.stop();

            // 选取一批任务优雅终止
            watch.start("chooseTask");
            int deltaResourceCost = currentResourceCost - averageResourceCost;
            List<IBackGroundTask> sortedTaskList = getSortedByTenantTaskList();
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
        }
        return true;
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
            if (accumulatedCost >= targetResourceCost) {
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
     * 关闭任务监听器
     */
    private void closeTaskListener() {
        backGroundTaskListenerController.stop();
    }

    /**
     * 发送任务至队列
     */
    private void sendTaskToQueue(TaskEntity taskEntity) {
        backGroundTaskDispatcher.dispatch(taskEntity);
    }
}
