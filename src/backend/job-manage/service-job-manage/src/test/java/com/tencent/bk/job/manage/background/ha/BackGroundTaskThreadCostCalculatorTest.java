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
import com.tencent.bk.job.manage.config.JobManageConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 线程消耗计算器单元测试
 */
@ExtendWith(MockitoExtension.class)
public class BackGroundTaskThreadCostCalculatorTest {

    @Mock
    private JobManageConfig jobManageConfig;

    @Mock
    private IUserApiClient userApiClient;

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private ServiceInstance serviceInstance;

    private BackGroundTaskThreadCostCalculator backGroundTaskThreadCostCalculator;

    @BeforeEach
    public void init() {
        backGroundTaskThreadCostCalculator = new BackGroundTaskThreadCostCalculator(
            jobManageConfig,
            userApiClient,
            discoveryClient
        );
    }

    @Test
    @DisplayName("calcThreadCostForTasksOfOneTenant - 默认配置下，单个租户所有任务总共消耗9个线程")
    public void testCalcThreadCostForTasksOfOneTenant() {
        when(jobManageConfig.getHostEventHandlerNum()).thenReturn(3);
        int result = backGroundTaskThreadCostCalculator.calcThreadCostForTasksOfOneTenant();
        assertEquals(9, result);
    }

    @Test
    @DisplayName("calcAverageThreadCostForOneInstance - 计算每个实例应承担的平均线程消耗 - 1租户1实例")
    public void testCalcAverageThreadCostForOneInstanceWhen1Tenant1Instance() {
        // 准备测试数据：1租户1实例
        when(jobManageConfig.getHostEventHandlerNum()).thenReturn(3);
        List<OpenApiTenant> tenantList = Collections.singletonList(createTenant("tenant1"));
        when(userApiClient.listAllTenant()).thenReturn(tenantList);
        when(discoveryClient.getInstances("job-manage"))
            .thenReturn(Collections.singletonList(serviceInstance));

        // 执行测试
        int result = backGroundTaskThreadCostCalculator.calcAverageThreadCostForOneInstance();

        // 验证结果：9个线程由单个实例完全承担
        assertEquals(9, result);
    }

    @Test
    @DisplayName("calcAverageThreadCostForOneInstance - 计算每个实例应承担的平均线程消耗 - 3租户2实例")
    public void testCalcAverageThreadCostForOneInstanceWhen3Tenant2Instance() {
        // 准备测试数据：3租户2实例
        when(jobManageConfig.getHostEventHandlerNum()).thenReturn(3);
        List<OpenApiTenant> tenantList = Arrays.asList(
            createTenant("tenant1"),
            createTenant("tenant2"),
            createTenant("tenant3")
        );
        when(userApiClient.listAllTenant()).thenReturn(tenantList);
        when(discoveryClient.getInstances("job-manage")).thenReturn(
            Arrays.asList(serviceInstance, serviceInstance)
        );

        // 执行测试
        int result = backGroundTaskThreadCostCalculator.calcAverageThreadCostForOneInstance();

        // 验证结果：3*9/2=13.5，向上取整为14
        assertEquals(14, result);
    }

    @Test
    @DisplayName("calcAverageThreadCostForOneInstance - 计算每个实例应承担的平均线程消耗 - 30租户0实例")
    public void testCalcAverageThreadCostForOneInstanceWhen30Tenant0Instance() {
        // 准备测试数据：30租户0实例
        when(jobManageConfig.getHostEventHandlerNum()).thenReturn(3);
        List<OpenApiTenant> tenantList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            tenantList.add(createTenant("tenant" + i));
        }
        when(userApiClient.listAllTenant()).thenReturn(tenantList);
        when(discoveryClient.getInstances("job-manage")).thenReturn(Collections.emptyList());

        // 执行测试
        int result = backGroundTaskThreadCostCalculator.calcAverageThreadCostForOneInstance();

        // 验证结果：启动过程中健康实例数为0的情况下，单个实例最大承担200线程，待后续实例正常后均衡
        assertEquals(200, result);
    }

    private OpenApiTenant createTenant(String tenantId) {
        OpenApiTenant tenant = new OpenApiTenant();
        tenant.setId(tenantId);
        tenant.setName("租户：" + tenantId);
        return tenant;
    }
}
