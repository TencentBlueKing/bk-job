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

import com.tencent.bk.job.common.cc.config.CcConfig;
import com.tencent.bk.job.common.cc.sdk.EsbCcClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.config.EsbConfig;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.BatchUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.client.SyncResourceClient;
import com.tencent.bk.job.execute.client.WhiteIpResourceClient;
import com.tencent.bk.job.execute.model.db.CacheHostDO;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@DependsOn({"ccConfigSetter"})
@Service
@Slf4j
public class HostServiceImpl implements HostService {
    private final SyncResourceClient syncResourceClient;
    private final RedisTemplate redisTemplate;
    private final EsbCcClient ccClient;
    private final WhiteIpResourceClient whiteIpResourceClient;
    private Map<IpDTO, ServiceWhiteIPInfo> whiteIpConfig = new ConcurrentHashMap<>();
    private volatile boolean isWhiteIpConfigLoaded = false;

    @Autowired
    public HostServiceImpl(SyncResourceClient syncResourceClient,
                           @Qualifier("jsonRedisTemplate") RedisTemplate redisTemplate,
                           EsbConfig esbConfig,
                           CcConfig ccConfig,
                           WhiteIpResourceClient whiteIpResourceClient,
                           QueryAgentStatusClient queryAgentStatusClient,
                           MeterRegistry meterRegistry) {
        this.syncResourceClient = syncResourceClient;
        this.redisTemplate = redisTemplate;
        ccClient = new EsbCcClient(
            esbConfig,
            ccConfig,
            queryAgentStatusClient,
            meterRegistry);
        this.whiteIpResourceClient = whiteIpResourceClient;
    }

    @Override
    public InternalResponse<List<ServiceHostInfoDTO>> listSyncHosts(long appId) {
        StopWatch watch = new StopWatch("sync-app-hosts-" + appId);
        watch.start();
        try {
            return syncResourceClient.getHostByAppId(appId);
        } catch (Exception e) {
            log.warn("Fail to get hosts from job-manage", e);
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        } finally {
            watch.stop();
            log.info("Get sync app hosts, appId:{}, cost:{}", appId, watch.getTotalTimeMillis());
        }
    }

    @Override
    public Long getCacheHostAppId(long cloudAreaId, String ip) {
        String fullIp = cloudAreaId + ":" + ip;
        String hostKey = "job:execute:host:" + fullIp;
        try {
            Object cacheHost = redisTemplate.opsForValue().get(hostKey);
            return cacheHost == null ? null : ((CacheHostDO) cacheHost).getAppId();
        } catch (Exception e) {
            log.warn("Get host in cache exception", e);
            return null;
        }
    }

    private List<CacheHostDO> batchGetCacheHost(List<IpDTO> hosts) {
        List<String> hostKeys = hosts.stream().map(host -> "job:execute:host:" + host.convertToStrIp())
            .collect(Collectors.toList());
        try {
            return (List<CacheHostDO>) redisTemplate.opsForValue().multiGet(hostKeys);
        } catch (Exception e) {
            log.warn("Batch get host in cache exception", e);
            return Collections.emptyList();
        }
    }

    private CacheHostDO getCacheHost(long cloudAreaId, String ip) {
        String fullIp = cloudAreaId + ":" + ip;
        String hostKey = "job:execute:host:" + fullIp;
        try {
            Object cacheHost = redisTemplate.opsForValue().get(hostKey);
            return cacheHost == null ? null : (CacheHostDO) cacheHost;
        } catch (Exception e) {
            log.warn("Get host in cache exception", e);
            return null;
        }
    }

    @Override
    public ApplicationHostInfoDTO getHostPreferCache(long cloudAreaId, String ip) {
        CacheHostDO cacheHost = getCacheHost(cloudAreaId, ip);
        if (cacheHost != null) {
            ApplicationHostInfoDTO host = new ApplicationHostInfoDTO();
            host.setAppId(host.getAppId());
            host.setCloudAreaId(cloudAreaId);
            host.setIp(ip);
            host.setHostId(cacheHost.getHostId());
            return host;
        }
        return ccClient.getHostByIp(cloudAreaId, ip);
    }

