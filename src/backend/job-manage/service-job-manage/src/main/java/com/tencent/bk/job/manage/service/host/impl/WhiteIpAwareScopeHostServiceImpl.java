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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.service.WhiteIPService;
import com.tencent.bk.job.manage.service.host.HostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.host.WhiteIpAwareScopeHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
    private final HostService hostService;
    private final ScopeHostService scopeHostService;

    @Autowired
    public WhiteIpAwareScopeHostServiceImpl(WhiteIPService whiteIPService,
                                            HostService hostService,
                                            ScopeHostService scopeHostService) {
        this.whiteIPService = whiteIPService;
        this.hostService = hostService;
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
        List<ApplicationHostDTO> whiteIpHostList = hostService.listHosts(whiteIpHostDTOList);
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
        List<ApplicationHostDTO> whiteIpHostList = hostService.listHosts(whiteIpHostDTOList);
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
        Map<String, ApplicationHostDTO> map = hostService.listHostsByIps(cloudIps);
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
        return hostService.listHosts(whiteIpHostDTOList);
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
        List<ApplicationHostDTO> whiteIpHostList = hostService.listHosts(whiteIpHostDTOList);
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
        // 当前关键字仅支持主机名称匹配
        return scopeHostService.getScopeHostsByHostNames(appResourceScope, keys);
    }
}
