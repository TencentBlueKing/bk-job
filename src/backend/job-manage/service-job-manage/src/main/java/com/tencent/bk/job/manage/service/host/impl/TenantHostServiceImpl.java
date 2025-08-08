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

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.dao.TenantHostDAO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import com.tencent.bk.job.manage.service.host.strategy.TenantListHostStrategy;
import com.tencent.bk.job.manage.service.host.strategy.TenantListHostStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 指定租户的主机服务
 */
@SuppressWarnings("SameParameterValue")
@Slf4j
@Service
public class TenantHostServiceImpl extends BaseHostService implements TenantHostService {

    private final TenantHostDAO tenantHostDAO;
    private final ApplicationService applicationService;
    private final IBizCmdbClient bizCmdbClient;
    private final TenantListHostStrategyService tenantListHostStrategyService;

    @Autowired
    public TenantHostServiceImpl(TenantHostDAO tenantHostDAO,
                                 ApplicationService applicationService,
                                 IBizCmdbClient bizCmdbClient,
                                 TenantListHostStrategyService tenantListHostStrategyService) {
        super(tenantListHostStrategyService);
        this.tenantHostDAO = tenantHostDAO;
        this.applicationService = applicationService;
        this.bizCmdbClient = bizCmdbClient;
        this.tenantListHostStrategyService = tenantListHostStrategyService;
    }

    private List<Long> buildIncludeBizIdList(ApplicationDTO application) {
        List<Long> bizIdList = new ArrayList<>();
        if (application.isBiz()) {
            bizIdList.add(application.getBizIdIfBizApp());
        } else if (application.isBizSet()) {
            if (application.getSubBizIds() != null) {
                bizIdList.addAll(application.getSubBizIds());
            }
        }
        return bizIdList;
    }

    @Override
    public List<ApplicationHostDTO> listHosts(String tenantId, Collection<HostDTO> hosts) {
        Pair<List<HostDTO>, List<ApplicationHostDTO>> hostResult = listHostsFromCacheOrCmdb(tenantId, hosts);
        if (CollectionUtils.isEmpty(hostResult.getRight())) {
            return Collections.emptyList();
        }
        return hostResult.getRight();
    }

    @Override
    public List<ApplicationHostDTO> listHostsByCloudIpv6(String tenantId, Long cloudAreaId, String ipv6) {
        List<ApplicationHostDTO> hosts = tenantHostDAO.listHostInfoByCloudIpv6(tenantId, cloudAreaId, ipv6);
        // 多个Ipv6中的具体一个与查询条件Ipv6相等才有效
        hosts = filterHostsByCloudIpv6(hosts, cloudAreaId, ipv6);
        if (CollectionUtils.isEmpty(hosts)) {
            hosts = bizCmdbClient.listHostsByCloudIpv6s(
                tenantId,
                Collections.singletonList(cloudAreaId + IpUtils.COLON + ipv6)
            );
            hosts = filterHostsByCloudIpv6(hosts, cloudAreaId, ipv6);
        }
        return hosts;
    }

    /**
     * 筛选出与指定云区域ID、Ipv6地址匹配的主机
     * 指定的Ipv6地址必须与主机多个Ipv6地址的其中一个精确匹配
     *
     * @param hosts       筛选前的主机列表
     * @param cloudAreaId 云区域ID
     * @param ipv6        Ipv6地址
     * @return 筛选后的主机列表
     */
    private List<ApplicationHostDTO> filterHostsByCloudIpv6(List<ApplicationHostDTO> hosts,
                                                            Long cloudAreaId,
                                                            String ipv6) {
        return hosts.stream().filter(host -> {
            String multiIpv6 = host.getIpv6();
            Set<String> ipv6s = Arrays.stream(multiIpv6.split("[,;]"))
                .map(String::trim)
                .collect(Collectors.toSet());
            return host.getCloudAreaId().equals(cloudAreaId) && ipv6s.contains(ipv6);
        }).collect(Collectors.toList());
    }

