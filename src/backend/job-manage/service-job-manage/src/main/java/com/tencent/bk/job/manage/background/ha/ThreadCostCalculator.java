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

import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.paas.user.IUserApiClient;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizSetEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.BizSetRelationEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.HostEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.watcher.HostRelationEventWatcher;
import com.tencent.bk.job.manage.config.JobManageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 线程消耗计算器
 */
@Slf4j
@Service
@SuppressWarnings("FieldCanBeLocal")
public class ThreadCostCalculator {

    /**
     * Job-Manage服务的Service名称
     */
    private final String SERVICE_NAME_JOB_MANAGE = "job-manage";
    /**
     * 所有实例都不健康时使用的默认平均线程消耗值
     */
    private final int DEFAULT_AVERAGE_THREAD_COST = 200;

    private final JobManageConfig jobManageConfig;
    private final IUserApiClient userApiClient;
    private final DiscoveryClient discoveryClient;

    @Autowired
    public ThreadCostCalculator(JobManageConfig jobManageConfig,
                                IUserApiClient userApiClient,
                                DiscoveryClient discoveryClient) {
        this.jobManageConfig = jobManageConfig;
        this.userApiClient = userApiClient;
        this.discoveryClient = discoveryClient;
    }

    /**
     * 计算平均每个实例应当承担的线程消耗，向上取整
     *
     * @return 线程消耗值
     */
    public int calcAverageThreadCostForOneInstance() {
        // 1.统计所有任务的占用线程总数
        int totalThreadCost = calcThreadCostForAllTenantTasks();
        // 2.统计所有实例数量
        int totalInstanceCount = getJobManageInstanceCount();
        if (totalInstanceCount == 0) {
            // 所有实例都不健康，使用默认值确保不接收过多任务，等待后续实例状态健康后再进行负载均衡
            log.info(
                "No healthy {} instance, use defaultThreadCost({}) for current instance",
                SERVICE_NAME_JOB_MANAGE,
                DEFAULT_AVERAGE_THREAD_COST
            );
            return DEFAULT_AVERAGE_THREAD_COST;
        }
        // 3.计算平均每个实例应当承担的线程消耗，向上取整
        return (int) Math.ceil((double) totalThreadCost / totalInstanceCount);
    }

    /**
     * 获取job-manage服务实例数量
     *
     * @return 服务实例数量
     */
    private int getJobManageInstanceCount() {
        return discoveryClient.getInstances(SERVICE_NAME_JOB_MANAGE).size();
    }

    /**
     * 计算所有租户下所有任务的线程消耗
     *
     * @return 线程消耗值
     */
    private int calcThreadCostForAllTenantTasks() {
        List<OpenApiTenant> tenantList = userApiClient.listAllTenant();
        return tenantList.size() * calcThreadCostForTasksOfOneTenant();
    }

    /**
     * 计算一个租户下所有任务的线程消耗
     *
     * @return 线程消耗值
     */
    int calcThreadCostForTasksOfOneTenant() {
        int threadCost = 0;
        threadCost += BizEventWatcher.threadCostForWatcher();
        threadCost += BizSetEventWatcher.threadCostForWatcher();
        threadCost += BizSetRelationEventWatcher.threadCostForWatcher();
        threadCost += HostEventWatcher.threadCostForWatcherAndHandler(jobManageConfig);
        threadCost += HostRelationEventWatcher.threadCostForWatcherAndHandler();
        return threadCost;
    }
}
