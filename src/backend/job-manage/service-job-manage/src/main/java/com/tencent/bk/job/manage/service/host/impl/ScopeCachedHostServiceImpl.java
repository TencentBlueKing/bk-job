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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import com.tencent.bk.job.manage.service.host.ScopeCachedHostService;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScopeCachedHostServiceImpl implements ScopeCachedHostService {

    private final ApplicationService applicationService;
    private final TenantHostService tenantHostService;
    private final NoTenantHostService noTenantHostService;
    private final HostDetailService hostDetailService;

    @Autowired
    public ScopeCachedHostServiceImpl(ApplicationService applicationService,
                                      TenantHostService tenantHostService,
                                      NoTenantHostService noTenantHostService,
                                      HostDetailService hostDetailService) {
        this.applicationService = applicationService;
        this.tenantHostService = tenantHostService;
        this.noTenantHostService = noTenantHostService;
        this.hostDetailService = hostDetailService;
    }

    @Override
    public ServiceListAppHostResultDTO listAppHostsPreferCache(AppResourceScope appResourceScope,
                                                               List<HostDTO> hosts,
                                                               boolean refreshAgentId) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (!applicationDTO.isTenantSet()) {
            // 单租户
            ServiceListAppHostResultDTO result = tenantHostService.listAppHostsPreferCache(
                appResourceScope.getAppId(),
                hosts,
                refreshAgentId
            );
            if (CollectionUtils.isNotEmpty(result.getValidHosts())) {
                hostDetailService.fillDetailForHosts(applicationDTO.getTenantId(), result.getValidHosts());
            }
            return result;
        } else if (applicationDTO.isAllTenantSet()) {
            // 全租户
            StopWatch watch = new StopWatch("listHostsFromCacheOrDB");
            try {
                return listHostsFromCacheOrDB(appResourceScope, hosts, watch);
            } finally {
                if (watch.isRunning()) {
                    watch.stop();
                }
                if (watch.getTotalTimeMillis() > 3000) {
                    log.warn("listHostsFromCacheOrDB slow, watch: {}", watch.prettyPrint());
                }
            }
        } else {
            // 租户集：暂未支持
            throw new NotImplementedException(ErrorCode.NOT_SUPPORT_FEATURE);
        }
    }

    private ServiceListAppHostResultDTO listHostsFromCacheOrDB(AppResourceScope appResourceScope,
                                                               List<HostDTO> hosts,
                                                               StopWatch watch) {
        ServiceListAppHostResultDTO resultDTO = new ServiceListAppHostResultDTO();

        watch.start("listHostsFromCacheOrDB");
        List<ApplicationHostDTO> hostDTOList = noTenantHostService.listHostsFromCacheOrDB(hosts);
        watch.stop();

        if (CollectionUtils.isNotEmpty(hostDTOList)) {
            watch.start("fillDetailForTenantHosts");
            Map<String, List<ApplicationHostDTO>> tenantHostMap = hostDTOList.stream()
                .collect(Collectors.groupingBy(ApplicationHostDTO::getTenantId));
            hostDetailService.fillDetailForTenantHosts(tenantHostMap);
            watch.stop();
        }

        watch.start("collectNotExistHosts");
        List<HostDTO> validHosts = new ArrayList<>();
        List<HostDTO> notExistHosts = new ArrayList<>();
        Set<Long> existHostIdSet = new HashSet<>();
        Set<String> existCloudIpSet = new HashSet<>();
        for (ApplicationHostDTO host : hostDTOList) {
            validHosts.add(host.toHostDTO());
            existHostIdSet.add(host.getHostId());
            existCloudIpSet.add(host.getCloudIp());
        }
        for (HostDTO inputHost : hosts) {
            Long hostId = inputHost.getHostId();
            if (hostId != null) {
                if (!existHostIdSet.contains(hostId)) {
                    notExistHosts.add(inputHost);
                }
            } else {
                String cloudIp = inputHost.getCloudIp();
                if (!existCloudIpSet.contains(cloudIp)) {
                    notExistHosts.add(inputHost);
                }
            }
        }
        watch.stop();

        if (CollectionUtils.isNotEmpty(notExistHosts)) {
            log.info(
                "Contains invalid hosts, app: {}, notExistHosts: {}",
                appResourceScope.toString(),
                notExistHosts
            );
        }

        resultDTO.setValidHosts(validHosts);
        resultDTO.setNotExistHosts(notExistHosts);
        return resultDTO;
    }
}