    @Override
    public ServiceListAppHostResultDTO listAppHostsPreferCache(Long appId,
                                                               List<HostDTO> hosts,
                                                               boolean refreshAgentId) {
        StopWatch watch = new StopWatch("listAppHostsPreferCache");
        try {
            ServiceListAppHostResultDTO result = new ServiceListAppHostResultDTO();

            watch.start("getAppByAppId");
            ApplicationDTO application = applicationService.getAppByAppId(appId);
            watch.stop();

            String tenantId = application.getTenantId();

            watch.start("listHostsFromCacheOrCmdb");
            Pair<List<HostDTO>, List<ApplicationHostDTO>> hostResult = listHostsFromCacheOrCmdb(tenantId, hosts);
            watch.stop();

            List<HostDTO> notExistHosts = hostResult.getLeft();
            List<ApplicationHostDTO> existHosts = hostResult.getRight();

            watch.start("refreshHostAgentIdIfNeed");
            refreshHostAgentIdIfNeed(tenantId, refreshAgentId, existHosts);
            watch.stop();

            List<HostDTO> validHosts = new ArrayList<>();
            List<HostDTO> notInAppHosts = new ArrayList<>();

            if (application.isAllBizSet()) {
                // 如果是全业务，所以主机都是合法的
                result.setNotExistHosts(notExistHosts);
                result.setValidHosts(existHosts.stream().map(ApplicationHostDTO::toHostDTO)
                    .collect(Collectors.toList()));
                result.setNotInAppHosts(Collections.emptyList());
                return result;
            }

            // 普通业务集和普通业务需要判断主机是否归属于业务
            List<Long> includeBizIds = buildIncludeBizIdList(application);
            if (CollectionUtils.isEmpty(includeBizIds)) {
                log.warn("App do not contains any biz, appId:{}", appId);
                result.setValidHosts(Collections.emptyList());
                result.setNotExistHosts(Collections.emptyList());
                result.setNotInAppHosts(hosts);
                return result;
            }

            existHosts.forEach(existHost -> {
                if (includeBizIds.contains(existHost.getBizId())) {
                    validHosts.add(existHost.toHostDTO());
                } else {
                    notInAppHosts.add(existHost.toHostDTO());
                }
            });

            // 对于判定为其他业务下的主机，可能是缓存数据不准确导致，需要根据CMDB实时数据进行二次判定
            if (CollectionUtils.isNotEmpty(notInAppHosts)) {
                watch.start("reConfirmNotInAppHostsByCmdb");
                reConfirmNotInAppHostsByCmdb(
                    notInAppHosts,
                    notExistHosts,
                    validHosts,
                    tenantId,
                    appId,
                    includeBizIds
                );
                watch.stop();
            }

            if (CollectionUtils.isNotEmpty(notExistHosts) || CollectionUtils.isNotEmpty(notInAppHosts)) {
                log.info("Contains invalid hosts, appId: {}, notExistHosts: {}, hostsInOtherApp: {}",
                    appId, notExistHosts, notInAppHosts);
            }

            result.setNotExistHosts(notExistHosts);
            result.setValidHosts(validHosts);
            result.setNotInAppHosts(notInAppHosts);

            return result;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 3000) {
                log.warn("ListAppHostsPreferCache slow, watch: {}", watch.prettyPrint());
            }
        }

    }

    /**
     * 对于判定为其他业务下的主机，可能是缓存数据不准确导致，根据CMDB实时数据进行二次判定
     *
     * @param notInAppHosts 前期判定为在其他业务下的主机，在该方法中数据可能被修改
     * @param notExistHosts 前期判定为不存在的主机，在该方法中数据可能被修改
     * @param validHosts    前期判定为在业务下的主机，在该方法中数据可能被修改
     * @param tenantId      租户ID
     * @param appId         Job内业务ID
     * @param includeBizIds Job内业务ID可能对应的多个CMDB业务ID列表
     */
    private void reConfirmNotInAppHostsByCmdb(List<HostDTO> notInAppHosts,
                                              List<HostDTO> notExistHosts,
                                              List<HostDTO> validHosts,
                                              String tenantId,
                                              Long appId,
                                              List<Long> includeBizIds) {
        Pair<List<HostDTO>, List<ApplicationHostDTO>> cmdbHostsPair = listHostsFromCmdb(tenantId, notInAppHosts);
        if (CollectionUtils.isNotEmpty(cmdbHostsPair.getLeft())) {
            notExistHosts.addAll(cmdbHostsPair.getLeft());
        }
        List<ApplicationHostDTO> cmdbExistHosts = cmdbHostsPair.getRight();
        if (CollectionUtils.isNotEmpty(cmdbExistHosts)) {
            notInAppHosts.clear();
            List<ApplicationHostDTO> cmdbValidHosts = new ArrayList<>();
            cmdbExistHosts.forEach(existHost -> {
                if (includeBizIds.contains(existHost.getBizId())) {
                    validHosts.add(existHost.toHostDTO());
                    cmdbValidHosts.add(existHost);
                } else {
                    notInAppHosts.add(existHost.toHostDTO());
                }
            });
            if (!cmdbValidHosts.isEmpty()) {
                log.info(
                    "{} hosts belong to appId {} after check in cmdb, cmdbValidHosts={}",
                    cmdbValidHosts.size(),
                    appId,
                    cmdbValidHosts
                );
            }
        }
    }

    private void refreshHostAgentIdIfNeed(String tenantId, boolean needRefreshAgentId, List<ApplicationHostDTO> hosts) {
        // 如果Job缓存的主机中没有agentId，那么需要从cmdb实时获取（解决一些特殊场景，比如节点管理Agent插件安装，bk_agent_id更新事件还没被处理的场景
        if (!needRefreshAgentId || CollectionUtils.isEmpty(hosts)) {
            return;
        }
        long start = System.currentTimeMillis();
        List<Long> missingAgentIdHostIds = hosts.stream()
            .filter(host -> StringUtils.isEmpty(host.getAgentId()))
            .map(ApplicationHostDTO::getHostId)
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(missingAgentIdHostIds)) {
            return;
        }
        List<ApplicationHostDTO> cmdbHosts = bizCmdbClient.listHostsByHostIds(tenantId, missingAgentIdHostIds);
        if (CollectionUtils.isEmpty(cmdbHosts)) {
            log.warn("Refresh host agent id, hosts are not exist in cmdb! hosts: {}", missingAgentIdHostIds);
            return;
        }

        Map<Long, String> hostIdAndAgentIdMap = cmdbHosts.stream()
            .filter(host -> StringUtils.isNotEmpty(host.getAgentId()))
            .collect(Collectors.toMap(ApplicationHostDTO::getHostId,
                ApplicationHostDTO::getAgentId, (oldValue, newValue) -> newValue));
        hosts.forEach(host -> {
            if (StringUtils.isEmpty(host.getAgentId())) {
                host.setAgentId(hostIdAndAgentIdMap.get(host.getHostId()));
            }
        });
        log.info("Refresh host agent id, hostIds: {}, hostIdAndAgentIdMap: {}, cost: {}",
            missingAgentIdHostIds, hostIdAndAgentIdMap, System.currentTimeMillis() - start);
    }

    @SuppressWarnings("DuplicatedCode")
    private Pair<List<HostDTO>, List<ApplicationHostDTO>> listHostsFromCmdb(String tenantId,
                                                                            Collection<HostDTO> hosts) {
        List<HostDTO> notExistHosts = new ArrayList<>();
        List<ApplicationHostDTO> appHosts = new ArrayList<>();
        Pair<List<Long>, List<String>> pair = separateByHostIdOrCloudIp(hosts);
        List<Long> hostIds = pair.getLeft();
        List<String> cloudIps = pair.getRight();
        if (CollectionUtils.isNotEmpty(hostIds)) {
            TenantListHostStrategy<Long> strategy = tenantListHostStrategyService.buildListHostByIdsFromCmdbStrategy();
            Pair<List<Long>, List<ApplicationHostDTO>> result = strategy.listHosts(tenantId, hostIds);
            appHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistHostId -> notExistHosts.add(HostDTO.fromHostId(notExistHostId)));
            }
        }
        if (CollectionUtils.isNotEmpty(cloudIps)) {
            TenantListHostStrategy<String> strategy =
                tenantListHostStrategyService.buildListHostByIpsFromCmdbStrategy();
            Pair<List<String>, List<ApplicationHostDTO>> result = strategy.listHosts(tenantId, cloudIps);
            appHosts.addAll(result.getRight());
            if (CollectionUtils.isNotEmpty(result.getLeft())) {
                result.getLeft().forEach(notExistCloudIp -> notExistHosts.add(HostDTO.fromCloudIp(notExistCloudIp)));
            }
        }
        return Pair.of(notExistHosts, appHosts);
    }

    @Override
    public Map<String, ApplicationHostDTO> listHostsByIps(String tenantId, Collection<String> cloudIps) {
        TenantListHostStrategy<String> strategy = tenantListHostStrategyService.buildByIpsFromCacheOrDbOrCmdbStrategy();
        Pair<List<String>, List<ApplicationHostDTO>> result = strategy.listHosts(
            tenantId,
            new ArrayList<>(cloudIps)
        );
        return result.getRight().stream().collect(
            Collectors.toMap(ApplicationHostDTO::getCloudIp, host -> host, (oldValue, newValue) -> newValue));
    }
}
