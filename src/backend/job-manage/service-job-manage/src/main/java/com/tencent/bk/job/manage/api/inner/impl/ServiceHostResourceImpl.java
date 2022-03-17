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

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.DynamicGroupInfoDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.manage.api.inner.ServiceHostResource;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceCheckAppHostsReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByDynamicGroupReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByIpReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceGetHostStatusByNodeReq;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;
import com.tencent.bk.job.manage.model.web.vo.NodeInfoVO;
import com.tencent.bk.job.manage.service.HostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceHostResourceImpl implements ServiceHostResource {
    private final HostService hostService;

    @Autowired
    public ServiceHostResourceImpl(HostService hostService) {
        this.hostService = hostService;
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByNode(
        Long appId,
        String username,
        ServiceGetHostStatusByNodeReq req
    ) {
        List<AppTopologyTreeNode> treeNodeList = req.getTreeNodeList();
        List<NodeInfoVO> nodeInfoVOList = hostService.getHostsByNode(username, appId, treeNodeList);
        List<ServiceHostStatusDTO> hostStatusDTOList = new ArrayList<>();
        nodeInfoVOList.parallelStream().forEach(nodeInfoVO -> {
            nodeInfoVO.getIpListStatus().forEach(hostInfoVO -> {
                ServiceHostStatusDTO serviceHostStatusDTO = new ServiceHostStatusDTO();
                serviceHostStatusDTO.setHostId(hostInfoVO.getHostId());
                serviceHostStatusDTO.setIp(hostInfoVO.getIp());
                serviceHostStatusDTO.setAlive(hostInfoVO.getAlive());
                if (!hostStatusDTOList.contains(serviceHostStatusDTO)) {
                    hostStatusDTOList.add(serviceHostStatusDTO);
                }
            });
        });
        return InternalResponse.buildSuccessResp(hostStatusDTOList);
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByDynamicGroup(
        Long appId,
        String username,
        ServiceGetHostStatusByDynamicGroupReq req
    ) {
        List<String> dynamicGroupIdList = req.getDynamicGroupIdList();
        List<DynamicGroupInfoDTO> dynamicGroupInfoDTOList = hostService.getDynamicGroupHostList(
            username,
            appId,
            dynamicGroupIdList
        );
        List<ServiceHostStatusDTO> hostStatusDTOList = new ArrayList<>();
        dynamicGroupInfoDTOList.parallelStream().forEach(dynamicGroupInfoDTO -> {
            dynamicGroupInfoDTO.getIpListStatus().forEach(hostInfoVO -> {
                ServiceHostStatusDTO serviceHostStatusDTO = new ServiceHostStatusDTO();
                serviceHostStatusDTO.setHostId(hostInfoVO.getHostId());
                serviceHostStatusDTO.setIp(hostInfoVO.getIp());
                serviceHostStatusDTO.setAlive(hostInfoVO.getGseAgentAlive() ? 1 : 0);
                if (!hostStatusDTOList.contains(serviceHostStatusDTO)) {
                    hostStatusDTOList.add(serviceHostStatusDTO);
                }
            });
        });
        return InternalResponse.buildSuccessResp(hostStatusDTOList);
    }

    @Override
    public InternalResponse<List<ServiceHostStatusDTO>> getHostStatusByIp(Long appId, String username,
                                                                          ServiceGetHostStatusByIpReq req) {
        List<String> ipList = req.getIpList();
        List<HostInfoVO> hostInfoVOList = hostService.getHostsByIp(username, appId, null, ipList);
        List<ServiceHostStatusDTO> hostStatusDTOList = new ArrayList<>();
        hostInfoVOList.forEach(hostInfoVO -> {
            ServiceHostStatusDTO serviceHostStatusDTO = new ServiceHostStatusDTO();
            serviceHostStatusDTO.setHostId(hostInfoVO.getHostId());
            serviceHostStatusDTO.setIp(hostInfoVO.getCloudAreaInfo().getId() + ":" + hostInfoVO.getIp());
            serviceHostStatusDTO.setAlive(hostInfoVO.getAlive());
            if (!hostStatusDTOList.contains(serviceHostStatusDTO)) {
                hostStatusDTOList.add(serviceHostStatusDTO);
            }
        });
        return InternalResponse.buildSuccessResp(hostStatusDTOList);
    }

    @Override
    public InternalResponse<List<IpDTO>> checkAppHosts(Long appId,
                                                       ServiceCheckAppHostsReq req) {
        return InternalResponse.buildSuccessResp(hostService.checkAppHosts(appId, req.getHosts()));
    }

    @Override
    public InternalResponse<List<ServiceHostDTO>> batchGetHosts(List<IpDTO> hostIps) {
        List<ApplicationHostDTO> hosts = hostService.listHosts(hostIps);
        if (CollectionUtils.isEmpty(hosts)) {
            return InternalResponse.buildSuccessResp(null);
        }

        return InternalResponse.buildSuccessResp(
            hosts.stream()
                .map(host -> new ServiceHostDTO(host.getHostId(), host.getCloudAreaId(), host.getIp(), host.getAppId()))
                .collect(Collectors.toList()));
    }
}
