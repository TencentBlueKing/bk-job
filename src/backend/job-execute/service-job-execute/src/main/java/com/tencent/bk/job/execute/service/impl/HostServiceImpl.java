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

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.client.ServiceHostResourceClient;
import com.tencent.bk.job.execute.client.WhiteIpResourceClient;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import com.tencent.bk.job.manage.model.inner.request.ServiceCheckAppHostsReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class HostServiceImpl implements HostService {
    private final WhiteIpResourceClient whiteIpResourceClient;
    private final ServiceHostResourceClient hostResourceClient;
    private final Map<IpDTO, ServiceWhiteIPInfo> whiteIpConfig = new ConcurrentHashMap<>();
    private volatile boolean isWhiteIpConfigLoaded = false;

    @Autowired
    public HostServiceImpl(WhiteIpResourceClient whiteIpResourceClient,
                           ServiceHostResourceClient hostResourceClient) {
        this.hostResourceClient = hostResourceClient;
        this.whiteIpResourceClient = whiteIpResourceClient;
    }

    @Override
    public Map<IpDTO, ServiceHostDTO> batchGetHosts(List<IpDTO> hostIps) {
        List<ServiceHostDTO> hosts = hostResourceClient.batchGetHosts(hostIps).getData();
        Map<IpDTO, ServiceHostDTO> hostMap = new HashMap<>();
        hosts.forEach(host -> hostMap.put(new IpDTO(host.getCloudAreaId(), host.getIp()), host));
        return hostMap;
    }

    @Override
    public List<String> getHostAllowedAction(long appId, IpDTO host) {
        try {
            InternalResponse<List<String>> resp = whiteIpResourceClient.getWhiteIPActionScopes(appId, host.getIp(),
                host.getCloudAreaId());
            if (!resp.isSuccess()) {
                log.warn("Get white ip action scopes return fail resp, appId:{}, host:{}", appId,
                    host.convertToStrIp());
                return Collections.emptyList();
            }
            log.info("Get white ip action scopes, appId:{}, host:{}, resp:{}", appId, host.convertToStrIp(), resp);
            return resp.getData();
        } catch (Exception e) {
            log.warn("GetHostAllowedAction fail!", e);
            return Collections.emptyList();
        }
    }

    @Scheduled(cron = "0 * * * * ?")
    public void syncWhiteIpConfig() {
        log.info("Sync white ip config!");
        isWhiteIpConfigLoaded = true;
        long start = System.currentTimeMillis();
        InternalResponse<List<ServiceWhiteIPInfo>> resp = whiteIpResourceClient.listWhiteIPInfos();
        if (resp == null || !resp.isSuccess()) {
            log.warn("Get all white ip config return fail resp!");
            return;
        }
        log.info("Sync white ip config, resp: {}", JsonUtils.toJson(resp));

        List<ServiceWhiteIPInfo> whiteIpInfos = resp.getData();
        whiteIpConfig.clear();
        whiteIpInfos.forEach(whiteIpInfo ->
            whiteIpConfig.put(new IpDTO(whiteIpInfo.getCloudId(), whiteIpInfo.getIp()), whiteIpInfo));

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000L) {
            log.warn("Sync white ip config is slow, cost: {}", cost);
        }
        log.info("Sync white ip config success!");
    }

    @Override
    public boolean isMatchWhiteIpRule(long appId, IpDTO host, String action) {
        try {
            if (!isWhiteIpConfigLoaded) {
                syncWhiteIpConfig();
            }
            ServiceWhiteIPInfo whiteIpInfo = whiteIpConfig.get(host);
            if (whiteIpInfo == null) {
                return false;
            }
            if (whiteIpInfo.isForAllApp()) {
                return CollectionUtils.isNotEmpty(whiteIpInfo.getAllAppActionScopeList())
                    && whiteIpInfo.getAllAppActionScopeList().contains(action);
            } else {
                return whiteIpInfo.getAppIdActionScopeMap() != null
                    && whiteIpInfo.getAppIdActionScopeMap().get(appId) != null
                    && whiteIpInfo.getAppIdActionScopeMap().get(appId).contains(action);
            }
        } catch (Throwable e) {
            log.warn("Get white ip config by host and action", e);
            return false;
        }
    }

    @Override
    public List<IpDTO> checkAppHosts(Long appId, Collection<IpDTO> hostIps) {
        InternalResponse<List<IpDTO>> response =
            hostResourceClient.checkAppHosts(appId, new ServiceCheckAppHostsReq(new ArrayList<>(hostIps)));
        return response.getData();
    }
}
