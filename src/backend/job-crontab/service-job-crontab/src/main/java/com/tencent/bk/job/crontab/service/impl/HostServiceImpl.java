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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.crontab.client.ServiceHostResourceClient;
import com.tencent.bk.job.crontab.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetHostsReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HostServiceImpl implements HostService {

    private final ServiceHostResourceClient serviceHostResourceClient;

    @Autowired
    public HostServiceImpl(ServiceHostResourceClient serviceHostResourceClient) {
        this.serviceHostResourceClient = serviceHostResourceClient;
    }

    private void fillHostInfo(HostDTO hostDTO, ServiceHostDTO serviceHostDTO) {
        if (hostDTO == null || serviceHostDTO == null) {
            return;
        }
        hostDTO.setHostId(serviceHostDTO.getHostId());
        hostDTO.setBkCloudId(serviceHostDTO.getCloudAreaId());
        hostDTO.setIp(serviceHostDTO.getIp());
    }

    private String buildCloudIp(ServiceHostDTO serviceHostDTO) {
        Long cloudAreaId = serviceHostDTO.getCloudAreaId();
        String ip = serviceHostDTO.getIp();
        return buildCloudIp(cloudAreaId, ip);
    }

    private String buildCloudIp(Long cloudAreaId, String ip) {
        if (cloudAreaId != null && StringUtils.isNotBlank(ip)) {
            return cloudAreaId + ":" + ip;
        }
        return null;
    }

    @Override
    public int fillHosts(List<HostDTO> hostList) {
        if (CollectionUtils.isEmpty(hostList)) {
            return 0;
        }
        InternalResponse<List<ServiceHostDTO>> resp =
            serviceHostResourceClient.batchGetHosts(new ServiceBatchGetHostsReq(hostList));
        List<ServiceHostDTO> serviceHostDTOList = resp.getData();
        if (CollectionUtils.isEmpty(serviceHostDTOList)) {
            log.info(
                "cannot find any host details by {}",
                hostList.stream().map(HostDTO::getUniqueKey).collect(Collectors.toList())
            );
            return 0;
        }
        Map<Long, ServiceHostDTO> hostIdMap = new HashMap<>();
        Map<String, ServiceHostDTO> cloudIpMap = new HashMap<>();
        serviceHostDTOList.forEach(serviceHostDTO -> {
            hostIdMap.put(serviceHostDTO.getHostId(), serviceHostDTO);
            cloudIpMap.put(buildCloudIp(serviceHostDTO), serviceHostDTO);
        });
        int filledCount = 0;
        Set<Long> noHostIds = new HashSet<>();
        Set<String> noHostCloudIps = new HashSet<>();
        for (HostDTO hostDTO : hostList) {
            Long hostId = hostDTO.getHostId();
            String cloudIp = buildCloudIp(hostDTO.getBkCloudId(), hostDTO.getIp());
            if (hostId != null) {
                ServiceHostDTO serviceHostDTO = hostIdMap.get(hostId);
                if (serviceHostDTO == null) {
                    noHostIds.add(hostId);
                } else {
                    fillHostInfo(hostDTO, serviceHostDTO);
                    filledCount += 1;
                }
            } else if (StringUtils.isNotBlank(cloudIp)) {
                ServiceHostDTO serviceHostDTO = cloudIpMap.get(cloudIp);
                if (serviceHostDTO == null) {
                    noHostCloudIps.add(cloudIp);
                } else {
                    fillHostInfo(hostDTO, serviceHostDTO);
                    filledCount += 1;
                }
            } else {
                log.warn("host does not contains hostId/cloudIp:{}", hostDTO);
            }
        }
        if (CollectionUtils.isNotEmpty(noHostIds) || CollectionUtils.isNotEmpty(noHostCloudIps)) {
            log.info(
                "cannot find host by cloudIps:{}, hostIds:{}, which may be removed from cmdb",
                noHostCloudIps,
                noHostIds
            );
        }
        return filledCount;
    }
}
