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
import com.tencent.bk.job.manage.background.event.cmdb.TenantBizEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.TenantBizSetEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.TenantBizSetRelationEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.TenantHostEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.TenantHostRelationEventWatcher;
import com.tencent.bk.job.manage.config.JobManageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 资源消耗计算器
 */
@Slf4j
@Service
public class ResourceCostCalculator {

    private final JobManageConfig jobManageConfig;
    private final IUserApiClient userApiClient;
    private final DiscoveryClient discoveryClient;

    @Autowired
    public ResourceCostCalculator(JobManageConfig jobManageConfig,
                                  IUserApiClient userApiClient,
                                  DiscoveryClient discoveryClient) {
        this.jobManageConfig = jobManageConfig;
        this.userApiClient = userApiClient;
        this.discoveryClient = discoveryClient;
    }

    /**
     * 计算平均每个实例应当承担的资源消耗，向上取整
     *
     * @return 资源消耗值
     */
    public int calcAverageResourceCostForOneInstance() {
        // 1.统计所有任务的占用资源（线程等）总数
        int totalResourceCost = calcResourceCostForAllTenantTasks();
        // 2.统计所有实例数量
        int totalInstanceCount = getJobManageInstanceCount();
        // 3.计算平均每个实例应当承担的资源消耗，向上取整
        return (int) Math.ceil((double) totalResourceCost / totalInstanceCount);
    }

    /**
     * 获取job-manage服务实例数量
     *
     * @return 服务实例数量
     */
    private int getJobManageInstanceCount() {
        String jobManageServiceName = "job-manage";
        return discoveryClient.getInstances(jobManageServiceName).size();
    }

    /**
     * 计算所有租户下所有任务的资源消耗
     *
     * @return 资源消耗值
     */
    public int calcResourceCostForAllTenantTasks() {
        List<OpenApiTenant> tenantList = userApiClient.listAllTenant();
        return tenantList.size() * calcResourceCostForTasksOfOneTenant();
    }

    /**
     * 计算一个租户下所有任务的资源消耗
     *
     * @return 资源消耗值
     */
    private int calcResourceCostForTasksOfOneTenant() {
        int resourceCost = 0;
        resourceCost += TenantBizEventWatcher.resourceCostForWatcher();
        resourceCost += TenantBizSetEventWatcher.resourceCostForWatcher();
        resourceCost += TenantBizSetRelationEventWatcher.resourceCostForWatcher();
        resourceCost += TenantHostEventWatcher.resourceCostForWatcher() + jobManageConfig.getHostEventHandlerNum();
        resourceCost += TenantHostRelationEventWatcher.resourceCostForWatcherAndHandler();
        return resourceCost;
    }
}