    @Override
    public Map<IpDTO, ApplicationHostInfoDTO> batchGetHostsPreferCache(List<IpDTO> hosts) {
        List<CacheHostDO> cacheHosts = batchGetCacheHost(hosts);
        Map<IpDTO, ApplicationHostInfoDTO> appHosts = new HashMap<>();
        for (int i = 0; i < hosts.size(); i++) {
            IpDTO host = hosts.get(i);
            CacheHostDO cacheHost = cacheHosts.get(i);
            if (cacheHost != null) {
                ApplicationHostInfoDTO appHost = new ApplicationHostInfoDTO();
                appHost.setAppId(cacheHost.getAppId());
                appHost.setCloudAreaId(cacheHost.getCloudAreaId());
                appHost.setIp(cacheHost.getIp());
                appHost.setHostId(cacheHost.getHostId());
                appHosts.put(host, appHost);
            } else {
                ApplicationHostInfoDTO appHost = ccClient.getHostByIp(host.getCloudAreaId(), host.getIp());
                appHosts.put(host, appHost);
            }
        }
        return appHosts;
    }

    @Override
    public Pair<List<IpDTO>, List<IpDTO>> checkHostsNotInAppByCache(List<Long> appIdList, List<IpDTO> hosts) {
        long start = System.currentTimeMillis();
        List<IpDTO> notInAppHosts = new ArrayList<>();
        List<IpDTO> notInCacheHosts = new ArrayList<>();
        if (hosts.size() == 1) {
            IpDTO host = hosts.get(0);
            Long appId = getCacheHostAppId(host.getCloudAreaId(), host.getIp());
            if (appId == null) {
                notInCacheHosts.add(host);
            } else if (!appIdList.contains(appId)) {
                notInAppHosts.add(host);
            }
        } else {
            List<List<IpDTO>> hostBatches = BatchUtil.buildBatchList(hosts, 1000);
            hostBatches.forEach(batchHosts -> {
                List<CacheHostDO> cacheHosts = batchGetCacheHost(batchHosts);
                for (int i = 0; i < batchHosts.size(); i++) {
                    IpDTO host = batchHosts.get(i);
                    CacheHostDO cacheHost = cacheHosts.get(i);
                    if (cacheHost == null) {
                        notInCacheHosts.add(host);
                    } else if (!appIdList.contains(cacheHost.getAppId())) {
                        notInAppHosts.add(host);
                    }
                }
            });
        }

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000) {
            log.info("checkHostsNotInAppByCache is slow, cost: {}ms, host size : {}", cost, hosts.size());
        }
        return new ImmutablePair<>(notInAppHosts, notInCacheHosts);
    }

    public List<IpDTO> checkHostsNotInAppByCmdb(long appId, Collection<IpDTO> hosts) {
        List<IpDTO> notSyncedHosts = new ArrayList<>(hosts);
        // 未同步的主机
        try {
            List<ApplicationHostInfoDTO> newHosts = ccClient.listAppHosts(appId, hosts);
            if (newHosts != null && !newHosts.isEmpty()) {
                List<IpDTO> newIpDTOList = newHosts.stream().map(host -> new IpDTO(host.getCloudAreaId(),
                    host.getIp())).collect(Collectors.toList());
                notSyncedHosts.removeAll(newIpDTOList);
                log.info("Add new hosts to cache, appId:{}, hosts:{}", appId, newIpDTOList);
                addNewHostsToCache(newHosts);
            }
        } catch (Exception e) {
            log.warn("Handle hosts that may not be synchronized from cmdb fail!", e);
            notSyncedHosts.addAll(hosts);
        }
        return notSyncedHosts;
    }

    private void addNewHostsToCache(List<ApplicationHostInfoDTO> newHosts) {
        for (ApplicationHostInfoDTO host : newHosts) {
            CacheHostDO cacheHost = new CacheHostDO();
            cacheHost.setIp(host.getIp());
            cacheHost.setHostId(host.getHostId());
            cacheHost.setCloudAreaId(host.getCloudAreaId());
            cacheHost.setAppId(host.getAppId());
            String hostKey = buildHostKey(host.getCloudAreaId(), host.getIp());
            redisTemplate.opsForValue().set(hostKey, cacheHost, 10, TimeUnit.MINUTES);
        }
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

    private String buildHostKey(Long cloudAreaId, String ip) {
        return "job:execute:host:" + cloudAreaId + ":" + ip;
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
        whiteIpInfos.forEach(whiteIpInfo -> {
            whiteIpConfig.put(new IpDTO(whiteIpInfo.getCloudId(), whiteIpInfo.getIp()), whiteIpInfo);
        });

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
}
