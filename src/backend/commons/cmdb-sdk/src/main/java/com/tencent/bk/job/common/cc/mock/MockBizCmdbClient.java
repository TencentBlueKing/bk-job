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

package com.tencent.bk.job.common.cc.mock;

import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.constants.EsbLang;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.FlowController;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.experimental.Delegate;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class MockBizCmdbClient implements IBizCmdbClient {

    @Delegate
    private final IBizCmdbClient proxy;

    public MockBizCmdbClient(AppProperties appProperties,
                             BkApiGatewayProperties bkApiGatewayProperties,
                             CmdbConfig cmdbConfig,
                             ThreadPoolExecutor cmdbThreadPoolExecutor,
                             ThreadPoolExecutor cmdbLongTermThreadPoolExecutor,
                             MeterRegistry meterRegistry,
                             ObjectProvider<FlowController> flowControllerProvider,
                             TenantEnvService tenantEnvService) {
        this.proxy = new BizCmdbClient(appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            EsbLang.EN,
            cmdbThreadPoolExecutor,
            cmdbLongTermThreadPoolExecutor,
            flowControllerProvider.getIfAvailable(),
            meterRegistry,
            tenantEnvService
        );
    }

    private InstanceTopologyDTO mockInstanceTopologyDTO() {
        InstanceTopologyDTO bizNode = new InstanceTopologyDTO();
        bizNode.setObjectId("biz");
        bizNode.setInstanceId(1L);
        bizNode.setInstanceName("蓝鲸");

        List<InstanceTopologyDTO> moduleList = new ArrayList<>();
        InstanceTopologyDTO idleMachineModule = new InstanceTopologyDTO();
        idleMachineModule.setObjectId("module");
        idleMachineModule.setInstanceId(2L);
        idleMachineModule.setInstanceName("空闲机");
        moduleList.add(idleMachineModule);
        InstanceTopologyDTO faultMachineModule = new InstanceTopologyDTO();
        faultMachineModule.setObjectId("module");
        faultMachineModule.setInstanceId(3L);
        faultMachineModule.setInstanceName("故障机");
        moduleList.add(faultMachineModule);
        InstanceTopologyDTO recycleMachineModule = new InstanceTopologyDTO();
        recycleMachineModule.setObjectId("module");
        recycleMachineModule.setInstanceId(4L);
        recycleMachineModule.setInstanceName("待回收");
        moduleList.add(recycleMachineModule);

        List<InstanceTopologyDTO> setList = new ArrayList<>();
        InstanceTopologyDTO idlePoolSet = new InstanceTopologyDTO();
        idlePoolSet.setObjectId("set");
        idlePoolSet.setInstanceId(2L);
        idlePoolSet.setInstanceName("空闲机池");
        idlePoolSet.setChild(moduleList);

        setList.add(idlePoolSet);
        idlePoolSet.setChild(setList);
        return idlePoolSet;
    }

    @Override
    public InstanceTopologyDTO getBizInstTopology(long bizId) {
        return mockInstanceTopologyDTO();
    }

    @Override
    public InstanceTopologyDTO getBizInstCompleteTopology(String tenantId, long bizId) {
        return mockInstanceTopologyDTO();
    }

    @Override
    public List<ApplicationHostDTO> getHosts(long bizId, List<CcInstanceDTO> ccInstList) {
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setHostId(1L);
        host.setBizId(1L);
        host.setIp("127.0.0.1");
        host.setHostName("MockHost1");
        host.setGseAgentStatus(0);
        host.setAgentId("MockAgentId1");
        host.setCloudAreaId(0L);
        host.setCloudAreaName("MockCloudArea1");
        return null;
    }
}
