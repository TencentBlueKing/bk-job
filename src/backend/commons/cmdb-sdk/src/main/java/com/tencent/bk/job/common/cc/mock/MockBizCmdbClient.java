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

package com.tencent.bk.job.common.cc.mock;

import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.model.result.HostProp;
import com.tencent.bk.job.common.cc.model.result.HostWithModules;
import com.tencent.bk.job.common.cc.model.result.ModuleProp;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.FlowController;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.experimental.Delegate;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class MockBizCmdbClient implements IBizCmdbClient {

    @Delegate
    private final IBizCmdbClient proxy;

    public MockBizCmdbClient(AppProperties appProperties,
                             BkApiGatewayProperties bkApiGatewayProperties,
                             CmdbConfig cmdbConfig,
                             String lang,
                             ThreadPoolExecutor cmdbThreadPoolExecutor,
                             ThreadPoolExecutor cmdbLongTermThreadPoolExecutor,
                             MeterRegistry meterRegistry,
                             ObjectProvider<FlowController> flowControllerProvider,
                             TenantEnvService tenantEnvService,
                             IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        this.proxy = new BizCmdbClient(
            appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            lang,
            cmdbThreadPoolExecutor,
            cmdbLongTermThreadPoolExecutor,
            flowControllerProvider.getIfAvailable(),
            meterRegistry,
            tenantEnvService,
            virtualAdminAccountProvider
        );
    }

    @Override
    public List<ApplicationHostDTO> listHostsByHostIds(String tenantId, List<Long> hostIds) {
        if ("system".equals(tenantId)
            && hostIds.size() == 1
            && hostIds.get(0) == 3L
        ) {
            ApplicationHostDTO host = new ApplicationHostDTO();
            host.setHostId(3L);
            host.setBizId(1L);
            host.setIp("127.0.0.3");
            host.setHostName("MockHost1");
            host.setGseAgentStatus(-2);
            host.setAgentId(null);
            host.setCloudAreaId(8L);
            host.setCloudAreaName("MockCloudArea1");
            host.setTenantId(tenantId);
            return Collections.singletonList(host);
        } else {
            return proxy.listHostsByHostIds(tenantId, hostIds);
        }
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
        bizNode.setChild(setList);
        return bizNode;
    }

    public List<HostWithModules> mockHostRelationsByTopology(String tenantId,
                                                             long bizId,
                                                             List<CcInstanceDTO> ccInstList) {
        if ("system".equals(tenantId)
            && bizId == 1L
            && ccInstList.size() == 1
            && ccInstList.get(0).getObjectType().equals("biz")
            && ccInstList.get(0).getInstanceId().equals(1L)
        ) {
            HostProp host = new HostProp();
            host.setHostId(1L);
            host.setIp("127.0.0.1");
            host.setIpv6(null);
            host.setAgentId("MockAgentId1");
            host.setHostName("MockHost1");
            host.setOsName("Linux");
            host.setOsType("1");
            host.setCloudAreaId(1L);
            host.setCloudVendorId("MockCloudVendor1");
            host.setLastTime("2025-03-06T00:00:00.001+08:00");
            host.setTenantId(tenantId);
            List<ModuleProp> modules = new ArrayList<>();
            ModuleProp moduleProp = new ModuleProp();
            moduleProp.setModuleId(2L);
            moduleProp.setSetId(2L);
            moduleProp.setLastTime("2025-03-06T00:00:00.002+08:00");
            modules.add(moduleProp);
            HostWithModules hostWithModules = new HostWithModules();
            hostWithModules.setHost(host);
            hostWithModules.setModules(modules);
            return Collections.singletonList(hostWithModules);
        }
        return Collections.emptyList();
    }

    public List<ApplicationHostDTO> mockHosts(String tenantId, long bizId, List<CcInstanceDTO> ccInstList) {
        if ("system".equals(tenantId)
            && bizId == 1L
            && ccInstList.size() == 1
            && ccInstList.get(0).getObjectType().equals("biz")
            && ccInstList.get(0).getInstanceId().equals(1L)
        ) {
            ApplicationHostDTO host = new ApplicationHostDTO();
            host.setHostId(1L);
            host.setBizId(1L);
            host.setIp("127.0.0.1");
            host.setHostName("MockHost1");
            host.setGseAgentStatus(0);
            host.setAgentId("MockAgentId1");
            host.setCloudAreaId(1L);
            host.setCloudAreaName("MockCloudArea1");
            host.setTenantId(tenantId);
            return Collections.singletonList(host);
        }
        return Collections.emptyList();
    }
}
