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

import com.tencent.bk.job.common.cc.sdk.BkNetClient;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.model.query.HostQuery;
import com.tencent.bk.job.manage.model.web.request.ipchooser.BizTopoNode;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.BizHostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.impl.topo.BizTopoService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资源范围下的主机服务
 */
@Slf4j
@Service
public class ScopeHostServiceImpl implements ScopeHostService {

    private final ApplicationService applicationService;
    private final BizHostService bizHostService;
    private final BkNetClient bkNetClient;
    private final BizTopoService bizTopoService;

    @Autowired
    public ScopeHostServiceImpl(ApplicationService applicationService,
                                BizHostService bizHostService,
                                BkNetClient bkNetClient,
                                BizTopoService bizTopoService) {
        this.applicationService = applicationService;
        this.bizHostService = bizHostService;
        this.bkNetClient = bkNetClient;
        this.bizTopoService = bizTopoService;
    }

    @Override
    public List<Long> filterScopeHostIds(AppResourceScope appResourceScope,
                                         Collection<Long> hostIds) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllBizSet()) {
            // 全业务
            return new ArrayList<>(hostIds);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return bizHostService.filterHostIdsByBiz(bizIds, hostIds);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return bizHostService.filterHostIdsByBiz(Collections.singletonList(bizId), hostIds);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByIds(AppResourceScope appResourceScope,
                                                       Collection<Long> hostIds) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllBizSet()) {
            // 全业务
            return bizHostService.getHostsByHostIds(hostIds);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return bizHostService.getHostsByBizAndHostIds(bizIds, hostIds);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return bizHostService.getHostsByBizAndHostIds(Collections.singletonList(bizId), hostIds);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByIps(AppResourceScope appResourceScope, Collection<String> ips) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllBizSet()) {
            // 全业务
            return bizHostService.getHostsByIps(ips);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return bizHostService.getHostsByBizAndIps(bizIds, ips);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return bizHostService.getHostsByBizAndIps(Collections.singletonList(bizId), ips);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByCloudIps(AppResourceScope appResourceScope,
                                                            Collection<String> cloudIps) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllBizSet()) {
            // 全业务
            return bizHostService.getHostsByCloudIps(cloudIps);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return bizHostService.getHostsByBizAndCloudIps(bizIds, cloudIps);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return bizHostService.getHostsByBizAndCloudIps(Collections.singletonList(bizId), cloudIps);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByIpv6s(AppResourceScope appResourceScope, Collection<String> ipv6s) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllBizSet()) {
            // 全业务
            return bizHostService.getHostsByIpv6s(ipv6s);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return bizHostService.getHostsByBizAndIpv6s(bizIds, ipv6s);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return bizHostService.getHostsByBizAndIpv6s(Collections.singletonList(bizId), ipv6s);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByHostNames(AppResourceScope appResourceScope,
                                                             Collection<String> hostNames) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllBizSet()) {
            // 全业务
            return bizHostService.getHostsByHostNames(hostNames);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return bizHostService.getHostsByBizAndHostNames(bizIds, hostNames);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return bizHostService.getHostsByBizAndHostNames(Collections.singletonList(bizId), hostNames);
        }
    }

    @Override
    public PageData<Long> listHostIdByBizTopologyNodes(AppResourceScope appResourceScope,
                                                       List<BizTopoNode> appTopoNodeList,
                                                       String searchContent,
                                                       Integer agentAlive,
                                                       List<String> ipKeyList,
                                                       List<String> ipv6KeyList,
                                                       List<String> hostNameKeyList,
                                                       List<String> osNameKeyList,
                                                       Long start,
                                                       Long pageSize) {
        StopWatch watch = new StopWatch("listHostByBizTopologyNodes");
        watch.start("genConditions");
        BasicParsedSearchConditions basicConditions = buildSearchConditions(
            appResourceScope,
            appTopoNodeList,
            searchContent
        );
        watch.stop();
        watch.start("pageListHostId");
        HostQuery hostQuery = HostQuery.builder()
            .bizIds(basicConditions.getBizIds())
            .moduleIds(basicConditions.getModuleIds())
            .cloudAreaIds(basicConditions.getCloudAreaIds())
            .searchContents(basicConditions.getSearchContents())
            .agentAlive(agentAlive)
            .ipKeyList(ipKeyList)
            .ipv6KeyList(ipv6KeyList)
            .hostNameKeyList(hostNameKeyList)
            .osNameKeyList(osNameKeyList)
            .start(start)
            .limit(pageSize)
            .build();
        PageData<Long> result = bizHostService.pageListHostId(hostQuery);
        watch.stop();
        if (watch.getTotalTimeMillis() > 5000) {
            log.warn("listHostIdByBizTopologyNodes slow:" + watch.prettyPrint());
        }
        return result;
    }

    @Override
    public PageData<ApplicationHostDTO> searchHost(AppResourceScope appResourceScope,
                                                   List<BizTopoNode> appTopoNodeList,
                                                   Integer agentAlive,
                                                   String searchContent,
                                                   List<String> ipKeyList,
                                                   List<String> ipv6KeyList,
                                                   List<String> hostNameKeyList,
                                                   List<String> osNameKeyList,
                                                   Long start,
                                                   Long pageSize) {
        BasicParsedSearchConditions basicConditions = buildSearchConditions(
            appResourceScope,
            appTopoNodeList,
            searchContent
        );
        if (CollectionUtils.isNotEmpty(ipv6KeyList)) {
            // 对有效的IPv6地址补全为全写形式再搜索
            ipv6KeyList = ipv6KeyList.stream().map(ipv6Key -> {
                if (IpUtils.checkIpv6(ipv6Key)) {
                    return IpUtils.getFullIpv6ByCompressedOne(ipv6Key);
                }
                return ipv6Key;
            }).collect(Collectors.toList());
        }
        HostQuery hostQuery = HostQuery.builder()
            .bizIds(basicConditions.getBizIds())
            .moduleIds(basicConditions.getModuleIds())
            .cloudAreaIds(basicConditions.getCloudAreaIds())
            .searchContents(basicConditions.getSearchContents())
            .agentAlive(agentAlive)
            .ipKeyList(ipKeyList)
            .ipv6KeyList(ipv6KeyList)
            .hostNameKeyList(hostNameKeyList)
            .osNameKeyList(osNameKeyList)
            .start(start)
            .limit(pageSize)
            .build();
        return bizHostService.pageListHost(hostQuery);
    }

    private BasicParsedSearchConditions buildSearchConditions(AppResourceScope appResourceScope,
                                                              List<BizTopoNode> appTopoNodeList,
                                                              String searchContent) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        List<Long> moduleIds = null;
        List<Long> bizIds = null;
        if (applicationDTO.isAllBizSet()) {
            // 全业务
            log.debug("listHostIdByBizTopologyNodes of allBizSet:{}", appResourceScope);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            bizIds = applicationDTO.getSubBizIds();
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            moduleIds = bizTopoService.findAllModuleIdsOfNodes(bizId, appTopoNodeList);
        }

        List<String> searchContents = null;
        if (searchContent != null) {
            searchContents = StringUtil.splitByNormalSeparator(searchContent.trim()).stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        }

        //获取所有云区域，找出名称符合条件的所有CloudAreaId
        List<Long> cloudAreaIds = bkNetClient.getAnyNameMatchedCloudAreaIds(searchContents);
        return new BasicParsedSearchConditions(bizIds, moduleIds, cloudAreaIds, searchContents);
    }

    @Getter
    @AllArgsConstructor
    static class BasicParsedSearchConditions {
        List<Long> bizIds;
        List<Long> moduleIds;
        List<Long> cloudAreaIds;
        List<String> searchContents;
    }
}
