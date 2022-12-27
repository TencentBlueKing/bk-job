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
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.engine.consts.Consts;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AgentServiceImpl implements AgentService {
    private final AgentStateClient agentStateClient;
    private final HostService hostService;
    private final LoadingCache<String, HostDTO> agentHostCache = CacheBuilder.newBuilder()
        .maximumSize(1).expireAfterWrite(60, TimeUnit.SECONDS).
            build(new CacheLoader<String, HostDTO>() {
                      @SuppressWarnings("all")
                      @Override
                      public HostDTO load(String key) {
                          HostDTO agentHost = getAgentBindHost();
                          log.debug("load agentHost and save to cache:{}", agentHost);
                          return agentHost;
                      }
                  }
            );

    @Autowired
    public AgentServiceImpl(AgentStateClient agentStateClient,
                            HostService hostService) {
        this.agentStateClient = agentStateClient;
        this.hostService = hostService;
    }

    @Override
    public HostDTO getLocalAgentHost() {
        try {
            String CACHE_KEY_AGENT_HOST = "agentHost";
            return agentHostCache.get(CACHE_KEY_AGENT_HOST);
        } catch (ExecutionException e) {
            log.warn("Fail to load agentHost from cache, try to load directly", e);
            return getAgentBindHost();
        }
    }

    @Override
    public ServersDTO getLocalServersDTO() {
        List<HostDTO> hostDTOList = new ArrayList<>();
        hostDTOList.add(getLocalAgentHost());
        ServersDTO servers = new ServersDTO();
        servers.setStaticIpList(hostDTOList);
        servers.setIpList(hostDTOList);
        return servers;
    }

    private HostDTO getAgentBindHost() {
        log.info("Get local agent bind host!");
        synchronized (this) {
            String physicalMachineMultiIp;
            String nodeIP = System.getenv("BK_JOB_NODE_IP");
            if (StringUtils.isNotBlank(nodeIP)) {
                log.info("working in container, get nodeIP:{}", nodeIP);
                physicalMachineMultiIp = nodeIP;
            } else {
                Map<String, String> networkIps = getMachineIP();
                StringJoiner sj = new StringJoiner(",");
                for (String ip : networkIps.values()) {
                    sj.add(ip);
                }
                physicalMachineMultiIp = sj.toString();
            }
            ServiceHostDTO host;
            if (physicalMachineMultiIp.contains(":")) {
                // IPv6地址
                // 首先转为完整无压缩格式
                physicalMachineMultiIp = IpUtils.getFullIpv6ByCompressedOne(physicalMachineMultiIp);
                host = getServiceHostByMultiIpv6(physicalMachineMultiIp);
            } else {
                // IPv4地址
                host = getServiceHostByMultiIpv4(physicalMachineMultiIp);
            }
            if (host == null) {
                log.error("Invalid host for ip: {}", physicalMachineMultiIp);
                return null;
            }
            return toHostDTO(host);
        }
    }

    private ServiceHostDTO getServiceHostByMultiIpv4(String multiIpv4) {
        List<HostDTO> hostIps = buildHostIps((long) Consts.DEFAULT_CLOUD_ID, multiIpv4);
        if (CollectionUtils.isEmpty(hostIps)) {
            log.warn("Cannot find host by multiIpv4:{}", multiIpv4);
            return null;
        }
        Map<HostDTO, ServiceHostDTO> map = hostService.batchGetHosts(hostIps);
        ServiceHostDTO aliveHost = findOneAliveHost(map.values());
        if (aliveHost == null) {
            log.warn("Cannot find alive hosts, use first ip of {}", multiIpv4);
            return map.get(hostIps.get(0));
        }
        return aliveHost;
    }

    private List<HostDTO> buildHostIps(Long cloudAreaId, String multiIpv4) {
        if (StringUtils.isBlank(multiIpv4)) {
            return Collections.emptyList();
        }
        List<HostDTO> hostIps = new ArrayList<>();
        String[] ipArr = multiIpv4.trim().split("[,;]");
        for (String ip : ipArr) {
            hostIps.add(new HostDTO(cloudAreaId, ip));
        }
        return hostIps;
    }

    private ServiceHostDTO getServiceHostByMultiIpv6(String multiIpv6) {
        if (StringUtils.isBlank(multiIpv6)) {
            return null;
        }
        String[] ipv6Arr = multiIpv6.trim().split("[,;]");
        List<ServiceHostDTO> hosts = new ArrayList<>();
        for (String ipv6 : ipv6Arr) {
            hosts.add(hostService.getHostByCloudIpv6(Consts.DEFAULT_CLOUD_ID, ipv6));
        }
        if (CollectionUtils.isEmpty(hosts)) {
            log.warn("Cannot find host by multiIpv6:{}", multiIpv6);
            return null;
        }
        ServiceHostDTO aliveHost = findOneAliveHost(hosts);
        if (aliveHost == null) {
            log.warn("Cannot find alive hosts, use first ip of {}", multiIpv6);
            return hosts.get(0);
        }
        return aliveHost;
    }

    /**
     * 从多个主机中找出Agent存活的一个
     *
     * @param serviceHosts 主机列表
     * @return Agent存活的第一个主机或者Null
     */
    private ServiceHostDTO findOneAliveHost(Collection<ServiceHostDTO> serviceHosts) {
        Map<String, ServiceHostDTO> agentIdToHostMap = new HashMap<>();

        serviceHosts.forEach(serviceHost -> agentIdToHostMap.put(
            StringUtils.isNotEmpty(serviceHost.getAgentId()) ? serviceHost.getAgentId()
                : serviceHost.getCloudIp(),
            serviceHost)
        );
        Map<String, Boolean> agentStatusMap =
            agentStateClient.batchGetAgentAliveStatus(new ArrayList<>(agentIdToHostMap.keySet()));

        List<ServiceHostDTO> aliveHosts = new ArrayList<>();
        agentStatusMap.forEach((agentId, status) -> {
            if (status != null && status) {
                aliveHosts.add(agentIdToHostMap.get(agentId));
            }
        });
        if (CollectionUtils.isEmpty(aliveHosts)) {
            return null;
        } else if (aliveHosts.size() > 1) {
            ServiceHostDTO choosedHost = aliveHosts.get(0);
            log.warn("{} aliveHosts found, use {}", aliveHosts.size(), choosedHost);
            return choosedHost;
        }
        return aliveHosts.get(0);
    }

    private HostDTO toHostDTO(ServiceHostDTO host) {
        HostDTO result = new HostDTO();
        result.setHostId(host.getHostId());
        result.setBkCloudId(host.getCloudAreaId());
        result.setIp(host.getIp());
        result.setIpv6(host.getIpv6());
        result.setAgentId(host.getAgentId());
        return result;
    }

    private Map<String, String> getMachineIP() {
        Map<String, String> allIp = new HashMap<>();
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();// 获取服务器的所有网卡
            if (null == allNetInterfaces) {
                log.error("Can not get any ip!!!");
            } else {
                while (allNetInterfaces.hasMoreElements()) {// 循环网卡获取网卡的IP地址
                    NetworkInterface netInterface = allNetInterfaces.nextElement();
                    String netInterfaceName = netInterface.getName();
                    if (StringUtils.isBlank(netInterfaceName) || "lo".equalsIgnoreCase(netInterfaceName)) {
                        // 过滤掉127.0.0.1的IP
                        continue;
                    }
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress ip = addresses.nextElement();
                        if (ip instanceof Inet4Address && !ip.isLoopbackAddress()) {
                            String machineIp = ip.getHostAddress();
                            log.info("NetInterfaceName:{}, ip={}", netInterfaceName, machineIp);
                            allIp.put(netInterfaceName, machineIp);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Get network error", e);
        }
        return allIp;
    }
}
