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

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.model.query.HostQuery;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.cloudarea.BkNetService;
import com.tencent.bk.job.manage.service.host.CurrentTenantBizHostService;
import com.tencent.bk.job.manage.service.host.NoTenantBizHostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.topo.BizTopoService;
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
    private final CurrentTenantBizHostService currentTenantBizHostService;
    private final NoTenantBizHostService noTenantBizHostService;
    private final NoTenantHostDAO noTenantHostDAO;
    private final BkNetService bkNetService;
    private final BizTopoService bizTopoService;

    @Autowired
    public ScopeHostServiceImpl(ApplicationService applicationService,
                                CurrentTenantBizHostService currentTenantBizHostService,
                                NoTenantBizHostService noTenantBizHostService,
                                NoTenantHostDAO noTenantHostDAO,
                                BkNetService bkNetService,
                                BizTopoService bizTopoService) {
        this.applicationService = applicationService;
        this.currentTenantBizHostService = currentTenantBizHostService;
        this.noTenantBizHostService = noTenantBizHostService;
        this.noTenantHostDAO = noTenantHostDAO;
        this.bkNetService = bkNetService;
        this.bizTopoService = bizTopoService;
    }

    @Override
    public List<Long> filterScopeHostIds(AppResourceScope appResourceScope,
                                         Collection<Long> hostIds) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllTenantSet()) {
            // 全租户
            return new ArrayList<>(hostIds);
        } else if (applicationDTO.isAllBizSet()) {
            // 全业务
            return currentTenantBizHostService.filterHostIds(hostIds);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return currentTenantBizHostService.filterHostIdsByBiz(bizIds, hostIds);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return currentTenantBizHostService.filterHostIdsByBiz(Collections.singletonList(bizId), hostIds);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByIds(AppResourceScope appResourceScope,
                                                       Collection<Long> hostIds) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllTenantSet()) {
            // 全租户
            return noTenantHostDAO.listHostInfoByHostIds(hostIds);
        } else if (applicationDTO.isAllBizSet()) {
            // 全业务
            return currentTenantBizHostService.getHostsByHostIds(hostIds);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return currentTenantBizHostService.getHostsByBizAndHostIds(bizIds, hostIds);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return currentTenantBizHostService.getHostsByBizAndHostIds(Collections.singletonList(bizId), hostIds);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByIps(AppResourceScope appResourceScope, Collection<String> ips) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllTenantSet()) {
            // 全租户
            return noTenantHostDAO.listHostInfoByIps(ips);
        } else if (applicationDTO.isAllBizSet()) {
            // 全业务
            return currentTenantBizHostService.getHostsByIps(ips);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return currentTenantBizHostService.getHostsByBizAndIps(bizIds, ips);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return currentTenantBizHostService.getHostsByBizAndIps(Collections.singletonList(bizId), ips);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByCloudIps(AppResourceScope appResourceScope,
                                                            Collection<String> cloudIps) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllTenantSet()) {
            // 全租户
            return noTenantHostDAO.listHostInfoByCloudIps(cloudIps);
        } else if (applicationDTO.isAllBizSet()) {
            // 全业务
            return currentTenantBizHostService.getHostsByCloudIps(cloudIps);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return currentTenantBizHostService.getHostsByBizAndCloudIps(bizIds, cloudIps);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return currentTenantBizHostService.getHostsByBizAndCloudIps(Collections.singletonList(bizId), cloudIps);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByIpv6s(AppResourceScope appResourceScope, Collection<String> ipv6s) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllTenantSet()) {
            // 全租户
            return noTenantHostDAO.listHostInfoByIpv6s(ipv6s);
        } else if (applicationDTO.isAllBizSet()) {
            // 全业务
            return currentTenantBizHostService.getHostsByIpv6s(ipv6s);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return currentTenantBizHostService.getHostsByBizAndIpv6s(bizIds, ipv6s);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return currentTenantBizHostService.getHostsByBizAndIpv6s(Collections.singletonList(bizId), ipv6s);
        }
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByHostNames(AppResourceScope appResourceScope,
                                                             Collection<String> hostNames) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllTenantSet()) {
            // 全租户
            return noTenantHostDAO.listHostInfoByHostNames(hostNames);
        } else if (applicationDTO.isAllBizSet()) {
            // 全业务
            return currentTenantBizHostService.getHostsByHostNames(hostNames);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return currentTenantBizHostService.getHostsByBizAndHostNames(bizIds, hostNames);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return currentTenantBizHostService.getHostsByBizAndHostNames(Collections.singletonList(bizId), hostNames);
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
        PageData<Long> result;
        if (basicConditions.isAllTenant()) {
            result = noTenantBizHostService.pageListHostId(hostQuery);
        } else {
            result = currentTenantBizHostService.pageListHostId(hostQuery);
        }
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
        if (basicConditions.isAllTenant()) {
            return noTenantBizHostService.pageListHost(hostQuery);
        } else {
            return currentTenantBizHostService.pageListHost(hostQuery);
        }
    }

    private BasicParsedSearchConditions buildSearchConditions(AppResourceScope appResourceScope,
                                                              List<BizTopoNode> appTopoNodeList,
                                                              String searchContent) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        List<Long> moduleIds = null;
        List<Long> bizIds = null;
        boolean allTenant = false;
        if (applicationDTO.isAllTenantSet()) {
            // 全租户
            allTenant = true;
            log.debug("listHostIdByBizTopologyNodes of allTenant:{}", appResourceScope);
        } else if (applicationDTO.isAllBizSet()) {
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
        String tenantId = JobContextUtil.getTenantId();
        List<Long> cloudAreaIds = bkNetService.getAnyNameMatchedCloudAreaIds(tenantId, searchContents);
        return new BasicParsedSearchConditions(allTenant, bizIds, moduleIds, cloudAreaIds, searchContents);
    }

    @Getter
    @AllArgsConstructor
    static class BasicParsedSearchConditions {
        boolean allTenant;
        List<Long> bizIds;
        List<Long> moduleIds;
        List<Long> cloudAreaIds;
        List<String> searchContents;
    }
}
