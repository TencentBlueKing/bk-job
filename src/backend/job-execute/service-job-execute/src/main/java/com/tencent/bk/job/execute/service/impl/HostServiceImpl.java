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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.model.CcCloudIdDTO;
import com.tencent.bk.job.common.cc.model.CcGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.client.ServiceHostResourceClient;
import com.tencent.bk.job.execute.client.WhiteIpResourceClient;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetAppHostsReq;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetHostsReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class HostServiceImpl implements HostService {
    private final WhiteIpResourceClient whiteIpResourceClient;
    private final ServiceHostResourceClient hostResourceClient;
    private final AppScopeMappingService appScopeMappingService;
    private final AgentStateClient agentStateClient;
    private final ExecutorService getHostsByTopoExecutor;

    private final LoadingCache<Long, String> cloudAreaNameCache = CacheBuilder.newBuilder()
        .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).
            build(new CacheLoader<Long, String>() {
                      @Override
                      public String load(Long cloudAreaId) {
                          IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
                          List<CcCloudAreaInfoDTO> cloudAreaList = bizCmdbClient.getCloudAreaList();
                          if (cloudAreaList == null || cloudAreaList.isEmpty()) {
                              log.warn("Get all cloud area return empty!");
                              return "Unknown";
                          }
                          log.info("Get all cloud area, result={}", JsonUtils.toJson(cloudAreaList));
                          for (CcCloudAreaInfoDTO cloudArea : cloudAreaList) {
                              if (cloudArea.getId().equals(cloudAreaId)) {
                                  return cloudArea.getName();
                              }
                          }
                          log.info("No found cloud area for cloudAreaId:{}", cloudAreaId);
                          return "Unknown";
                      }
                  }
            );

    @Autowired
    public HostServiceImpl(WhiteIpResourceClient whiteIpResourceClient,
                           ServiceHostResourceClient hostResourceClient,
                           AppScopeMappingService appScopeMappingService,
                           AgentStateClient agentStateClient,
                           @Qualifier("getHostsByTopoExecutor") ExecutorService getHostsByTopoExecutor) {
        this.hostResourceClient = hostResourceClient;
        this.whiteIpResourceClient = whiteIpResourceClient;
        this.appScopeMappingService = appScopeMappingService;
        this.agentStateClient = agentStateClient;
        this.getHostsByTopoExecutor = getHostsByTopoExecutor;
    }

    @Override
    public Map<HostDTO, ServiceHostDTO> batchGetHosts(List<HostDTO> hostIps) {
        List<ServiceHostDTO> hosts = hostResourceClient.batchGetHosts(
            new ServiceBatchGetHostsReq(hostIps)).getData();
        Map<HostDTO, ServiceHostDTO> hostMap = new HashMap<>();
        hosts.forEach(host -> hostMap.put(new HostDTO(host.getCloudAreaId(), host.getIp()), host));
        return hostMap;
    }

    @Override
    public ServiceHostDTO getHost(HostDTO host) {
        List<ServiceHostDTO> hosts = hostResourceClient.batchGetHosts(
            new ServiceBatchGetHostsReq(Collections.singletonList(host))).getData();
        if (CollectionUtils.isEmpty(hosts)) {
            return null;
        }
        return hosts.get(0);
    }

    @Override
    public List<String> getHostAllowedAction(long appId, HostDTO host) {
        try {
            InternalResponse<List<String>> resp = whiteIpResourceClient.getWhiteIPActionScopes(appId, host.getIp(),
                host.getBkCloudId(), host.getHostId());
            if (!resp.isSuccess()) {
                log.warn("Get white ip action scopes return fail resp, appId:{}, host:{}", appId,
                    host.toCloudIp());
                return Collections.emptyList();
            }
            log.info("Get white ip action scopes, appId:{}, host:{}, resp:{}", appId, host.toCloudIp(), resp);
            return resp.getData();
        } catch (Exception e) {
            log.warn("GetHostAllowedAction fail!", e);
            return Collections.emptyList();
        }
    }

    @Override
    public ServiceListAppHostResultDTO batchGetAppHosts(Long appId,
                                                        Collection<HostDTO> hosts,
                                                        boolean refreshAgentId) {
        InternalResponse<ServiceListAppHostResultDTO> response =
            hostResourceClient.batchGetAppHosts(appId,
                new ServiceBatchGetAppHostsReq(new ArrayList<>(hosts), refreshAgentId));
        return response.getData();
    }

    private String chooseOneIpPreferAlive(Long cloudId, String multiIp) {
        List<String> agentIdList = IpUtils.buildCloudIpListByMultiIp(cloudId, multiIp);
        String cloudIp = agentStateClient.chooseOneAgentIdPreferAlive(agentIdList);
        return cloudIp.split(":")[1];
    }

    @Override
    public List<HostDTO> getHostsByDynamicGroupId(long appId, String groupId) {
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        List<CcGroupHostPropDTO> cmdbGroupHostList =
            bizCmdbClient.getDynamicGroupIp(Long.parseLong(resourceScope.getId()), groupId);
        List<HostDTO> hostList = new ArrayList<>();
        if (cmdbGroupHostList == null || cmdbGroupHostList.isEmpty()) {
            return hostList;
        }
        for (CcGroupHostPropDTO hostProp : cmdbGroupHostList) {
            List<CcCloudIdDTO> hostCloudIdList = hostProp.getCloudIdList();
            if (hostCloudIdList == null || hostCloudIdList.isEmpty()) {
                log.warn("Get ip by dynamic group id, cmdb return illegal host, skip it!appId={}, groupId={}, " +
                    "hostIp={}", appId, groupId, hostProp.getIp());
                continue;
            }
            CcCloudIdDTO hostCloudId = hostCloudIdList.get(0);
            if (hostCloudId == null) {
                log.warn("Get ip by dynamic group id, cmdb return illegal host, skip it!appId={}, groupId={}, " +
                    "hostIp={}", appId, groupId, hostProp.getIp());
                continue;
            }
            String singleIp = chooseOneIpPreferAlive(hostCloudId.getInstanceId(), hostProp.getIp());
            HostDTO host = new HostDTO(hostCloudId.getInstanceId(), singleIp);
            host.setHostId(hostProp.getId());
            host.setIpv6(hostProp.getIpv6());
            host.setAgentId(hostProp.getAgentId());
            hostList.add(host);
        }
        log.info("Get hosts by groupId, appId={}, groupId={}, hosts={}", appId, groupId, hostList);
        return hostList;
    }

    @Override
    public Map<DynamicServerGroupDTO, List<HostDTO>> batchGetAndGroupHostsByDynamicGroup(
        long appId,
        Collection<DynamicServerGroupDTO> groups) {
        if (CollectionUtils.isEmpty(groups)) {
            return new HashMap<>();
        }

        Map<DynamicServerGroupDTO, List<HostDTO>> result = new HashMap<>();
        groups.forEach(group -> result.put(group, getHostsByDynamicGroupId(appId, group.getGroupId())));
        return result;
    }

    @Override
    public List<HostDTO> getHostsByTopoNodes(long appId, List<CcInstanceDTO> ccInstances) {
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        long bizId = Long.parseLong(resourceScope.getId());
        List<ApplicationHostDTO> appHosts = bizCmdbClient.getHosts(bizId, ccInstances);
        List<HostDTO> ips = new ArrayList<>();
        if (appHosts == null || appHosts.isEmpty()) {
            return ips;
        }
        for (ApplicationHostDTO hostProp : appHosts) {
            HostDTO host = new HostDTO(hostProp.getCloudAreaId(), hostProp.getIp());
            host.setHostId(hostProp.getHostId());
            host.setIpv6(hostProp.getIpv6());
            host.setAgentId(hostProp.getAgentId());
            ips.add(host);
        }
        log.info("Get hosts by cc topo nodes, appId={}, nodes={}, hosts={}", appId, ccInstances, ips);
        return ips;
    }

    @Override
    public Map<DynamicServerTopoNodeDTO, List<HostDTO>> getAndGroupHostsByTopoNodes(
        long appId,
        Collection<DynamicServerTopoNodeDTO> topoNodes) {

        Map<DynamicServerTopoNodeDTO, List<HostDTO>> result;
        if (topoNodes.size() < 10) {
            result = new HashMap<>();
            for (DynamicServerTopoNodeDTO topoNode : topoNodes) {
                List<HostDTO> topoHosts = getHostsByTopoNodes(appId,
                    Collections.singletonList(new CcInstanceDTO(topoNode.getNodeType(), topoNode.getTopoNodeId())));
                topoNode.setIpList(topoHosts);
                result.put(topoNode, topoHosts);
            }
        } else {
            result = getTopoHostsConcurrent(appId, topoNodes);
        }
        return result;
    }

    private Map<DynamicServerTopoNodeDTO, List<HostDTO>> getTopoHostsConcurrent(
        long appId,
        Collection<DynamicServerTopoNodeDTO> topoNodes) {

        Map<DynamicServerTopoNodeDTO, List<HostDTO>> result = new HashMap<>();

        CountDownLatch latch = new CountDownLatch(topoNodes.size());
        List<Future<Pair<DynamicServerTopoNodeDTO, List<HostDTO>>>> futures = new ArrayList<>(topoNodes.size());
        for (DynamicServerTopoNodeDTO topoNode : topoNodes) {
            futures.add(getHostsByTopoExecutor.submit(new GetTopoHostTask(appId, topoNode, latch)));
        }

        try {
            for (Future<Pair<DynamicServerTopoNodeDTO, List<HostDTO>>> future : futures) {
                Pair<DynamicServerTopoNodeDTO, List<HostDTO>> topoAndHosts = future.get();
                for (DynamicServerTopoNodeDTO topoNode : topoNodes) {
                    if (topoNode.equals(topoAndHosts.getLeft())) {
                        result.put(topoNode, topoAndHosts.getRight());
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Get topo hosts concurrent error", e);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("Get topo hosts concurrent error", e);
        }
        return result;
    }

    @Override
    public String getCloudAreaName(long cloudAreaId) {
        try {
            return cloudAreaNameCache.get(cloudAreaId);
        } catch (Exception e) {
            log.warn("Fail to get cloud area name", e);
            return "Unknown";
        }
    }

    private class GetTopoHostTask implements Callable<Pair<DynamicServerTopoNodeDTO, List<HostDTO>>> {
        private final long appId;
        private final DynamicServerTopoNodeDTO topoNode;
        private final CountDownLatch latch;

        private GetTopoHostTask(long appId, DynamicServerTopoNodeDTO topoNode, CountDownLatch latch) {
            this.appId = appId;
            this.topoNode = topoNode;
            this.latch = latch;
        }

        @Override
        public Pair<DynamicServerTopoNodeDTO, List<HostDTO>> call() {
            try {
                List<HostDTO> topoHosts = getHostsByTopoNodes(appId,
                    Collections.singletonList(new CcInstanceDTO(topoNode.getNodeType(), topoNode.getTopoNodeId())));
                return new ImmutablePair<>(topoNode, topoHosts);
            } catch (Throwable e) {
                log.warn("Get hosts by topo fail", e);
                return new ImmutablePair<>(topoNode, Collections.emptyList());
            } finally {
                latch.countDown();
            }
        }
    }
}
