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

import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.manage.api.common.constants.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.model.web.request.HostCheckReq;
import com.tencent.bk.job.manage.service.WhiteIPService;
import com.tencent.bk.job.manage.service.host.CurrentTenantHostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.host.WhiteIpAwareScopeHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资源范围下的主机服务
 */
@Slf4j
@Service
public class WhiteIpAwareScopeHostServiceImpl implements WhiteIpAwareScopeHostService {

    private final WhiteIPService whiteIPService;
    private final CurrentTenantHostService currentTenantHostService;
    private final ScopeHostService scopeHostService;

    @Autowired
    public WhiteIpAwareScopeHostServiceImpl(WhiteIPService whiteIPService,
                                            CurrentTenantHostService currentTenantHostService,
                                            ScopeHostService scopeHostService) {
        this.whiteIPService = whiteIPService;
        this.currentTenantHostService = currentTenantHostService;
        this.scopeHostService = scopeHostService;
    }

    /**
     * 记录检索过程中各阶段未找到的数据，便于排查问题
     *
     * @param hostIdSet                 用于查询的原始hostId集合
     * @param whiteIpHostDTOList        从白名单数据找到的机器信息
     * @param whiteIpHostWithDetailList 查询机器详情得到的机器信息
     */
    private void recordNotFoundWhiteIP(Set<Long> hostIdSet,
                                       List<HostDTO> whiteIpHostDTOList,
                                       List<ApplicationHostDTO> whiteIpHostWithDetailList) {
        int whiteIpNum = whiteIpHostDTOList.size();
        log.info("{} white ips retrieved:{}", whiteIpNum, whiteIpHostDTOList);
        List<Long> whiteIpHostIds =
            whiteIpHostDTOList.stream().map(HostDTO::getHostId).collect(Collectors.toList());
        if (whiteIpNum < hostIdSet.size()) {
            hostIdSet.removeAll(whiteIpHostIds);
            log.info("no white ip hosts found by hostIds:{}", hostIdSet);
        }
        int whiteIpDetailNum = whiteIpHostWithDetailList.size();
        if (whiteIpDetailNum < whiteIpNum) {
            Set<Long> whiteIpHostDetailIds =
                whiteIpHostWithDetailList.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toSet());
            List<HostDTO> hostListCopy = new ArrayList<>(whiteIpHostDTOList);
            hostListCopy.removeIf(hostDTO -> whiteIpHostDetailIds.contains(hostDTO.getHostId()));
            log.warn("cannot find host detail of {} white ips:{}", hostListCopy.size(), hostListCopy);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByHostId(AppResourceScope appResourceScope,
                                                                          ActionScopeEnum actionScope,
                                                                          Collection<Long> hostIds) {
        Set<Long> hostIdSet = new HashSet<>(hostIds);
        List<ApplicationHostDTO> scopeHostList = scopeHostService.getScopeHostsByIds(appResourceScope, hostIdSet);
        List<ApplicationHostDTO> finalHostList = new ArrayList<>(scopeHostList);
        Set<Long> scopeHostIdSet = scopeHostList.stream()
            .map(ApplicationHostDTO::getHostId)
            .collect(Collectors.toSet());
        hostIdSet.removeAll(scopeHostIdSet);
        if (CollectionUtils.isEmpty(hostIdSet)) {
            return finalHostList;
        }
        List<HostDTO> whiteIpHostDTOList = whiteIPService.listAvailableWhiteIPHost(
            appResourceScope.getAppId(),
            actionScope,
            hostIdSet
        );
        List<ApplicationHostDTO> whiteIpHostList = currentTenantHostService.listHosts(whiteIpHostDTOList);
        log.info("{} white ips added", whiteIpHostList.size());
        if (CollectionUtils.isNotEmpty(whiteIpHostList)) {
            finalHostList.addAll(whiteIpHostList);
        }
        recordNotFoundWhiteIP(hostIdSet, whiteIpHostDTOList, whiteIpHostList);
        return finalHostList;
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByIp(AppResourceScope appResourceScope,
                                                                      ActionScopeEnum actionScope,
                                                                      Collection<String> ips) {
        List<ApplicationHostDTO> scopeHostList = scopeHostService.getScopeHostsByIps(appResourceScope, ips);
        List<ApplicationHostDTO> finalHostList = new ArrayList<>(scopeHostList);
        List<HostDTO> whiteIpHostDTOList = whiteIPService.listAvailableWhiteIPHostByIps(
            appResourceScope.getAppId(),
            actionScope,
            ips
        );
        List<ApplicationHostDTO> whiteIpHostList = currentTenantHostService.listHosts(whiteIpHostDTOList);
        log.info("{} white ips added", whiteIpHostList.size());
        if (CollectionUtils.isNotEmpty(whiteIpHostList)) {
            finalHostList.addAll(whiteIpHostList);
        }
        return finalHostList;
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByCloudIp(AppResourceScope appResourceScope,
                                                                           ActionScopeEnum actionScope,
                                                                           Collection<String> cloudIps) {
        List<ApplicationHostDTO> scopeHostList = scopeHostService.getScopeHostsByCloudIps(appResourceScope, cloudIps);
        List<ApplicationHostDTO> finalHostList = new ArrayList<>(scopeHostList);
        Map<String, ApplicationHostDTO> map = currentTenantHostService.listHostsByIps(cloudIps);
        Set<Long> hostIds = map.values().stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toSet());
        List<ApplicationHostDTO> whiteIpHostList = listWhiteIpHostsByIds(appResourceScope, actionScope, hostIds);
        log.info("{} white ips added", whiteIpHostList.size());
        if (CollectionUtils.isNotEmpty(whiteIpHostList)) {
            finalHostList.addAll(whiteIpHostList);
        }
        return finalHostList;
    }

    private List<ApplicationHostDTO> listWhiteIpHostsByIds(AppResourceScope appResourceScope,
                                                           ActionScopeEnum actionScope,
                                                           Collection<Long> hostIds) {
        List<HostDTO> whiteIpHostDTOList = whiteIPService.listAvailableWhiteIPHost(
            appResourceScope.getAppId(),
            actionScope,
            hostIds
        );
        return currentTenantHostService.listHosts(whiteIpHostDTOList);
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByIpv6(AppResourceScope appResourceScope,
                                                                        ActionScopeEnum actionScope,
                                                                        Collection<String> ipv6s) {
        List<ApplicationHostDTO> scopeHostList = scopeHostService.getScopeHostsByIpv6s(appResourceScope, ipv6s);
        List<ApplicationHostDTO> finalHostList = new ArrayList<>(scopeHostList);
        List<HostDTO> whiteIpHostDTOList = whiteIPService.listAvailableWhiteIPHostByIpv6s(
            appResourceScope.getAppId(),
            actionScope,
            ipv6s
        );
        List<ApplicationHostDTO> whiteIpHostList = currentTenantHostService.listHosts(whiteIpHostDTOList);
        log.info("{} white ips added", whiteIpHostList.size());
        if (CollectionUtils.isNotEmpty(whiteIpHostList)) {
            finalHostList.addAll(whiteIpHostList);
        }
        return finalHostList;
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByKey(AppResourceScope appResourceScope,
                                                                       ActionScopeEnum actionScope,
                                                                       Collection<String> keys) {
        // 1.数字优先解析为hostId进行匹配
        Pair<List<Long>, List<String>> hostIdValuePair = StringUtil.extractValueFromStrings(keys, Long.class);
        List<Long> hostIdList = hostIdValuePair.getLeft();
        List<String> remainKeys = hostIdValuePair.getRight();
        List<ApplicationHostDTO> finalHostList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(hostIdList)) {
            if (log.isDebugEnabled()) {
                log.debug("hostIdList={}, remainKeys={}", hostIdList, remainKeys);
            }
            finalHostList.addAll(scopeHostService.getScopeHostsByIds(appResourceScope, hostIdList));
        }
        // 2.剩余关键字通过主机名称匹配
        finalHostList.addAll(scopeHostService.getScopeHostsByHostNames(appResourceScope, remainKeys));
        return finalHostList;
    }

    @Override
    public List<ApplicationHostDTO> findHosts(AppResourceScope appResourceScope, HostCheckReq req) {
        List<ApplicationHostDTO> hostDTOList = new ArrayList<>();
        // 根据主机ID解析主机
        findHostsByHostIds(appResourceScope, req.getActionScope(), req.getHostIdList(), hostDTOList);
        // 根据Ipv4解析主机
        findHostsByIpv4s(appResourceScope, req.getActionScope(), req.getIpList(), hostDTOList);
        // 根据Ipv6解析主机
        findHostsByIpv6s(appResourceScope, req.getActionScope(), req.getIpv6List(), hostDTOList);
        // 根据关键字（主机名称）解析主机
        findHostsByKeys(appResourceScope, req.getActionScope(), req.getKeyList(), hostDTOList);
        // 去重
        Set<Long> hostIdSet = new HashSet<>();
        Iterator<ApplicationHostDTO> iterator = hostDTOList.iterator();
        while (iterator.hasNext()) {
            ApplicationHostDTO hostDTO = iterator.next();
            if (hostIdSet.contains(hostDTO.getHostId())) {
                iterator.remove();
            } else {
                hostIdSet.add(hostDTO.getHostId());
            }
        }
        return hostDTOList;
    }

    private void findHostsByHostIds(AppResourceScope appResourceScope,
                                    ActionScopeEnum actionScope,
                                    List<Long> hostIdList,
                                    List<ApplicationHostDTO> hostDTOList) {
        if (CollectionUtils.isNotEmpty(hostIdList)) {
            // 根据hostId查资源范围及白名单内的主机详情
            hostDTOList.addAll(getScopeHostsIncludingWhiteIPByHostId(
                appResourceScope,
                actionScope,
                hostIdList
            ));
        }
    }

    private void findHostsByIpv4s(AppResourceScope appResourceScope,
                                  ActionScopeEnum actionScope,
                                  List<String> ipOrCloudIpList,
                                  List<ApplicationHostDTO> hostDTOList) {
        if (CollectionUtils.isEmpty(ipOrCloudIpList)) {
            return;
        }
        Pair<Set<String>, Set<String>> pair = IpUtils.parseCleanIpv4AndCloudIpv4s(ipOrCloudIpList);
        Set<String> ipSet = pair.getLeft();
        Set<String> cloudIpSet = pair.getRight();
        // 根据ip地址查资源范围及白名单内的主机详情
        if (CollectionUtils.isNotEmpty(ipSet)) {
            hostDTOList.addAll(getScopeHostsIncludingWhiteIPByIp(
                appResourceScope,
                actionScope,
                ipSet
            ));
        }
        if (CollectionUtils.isNotEmpty(cloudIpSet)) {
            hostDTOList.addAll(getScopeHostsIncludingWhiteIPByCloudIp(
                appResourceScope,
                actionScope,
                cloudIpSet
            ));
        }
    }

    private void findHostsByIpv6s(AppResourceScope appResourceScope,
                                  ActionScopeEnum actionScope,
                                  List<String> ipv6OrCloudIpv6List,
                                  List<ApplicationHostDTO> hostDTOList) {
        if (CollectionUtils.isEmpty(ipv6OrCloudIpv6List)) {
            return;
        }
        Pair<Set<String>, Set<Pair<Long, String>>> pair = IpUtils.parseFullIpv6AndCloudIpv6s(ipv6OrCloudIpv6List);
        Set<String> ipv6Set = pair.getLeft();
        Set<Pair<Long, String>> cloudIpv6Set = pair.getRight();
        Set<String> allIpv6Set = new HashSet<>(ipv6Set);
        Map<String, Long> ipv6CloudIdMap = new HashMap<>();
        for (Pair<Long, String> cloudIpv6 : cloudIpv6Set) {
            allIpv6Set.add(cloudIpv6.getRight());
            ipv6CloudIdMap.put(cloudIpv6.getRight(), cloudIpv6.getLeft());
        }
        List<ApplicationHostDTO> hostList = getScopeHostsIncludingWhiteIPByIpv6(
            appResourceScope,
            actionScope,
            allIpv6Set
        );
        for (ApplicationHostDTO host : hostList) {
            String ipv6 = host.getIpv6();
            Long cloudId = host.getCloudAreaId();
            // 未指定云区域ID的数据匹配所有ipv6符合的数据
            if (ipv6Set.contains(ipv6) ||
                // 指定了云区域ID的数据需要精确匹配
                (ipv6CloudIdMap.containsKey(ipv6) && cloudId.equals(ipv6CloudIdMap.get(ipv6)))) {
                // 根据ipv6地址查资源范围及白名单内的主机详情
                hostDTOList.add(host);
            }
        }
    }

    private void findHostsByKeys(AppResourceScope appResourceScope,
                                 ActionScopeEnum actionScope,
                                 List<String> keyList,
                                 List<ApplicationHostDTO> hostDTOList) {
        if (CollectionUtils.isEmpty(keyList)) {
            return;
        }
        // 根据关键字（主机名称）查资源范围及白名单内的主机详情
        hostDTOList.addAll(getScopeHostsIncludingWhiteIPByKey(
            appResourceScope,
            actionScope,
            keyList
        ));
    }
}
