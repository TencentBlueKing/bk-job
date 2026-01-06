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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.manage.service.cloudarea.BkNetService;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.WhiteIpAwareScopeHostService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class HostDetailServiceImpl implements HostDetailService {

    private final WhiteIpAwareScopeHostService whiteIpAwareScopeHostService;
    private final AgentStatusService agentStatusService;
    private final CloudVendorService cloudVendorService;
    private final OsTypeService osTypeService;
    private final BkNetService bkNetService;

    @Autowired
    public HostDetailServiceImpl(WhiteIpAwareScopeHostService whiteIpAwareScopeHostService,
                                 AgentStatusService agentStatusService,
                                 CloudVendorService cloudVendorService,
                                 OsTypeService osTypeService,
                                 BkNetService bkNetService) {
        this.whiteIpAwareScopeHostService = whiteIpAwareScopeHostService;
        this.agentStatusService = agentStatusService;
        this.cloudVendorService = cloudVendorService;
        this.osTypeService = osTypeService;
        this.bkNetService = bkNetService;
    }

    @Override
    public List<ApplicationHostDTO> listHostDetails(String tenantId,
                                                    AppResourceScope appResourceScope,
                                                    Collection<Long> hostIds) {
        List<ApplicationHostDTO> scopeHostList = whiteIpAwareScopeHostService.getScopeHostsIncludingWhiteIPByHostId(
            appResourceScope,
            null,
            hostIds
        );
        // 填充实时agent状态
        agentStatusService.fillRealTimeAgentStatus(scopeHostList);
        fillDetailForApplicationHosts(tenantId, scopeHostList);
        return scopeHostList;
    }

    @Override
    public void fillDetailForApplicationHosts(String tenantId, List<ApplicationHostDTO> hostList) {
        fillHostsDetail(hostList, host -> {
            host.setCloudAreaName(bkNetService.getCloudAreaNameFromCache(tenantId, host.getCloudAreaId()));
            String cloudVendorId = host.getCloudVendorId();
            host.setCloudVendorName(
                cloudVendorService.getCloudVendorNameOrDefault(
                    tenantId,
                    cloudVendorId,
                    cloudVendorId == null ? null : JobConstants.UNKNOWN_NAME
                )
            );
            String osTypeId = host.getOsType();
            host.setOsTypeName(
                osTypeService.getOsTypeNameOrDefault(
                    tenantId,
                    osTypeId,
                    osTypeId == null ? null : JobConstants.UNKNOWN_NAME
                )
            );
        });
    }

    @Override
    public void fillDetailForTenantHosts(Map<String, List<ApplicationHostDTO>> tenantHostMap) {
        for (Map.Entry<String, List<ApplicationHostDTO>> entry : tenantHostMap.entrySet()) {
            fillDetailForApplicationHosts(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void fillDetailForHosts(String tenantId, List<HostDTO> hostList) {
        fillHostsDetail(hostList, host -> {
            host.setBkCloudName(bkNetService.getCloudAreaNameFromCache(tenantId, host.getBkCloudId()));
            String cloudVendorId = host.getCloudVendorId();
            host.setCloudVendorName(
                cloudVendorService.getCloudVendorNameOrDefault(
                    tenantId,
                    cloudVendorId,
                    cloudVendorId == null ? null : JobConstants.UNKNOWN_NAME
                )
            );
            String osTypeId = host.getOsType();
            host.setOsTypeName(
                osTypeService.getOsTypeNameOrDefault(
                    tenantId,
                    osTypeId,
                    osTypeId == null ? null : JobConstants.UNKNOWN_NAME
                )
            );
        });
    }

    private <T> void fillHostsDetail(Collection<T> hosts, Consumer<T> consumer) {
        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        StopWatch watch = new StopWatch("FillDetailForHosts");
        try {
            watch.start();
            for (T host : hosts) {
                consumer.accept(host);
            }
            watch.stop();
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 100) {
                log.warn("FillDetailForHosts slow, watch: {}", watch.prettyPrint());
            }
        }
    }
}
