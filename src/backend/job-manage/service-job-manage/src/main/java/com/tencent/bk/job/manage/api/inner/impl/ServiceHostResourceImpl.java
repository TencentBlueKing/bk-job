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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.inner.ServiceHostResource;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetAppHostsReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetHostsReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByDynamicGroupReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByHostReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByNodeReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostsByCloudIpv6Req;
import com.tencent.bk.job.manage.model.web.request.ipchooser.BizTopoNode;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.BizTopoHostService;
import com.tencent.bk.job.manage.service.host.HostService;
import com.tencent.bk.job.manage.service.host.impl.BizDynamicGroupHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceHostResourceImpl implements ServiceHostResource {
    private final AppScopeMappingService appScopeMappingService;
    private final ApplicationService applicationService;
    private final HostService hostService;
    private final BizTopoHostService bizTopoHostService;
    private final BizDynamicGroupHostService bizDynamicGroupHostService;

    @Autowired
    public ServiceHostResourceImpl(AppScopeMappingService appScopeMappingService,
                                   ApplicationService applicationService,
                                   HostService hostService,
                                   BizTopoHostService bizTopoHostService,
                                   BizDynamicGroupHostService bizDynamicGroupHostService) {
        this.appScopeMappingService = appScopeMappingService;
        this.applicationService = applicationService;
        this.hostService = hostService;
        this.bizTopoHostService = bizTopoHostService;
        this.bizDynamicGroupHostService = bizDynamicGroupHostService;
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByNode(Long appId,
                                                                            ServiceGetHostStatusByNodeReq req) {
        ApplicationDTO appDTO = applicationService.getAppByAppId(appId);
        if (appDTO.isBizSet()) {
            String msg = "topo node of bizset not supported yet";
            throw new NotImplementedException(msg, ErrorCode.NOT_SUPPORT_FEATURE);
        }
        List<BizTopoNode> treeNodeList = req.getTreeNodeList();
        List<ApplicationHostDTO> hostList = bizTopoHostService.listHostByNodes(appDTO.getBizIdIfBizApp(), treeNodeList);
        Set<ServiceHostStatusDTO> hostStatusDTOSet = new HashSet<>();
        hostList.parallelStream().forEach(host -> {
            ServiceHostStatusDTO serviceHostStatusDTO = new ServiceHostStatusDTO();
            serviceHostStatusDTO.setHostId(host.getHostId());
            serviceHostStatusDTO.setAlive(host.getAgentStatusValue());
            hostStatusDTOSet.add(serviceHostStatusDTO);
        });
        return InternalResponse.buildSuccessResp(new ArrayList<>(hostStatusDTOSet));
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByDynamicGroup(
        Long appId,
        ServiceGetHostStatusByDynamicGroupReq req
    ) {
        ApplicationDTO appDTO = applicationService.getAppByAppId(appId);
        if (appDTO.isBizSet()) {
            String msg = "dynamic group of bizset not supported yet";
            throw new NotImplementedException(msg, ErrorCode.NOT_SUPPORT_FEATURE);
        }
        List<String> dynamicGroupIdList = req.getDynamicGroupIdList();
        AppResourceScope appResourceScope = new AppResourceScope(appId);
        appScopeMappingService.fillAppResourceScope(appResourceScope);
        List<ApplicationHostDTO> hostList = bizDynamicGroupHostService.listHostByDynamicGroups(
            appResourceScope,
            dynamicGroupIdList
        );
        Set<ServiceHostStatusDTO> hostStatusDTOSet = new HashSet<>();
        hostList.forEach(hostInfoVO -> {
            ServiceHostStatusDTO serviceHostStatusDTO = new ServiceHostStatusDTO();
            serviceHostStatusDTO.setHostId(hostInfoVO.getHostId());
            serviceHostStatusDTO.setAlive(hostInfoVO.getGseAgentAlive() ? 1 : 0);
            hostStatusDTOSet.add(serviceHostStatusDTO);
        });
        return InternalResponse.buildSuccessResp(new ArrayList<>(hostStatusDTOSet));
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByHost(Long appId,
                                                                            ServiceGetHostStatusByHostReq req) {
        List<ApplicationHostDTO> hostDTOList = hostService.listHosts(req.getHostList());
        List<ServiceHostStatusDTO> hostStatusDTOList = new ArrayList<>();
        hostDTOList.forEach(host -> {
            ServiceHostStatusDTO hostStatusDTO = new ServiceHostStatusDTO();
            hostStatusDTO.setHostId(host.getHostId());
            hostStatusDTO.setAlive(host.getAgentStatusValue());
            if (!hostStatusDTOList.contains(hostStatusDTO)) {
                hostStatusDTOList.add(hostStatusDTO);
            }
        });
        return InternalResponse.buildSuccessResp(hostStatusDTOList);
    }

    @Override
    public InternalResponse<ServiceListAppHostResultDTO> batchGetAppHosts(Long appId,
                                                                          ServiceBatchGetAppHostsReq req) {
        req.validate();
        return InternalResponse.buildSuccessResp(
            hostService.listAppHostsPreferCache(appId, req.getHosts(), req.isRefreshAgentId()));
    }

    @Override
    public InternalResponse<List<ServiceHostDTO>> batchGetHosts(ServiceBatchGetHostsReq req) {
        List<HostDTO> queryHosts = req.getHosts();
        List<ApplicationHostDTO> hosts = hostService.listHosts(queryHosts);
        if (CollectionUtils.isEmpty(hosts)) {
            return InternalResponse.buildSuccessResp(Collections.emptyList());
        }

        return InternalResponse.buildSuccessResp(
            hosts.stream()
                .map(ServiceHostDTO::fromApplicationHostDTO)
                .collect(Collectors.toList()));
    }

    @Override
    public InternalResponse<List<ServiceHostDTO>> getHostsByCloudIpv6(ServiceGetHostsByCloudIpv6Req req) {
        List<ApplicationHostDTO> hosts = hostService.listHostsByCloudIpv6(req.getCloudAreaId(), req.getIpv6());
        if (CollectionUtils.isEmpty(hosts)) {
            return InternalResponse.buildSuccessResp(Collections.emptyList());
        }

        return InternalResponse.buildSuccessResp(
            hosts.stream()
                .map(ServiceHostDTO::fromApplicationHostDTO)
                .collect(Collectors.toList()));
    }
}
