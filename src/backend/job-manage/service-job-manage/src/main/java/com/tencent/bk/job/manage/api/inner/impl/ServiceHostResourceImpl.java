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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.StackTraceUtil;
import com.tencent.bk.job.manage.api.inner.ServiceHostResource;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetAppHostsReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetHostToposReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetHostsReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByDynamicGroupReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByHostReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByNodeReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostsByCloudIpv6Req;
import com.tencent.bk.job.manage.model.inner.resp.ServiceHostTopoDTO;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.BizTopoHostService;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import com.tencent.bk.job.manage.service.host.ScopeCachedHostService;
import com.tencent.bk.job.manage.service.host.ScopeDynamicGroupHostService;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
    private final TenantService tenantService;
    private final TenantHostService tenantHostService;
    private final ScopeCachedHostService scopeCachedHostService;
    private final NoTenantHostService noTenantHostService;
    private final BizTopoHostService bizTopoHostService;
    private final ScopeDynamicGroupHostService scopeDynamicGroupHostService;
    private final HostDetailService hostDetailService;
    private final HostTopoDAO hostTopoDAO;

    @Autowired
    public ServiceHostResourceImpl(AppScopeMappingService appScopeMappingService,
                                   ApplicationService applicationService,
                                   TenantService tenantService,
                                   TenantHostService tenantHostService,
                                   ScopeCachedHostService scopeCachedHostService,
                                   NoTenantHostService noTenantHostService,
                                   BizTopoHostService bizTopoHostService,
                                   ScopeDynamicGroupHostService scopeDynamicGroupHostService,
                                   HostDetailService hostDetailService,
                                   HostTopoDAO hostTopoDAO) {
        this.appScopeMappingService = appScopeMappingService;
        this.applicationService = applicationService;
        this.tenantService = tenantService;
        this.tenantHostService = tenantHostService;
        this.scopeCachedHostService = scopeCachedHostService;
        this.noTenantHostService = noTenantHostService;
        this.bizTopoHostService = bizTopoHostService;
        this.scopeDynamicGroupHostService = scopeDynamicGroupHostService;
        this.hostDetailService = hostDetailService;
        this.hostTopoDAO = hostTopoDAO;
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
        hostList.forEach(host -> {
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
        List<ApplicationHostDTO> hostList = scopeDynamicGroupHostService.listHostByDynamicGroups(
            appDTO.getTenantId(),
            appResourceScope,
            dynamicGroupIdList
        );
        Set<ServiceHostStatusDTO> hostStatusDTOSet = new HashSet<>();
        hostList.forEach(applicationHostDTO -> {
            ServiceHostStatusDTO serviceHostStatusDTO = new ServiceHostStatusDTO();
            serviceHostStatusDTO.setHostId(applicationHostDTO.getHostId());
            serviceHostStatusDTO.setAlive(applicationHostDTO.getGseAgentAlive() ? 1 : 0);
            hostStatusDTOSet.add(serviceHostStatusDTO);
        });
        return InternalResponse.buildSuccessResp(new ArrayList<>(hostStatusDTOSet));
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByHost(Long appId,
                                                                            ServiceGetHostStatusByHostReq req) {
        String tenantId = tenantService.getTenantIdByAppId(appId);
        List<ApplicationHostDTO> hostDTOList = tenantHostService.listHosts(tenantId, req.getHostList());
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
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(appId);
        ServiceListAppHostResultDTO result = scopeCachedHostService.listAppHostsPreferCache(
            appResourceScope,
            req.getHosts(),
            req.isRefreshAgentId()
        );
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<List<ServiceHostDTO>> batchGetHostsFromCacheOrDB(ServiceBatchGetHostsReq req) {
        List<HostDTO> queryHosts = req.getHosts();
        List<ApplicationHostDTO> hosts = noTenantHostService.listHostsFromCacheOrDB(queryHosts);
        if (CollectionUtils.isEmpty(hosts)) {
            return InternalResponse.buildSuccessResp(Collections.emptyList());
        }
        String tenantId = getTenantIdWithDefault(req, hosts);
        hostDetailService.fillDetailForApplicationHosts(tenantId, hosts);

        return InternalResponse.buildSuccessResp(
            hosts.stream()
                .map(ServiceHostDTO::fromApplicationHostDTO)
                .collect(Collectors.toList()));
    }

    @Deprecated
    @CompatibleImplementation(
        name = "tenant",
        explain = "兼容发布过程中老的调用，发布完成后删除",
        deprecatedVersion = "3.12.x",
        type = CompatibleType.DEPLOY
    )
    private String getTenantIdWithDefault(ServiceBatchGetHostsReq req, List<ApplicationHostDTO> hosts) {
        if (req.getTenantId() != null) {
            return req.getTenantId();
        }
        if (CollectionUtils.isNotEmpty(hosts)) {
            log.warn(
                "CompatibleImplementation: getTenantIdWithDefault is still work with hosts, please check stack:{}",
                StackTraceUtil.getCurrentStackTrace()
            );
            String tenantId = hosts.get(0).getTenantId();
            if (StringUtils.isNotBlank(tenantId)) {
                return tenantId;
            }
        }
        log.warn(
            "CompatibleImplementation: getTenantIdWithDefault is still work with default, please check stack:{}",
            StackTraceUtil.getCurrentStackTrace()
        );
        return TenantIdConstants.DEFAULT_TENANT_ID;
    }

    @Override
    public InternalResponse<List<ServiceHostDTO>> getHostsByCloudIpv6(ServiceGetHostsByCloudIpv6Req req) {
        addDefaultTenant(req);
        List<ApplicationHostDTO> hosts = tenantHostService.listHostsByCloudIpv6(
            req.getTenantId(),
            req.getCloudAreaId(),
            req.getIpv6()
        );
        if (CollectionUtils.isEmpty(hosts)) {
            return InternalResponse.buildSuccessResp(Collections.emptyList());
        }
        hostDetailService.fillDetailForApplicationHosts(req.getTenantId(), hosts);
        return InternalResponse.buildSuccessResp(
            hosts.stream()
                .map(ServiceHostDTO::fromApplicationHostDTO)
                .collect(Collectors.toList()));
    }

    @Deprecated
    @CompatibleImplementation(
        name = "tenant",
        explain = "兼容发布过程中老的调用，发布完成后删除",
        deprecatedVersion = "3.12.x",
        type = CompatibleType.DEPLOY
    )
    private void addDefaultTenant(ServiceGetHostsByCloudIpv6Req req) {
        if (req.getTenantId() != null) {
            return;
        }
        log.warn("CompatibleImplementation: addDefaultTenant is still work with default, please check");
        req.setTenantId(TenantIdConstants.DEFAULT_TENANT_ID);
    }

    @Override
    public InternalResponse<List<ServiceHostTopoDTO>> batchGetHostTopos(ServiceBatchGetHostToposReq req) {
        List<HostTopoDTO> hostTopoDTOList = hostTopoDAO.listHostTopoByHostIds(req.getHostIdList());
        List<ServiceHostTopoDTO> serviceHostTopoDTOList = hostTopoDTOList.stream()
            .map(HostTopoDTO::toServiceHostTopoDTO)
            .collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(serviceHostTopoDTOList);
    }

}
