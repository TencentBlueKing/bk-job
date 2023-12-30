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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.cc.model.container.ContainerDetailDTO;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.service.ContainerService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContainerServiceImpl implements ContainerService {
    private final HostService hostService;
    private final BizCmdbClient cmdbClient;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public ContainerServiceImpl(HostService hostService, BizCmdbClient cmdbClient,
                                AppScopeMappingService appScopeMappingService) {
        this.hostService = hostService;
        this.cmdbClient = cmdbClient;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public List<Container> listContainerByIds(long appId, Collection<Long> ids) {
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        List<ContainerDetailDTO> containerDetailList =
            cmdbClient.listKubeContainerByIds(Long.parseLong(resourceScope.getId()), ids);
        if (CollectionUtils.isEmpty(containerDetailList)) {
            return Collections.emptyList();
        }
        List<Container> containers = containerDetailList.stream()
            .map(this::toContainer).collect(Collectors.toList());

        ServiceListAppHostResultDTO hostResult =
            hostService.batchGetAppHosts(
                appId,
                containers.stream()
                    .map(container -> new HostDTO(container.getNodeHostId()))
                    .collect(Collectors.toList()),
                false);
        Map<Long, HostDTO> hostMap = hostResult.getValidHosts().stream()
            .collect(Collectors.toMap(HostDTO::getHostId, host -> host));

        containers.forEach(container -> {
            HostDTO nodeHost = hostMap.get(container.getNodeHostId());
            container.setNodeAgentId(nodeHost.getAgentId());
        });

        return containers;
    }

    private Container toContainer(ContainerDetailDTO containerDetailDTO) {
        Container container = new Container();
        container.setId(containerDetailDTO.getContainer().getId());
        container.setContainerId(containerDetailDTO.getContainer().getContainerUID());
        container.setName(containerDetailDTO.getContainer().getName());
        container.setPodName(containerDetailDTO.getPod().getName());
        container.setPodLabels(containerDetailDTO.getPod().getLabels());
        container.setNodeHostId(containerDetailDTO.getTopo().getHostId());
        return container;
    }
}
