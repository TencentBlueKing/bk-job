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

package com.tencent.bk.job.common.gse.service;

import com.tencent.bk.gse.cacheapi.AgentStatusRequestInfo;
import com.tencent.bk.gse.cacheapi.AgentStatusResponse;
import com.tencent.bk.gse.cacheapi.CacheIpInfo;
import com.tencent.bk.gse.cacheapi.CacheUser;
import com.tencent.bk.job.common.gse.config.GseConfig;
import com.tencent.bk.job.common.gse.model.AgentStatusDTO;
import com.tencent.bk.job.common.gse.sdk.GseCacheClient;
import com.tencent.bk.job.common.gse.sdk.GseCacheClientFactory;
import com.tencent.bk.job.common.util.AgentUtils;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class QueryAgentStatusClientImpl implements QueryAgentStatusClient {

    private final GseCacheClientFactory gseCacheClientFactory;
    private final GseConfig gseConfig;

    public QueryAgentStatusClientImpl(GseConfig gseConfig) {
        this.gseConfig = gseConfig;
        gseCacheClientFactory = new GseCacheClientFactory(gseConfig);
    }

    /**
     * 解析agent状态 预期解析的文本: {"businessid":"","exist":1,"ip":"1.1.1.1","region":"1","version":"NULL"}
     *
     * @return
     */
    private static int parseCacheAgentStatus(String statusStr) {
        AgentStatusDTO agentStatus = JsonUtils.fromJson(statusStr, AgentStatusDTO.class);
        return agentStatus.getExist();
    }

    @Override
    public Map<String, AgentStatus> batchGetAgentStatus(List<String> ips) {
        Long startTime = System.currentTimeMillis();
        Map<String, AgentStatus> resultMap = new HashMap<>();
        // 分批
        int batchSize = gseConfig.getQueryBatchSize();
        int threadNum = gseConfig.getQueryThreadsNum();
        int start = 0;
        int end = 0;
        int size = ips.size();
        List<List<String>> ipSubListList = new ArrayList<>();
        while (start < size) {
            end = start + batchSize;
            end = Math.min(end, size);
            List<String> ipSubList = ips.subList(start, end);
            ipSubListList.add(ipSubList);
            start += batchSize;
        }
        // 并发查询
        Collection<Map<String, AgentStatus>> maps = ConcurrencyUtil.getResultWithThreads(ipSubListList, threadNum,
            new ConcurrencyUtil.Handler<List<String>, Map<String, AgentStatus>>() {
            @Override
            public Collection<Map<String, AgentStatus>> handle(List<String> ipList1) {
                return Collections.singletonList(batchGetAgentStatusWithoutLimit(ipList1));
            }
        });
        maps.forEach(resultMap::putAll);
        Long endTime = System.currentTimeMillis();
        if (endTime - startTime > 100L) {
            log.warn("Get status of {} ips, time consuming: {}ms", resultMap.size(), (endTime - startTime));
        }
        return resultMap;
    }

//    private Map<String, AgentStatus> batchGetAgentStatusDefault(List<String> ips) {
//        Map<String, AgentStatus> resultMap = new HashMap<>();
//        if (CollectionUtils.isNotEmpty(ips)) {
//            ips.forEach(ip -> {
//                AgentStatus status = new QueryAgentStatusClient.AgentStatus();
//                String[] ipInfo = ip.split(":");
//                if (ipInfo.length != 2) {
//                    log.warn("Request ip format error! IP: {}", ip);
//                    return;
//                }
//                status.(Integer.parseInt(ipInfo[0]));
//
//            });
//        }
//
//    }



    public Map<String, AgentStatus> batchGetAgentStatusWithoutLimit(Collection<String> ips) {
        Map<String, AgentStatus> resultMap = new HashMap<>();
        GseCacheClient gseClient = gseCacheClientFactory.getClient();
        if (null == gseClient) {
            log.error("get GSE cache connection failed");
            return resultMap;
        }
        try {
            List<CacheIpInfo> ipInfoList = new ArrayList<>();
            for (String ip : ips) {
                String[] ipInfo = ip.split(":");
                if (ipInfo.length != 2) {
                    log.warn("Request ip format error! IP: {}", ip);
                    continue;
                }

                CacheIpInfo cacheIpInfo = new CacheIpInfo();
                String sourceStr = "1".equals(ipInfo[0]) ? "0" : ipInfo[0];
                cacheIpInfo.setPlatId(sourceStr);
                cacheIpInfo.setIp(ipInfo[1]);
                ipInfoList.add(cacheIpInfo);

            }
            if (ipInfoList.isEmpty()) {
                return resultMap;
            }

            CacheUser user = new CacheUser();
            user.setUser("bitmap");
            user.setPassword("bitmap");

            AgentStatusRequestInfo request = new AgentStatusRequestInfo();
            request.setUser(user);
            request.setIpinfos(ipInfoList);

            log.debug("queryAgentStatus request: " + request.toString());
            Long startTime = System.currentTimeMillis();
            AgentStatusResponse response = gseClient.getCacheClient().quireAgentStatus(request);
            Long endTime = System.currentTimeMillis();
            log.debug("queryAgentStatus response: " + response.toString());
            log.info("batchGetAgentStatus {} ips, time consuming:{}ms", ips.size(), (endTime - startTime));

            Map<String, String> responseMap = response.getResult();
            for (Map.Entry<String, String> item : responseMap.entrySet()) {
                String[] ipInfo = item.getKey().split(":");
                if (ipInfo.length != 2) {
                    log.error("query Gse agent not return businessId:ip: {}", item.getKey());
                    continue;
                }

                AgentStatus agentStatus = new AgentStatus();
                agentStatus.ip = ipInfo[1];
                agentStatus.status = parseCacheAgentStatus(item.getValue());
                agentStatus.cloudAreaId = Utils.tryParseInt(ipInfo[0]);
                resultMap.put(agentStatus.cloudAreaId + ":" + agentStatus.ip, agentStatus);
            }
            return resultMap;
        } catch (Exception e) {
            log.error("gse request failed", e);
            return resultMap;
        } finally {
            gseClient.tearDown();
        }
    }

    @Override
    public AgentStatus getAgentStatus(String ip) {
        if (!ip.contains(":")) {
            ip = "0:" + ip;
            log.warn("getAgentStatus with ip(no cloudArea), use default cloudAreaId=0");
        }
        List<String> ips = new ArrayList<>();
        ips.add(ip);
        return batchGetAgentStatus(ips).get(ip);
    }

    private String getOneAliveIP(List<String> ipList) {
        if (ipList == null || ipList.isEmpty()) {
            return null;
        }
        try {
            Map<String, AgentStatus> resultMap = batchGetAgentStatus(ipList);
            for (QueryAgentStatusClient.AgentStatus agentStatus : resultMap.values()) {
                if (AgentUtils.isAgentOkByStatus(agentStatus.status)) {
                    return agentStatus.ip;
                }
            }
        } catch (Exception e) {
            return ipList.get(0);
        }
        return ipList.get(0);
    }

    /**
     * 传入的multiIp为逗号分隔的不带云区域ID的IP
     *
     * @param multiIp
     * @param cloudAreaId
     * @return 返回的单个IP不带云区域ID
     */
    public String getHostIpByAgentStatus(String multiIp, long cloudAreaId) {
        List<String> ipList = Utils.getNotBlankSplitList(multiIp, ",");
        String hostIp = ipList.get(0);

        if (ipList.size() > 1) {
            List<String> ipListWithSource = addCloudAreaId(ipList, cloudAreaId);

            String oneAliveIP = getOneAliveIP(ipListWithSource);
            if (oneAliveIP != null) {
                if (oneAliveIP.contains(":")) {
                    return oneAliveIP.split(":")[1];
                }
                return oneAliveIP;
            }
        }
        return hostIp;
    }

    @Override
    public Pair<String, Boolean> getHostIpWithAgentStatus(String multiIp, long cloudAreaId) {
        List<String> ipList = Utils.getNotBlankSplitList(multiIp, ",");
        if (ipList.isEmpty()) {
            return null;
        }
        String hostIp = ipList.get(0);

        List<String> ipListWithSource = addCloudAreaId(ipList, cloudAreaId);
        Map<String, AgentStatus> resultMap = batchGetAgentStatus(ipListWithSource);
        for (QueryAgentStatusClient.AgentStatus agentStatus : resultMap.values()) {
            if (AgentUtils.isAgentOkByStatus(agentStatus.status)) {
                return Pair.of(agentStatus.ip, true);
            }
        }
        return Pair.of(hostIp, false);
    }

    private List<String> addCloudAreaId(List<String> ipList, long cloudAreaId) {
        List<String> newIpList = new ArrayList<>();
        for (String ip : ipList) {
            newIpList.add(cloudAreaId + ":" + ip);
        }
        return newIpList;
    }

}
