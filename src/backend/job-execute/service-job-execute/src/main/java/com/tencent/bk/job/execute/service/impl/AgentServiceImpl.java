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

import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.execute.engine.consts.Consts;
import com.tencent.bk.job.execute.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@Service
@Slf4j
public class AgentServiceImpl implements AgentService {
    private final QueryAgentStatusClient queryAgentStatusClient;
    private volatile String agentBindIp;

    @Autowired
    public AgentServiceImpl(QueryAgentStatusClient queryAgentStatusClient) {
        this.queryAgentStatusClient = queryAgentStatusClient;
    }

    @Override
    public String getLocalAgentBindIp() {
        if (StringUtils.isNotBlank(agentBindIp)) {
            return agentBindIp;
        }
        return getAgentBindIp();
    }

    private String getAgentBindIp() {
        log.info("Get local agent bind ip!");
        synchronized (this) {
            if (StringUtils.isNotBlank(agentBindIp)) {
                return agentBindIp;
            }
            Map<String, String> networkIps = getMachineIP();
            StringJoiner sj = new StringJoiner(",");
            for (String ip : networkIps.values()) {
                sj.add(ip);
            }
            String multiIp = sj.toString();
            agentBindIp = queryAgentStatusClient.getHostIpByAgentStatus(multiIp, Consts.DEFAULT_CLOUD_ID);
            log.info("Local agent bind ip is {}", agentBindIp);
            return agentBindIp;
        }
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
                    if (StringUtils.isBlank(netInterfaceName) || "lo".equalsIgnoreCase(netInterfaceName)) {// 过滤掉127
                        // .0.0.1的IP
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
