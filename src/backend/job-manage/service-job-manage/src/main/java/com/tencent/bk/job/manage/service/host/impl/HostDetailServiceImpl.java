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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.cc.service.CloudAreaService;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class HostDetailServiceImpl implements HostDetailService {

    private final ScopeHostService scopeHostService;
    private final AgentStatusService agentStatusService;
    private final CloudVendorService cloudVendorService;

    @Autowired
    public HostDetailServiceImpl(ScopeHostService scopeHostService,
                                 AgentStatusService agentStatusService,
                                 CloudVendorService cloudVendorService) {
        this.scopeHostService = scopeHostService;
        this.agentStatusService = agentStatusService;
        this.cloudVendorService = cloudVendorService;
    }

    @Override
    public List<ApplicationHostDTO> listHostDetails(AppResourceScope appResourceScope, Collection<Long> hostIds) {
        List<ApplicationHostDTO> scopeHostList = scopeHostService.getScopeHostsByIds(appResourceScope, hostIds);
        // 填充实时agent状态
        agentStatusService.fillRealTimeAgentStatus(scopeHostList);
        fillCloudInfoForHosts(scopeHostList);
        return scopeHostList;
    }

    @Override
    public void fillCloudInfoForHosts(List<ApplicationHostDTO> hostList) {
        for (ApplicationHostDTO host : hostList) {
            String cloudVendorId = host.getCloudVendorId();
            host.setCloudAreaName(CloudAreaService.getCloudAreaNameFromCache(host.getCloudAreaId()));
            host.setCloudVendorName(cloudVendorService.getCloudVendorNameOrDefault(
                cloudVendorId,
                cloudVendorId == null ? null : "ID=" + host.getCloudVendorId()
                )
            );
        }
    }
}
