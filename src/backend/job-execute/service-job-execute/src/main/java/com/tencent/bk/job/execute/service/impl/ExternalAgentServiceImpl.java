/*
 *
 *  * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *  *
 *  * Copyright (C) 2021 Tencent.  All rights reserved.
 *  *
 *  * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *  *
 *  * License for BK-JOB蓝鲸智云作业平台:
 *  * --------------------------------------------------------------------
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 *  * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 *  * IN THE SOFTWARE.
 *
 */

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.DistributeFileFromExternalAgentException;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.service.model.HostAgentStateQuery;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.execute.config.NFSExternalAgentHostConfig;
import com.tencent.bk.job.execute.model.ExternalHostDTO;
import com.tencent.bk.job.execute.service.ExternalAgentService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 * 获取配置的外部有Gse agent的机器作为文件分发源
 */
@Slf4j
public class ExternalAgentServiceImpl implements ExternalAgentService {

    private final HostService hostService;
    private final AgentStateClient agentStateClient;
    private final TenantEnvService tenantEnvService;

    private final AtomicLong roundRobinCnt = new AtomicLong(0);
    private final List<HostDTO> externalAgentHostPool = new ArrayList<>();

    public ExternalAgentServiceImpl(NFSExternalAgentHostConfig externalAgentHostConfig,
                                    HostService hostService,
                                    AgentStateClient agentStateClient,
                                    TenantEnvService tenantEnvService) {
        this.hostService = hostService;
        this.agentStateClient = agentStateClient;
        this.tenantEnvService = tenantEnvService;
        initExternalHostsPool(externalAgentHostConfig);
    }

    @Override
    public HostDTO getDistributeSourceHost() {
        String tenantId = tenantEnvService.getJobMachineTenantId();
        Map<HostDTO, ServiceHostDTO> cmdbHostMap = hostService.batchGetHostsFromCacheOrDB(
            tenantId,
            externalAgentHostPool
        );
        List<ServiceHostDTO> cmdbHostList = new ArrayList<>();
        externalAgentHostPool.forEach(hostDTO -> {
            ServiceHostDTO serviceHostDTO = cmdbHostMap.get(hostDTO);
            if (serviceHostDTO != null) {
                cmdbHostList.add(serviceHostDTO);
            }
        });
        return ServiceHostDTO.toHostDTO(getOneAliveAgentHost(cmdbHostList));
    }

    private ServiceHostDTO getOneAliveAgentHost(List<ServiceHostDTO> hosts) {
        int hostCnt = hosts.size();
        List<HostAgentStateQuery> agentStateQueries = new ArrayList<>();
        hosts.forEach(host -> {
            HostAgentStateQuery query = new HostAgentStateQuery();
            query.setHostId(host.getHostId());
            query.setBizId(host.getBizId());
            query.setCloudIp(host.getCloudIp());
            query.setAgentId(host.getAgentId());
            agentStateQueries.add(query);
        });
        Map<String, Boolean> hostIpAliveStatusMap = agentStateClient.batchGetAgentAliveStatus(agentStateQueries);
        ServiceHostDTO sourceHost = null;
        // 同一个服务实例内通过RR获取分发的源主机，遍历配置的主机池直至找到一个 agent 状态正常的主机
        for (int i = 0; i < hostCnt; i++) {
            // 从上次使用的主机的下一个开始获取
            int idx = (i + (int) roundRobinCnt.get()) % hostCnt;
            ServiceHostDTO serviceHostDTO = hosts.get(idx);
            if (Boolean.TRUE.equals(hostIpAliveStatusMap.get(serviceHostDTO.getAgentId()))) {
                // 下一次获取，计数器从下一个开始
                roundRobinCnt.accumulateAndGet(i + 1, Long::sum);
                sourceHost = serviceHostDTO;
                break;
            }
        }

        if (sourceHost == null) {
            String msg = "Distribute file use external agent, but no available host found, please check configuration";
            log.error(msg);
            throw new DistributeFileFromExternalAgentException(msg, ErrorCode.INTERNAL_ERROR);
        }

        log.info(
            "Using external agent host:(cloudIp={}, hostId={}, agentId={}) to distribute file",
            sourceHost.getCloudIp(),
            sourceHost.getHostId(),
            sourceHost.getAgentId()
        );
        return sourceHost;
    }

    private void initExternalHostsPool(NFSExternalAgentHostConfig configFromDeployment) {
        externalAgentHostPool.addAll(
            configFromDeployment.getHosts()
                .stream()
                .map(ExternalHostDTO::convertToHostDTO)
                .collect(Collectors.toList())
        );
    }

}
