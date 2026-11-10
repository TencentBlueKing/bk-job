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

package com.tencent.bk.job.manage.api.iam.v4.impl;

import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.common.iam.util.IamV4RespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.service.host.CurrentTenantBizHostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackFilterDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackParentDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4BaseInstanceDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4InstanceInfoDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4ListInstanceDataDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 主机资源的 IAM v4 回调实现。数据取自 Job 本地已同步的主机表，不实时请求 CMDB。
 *
 * <p>主机的 {@code _bk_iam_path_} 恒为单条 {@code /biz,{bizId}/}：主机的 6 种实例视图里只有默认视图
 * 以主机实例为叶子，且 CMDB 中一台主机只属于一个业务。</p>
 */
@Slf4j
@Component
public class IamV4HostCallbackService extends BaseIamV4CallbackService {

    private final ScopeHostService scopeHostService;
    private final CurrentTenantBizHostService currentTenantBizHostService;

    @Autowired
    public IamV4HostCallbackService(ScopeHostService scopeHostService,
                                         CurrentTenantBizHostService currentTenantBizHostService,
                                         AppScopeMappingService appScopeMappingService,
                                         TenantEnvService tenantEnvService,
                                         IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.scopeHostService = scopeHostService;
        this.currentTenantBizHostService = currentTenantBizHostService;
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.HOST;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        AppResourceScope appResourceScope = resolveScope(filter == null ? null : filter.getParent());
        String keyword = filter == null ? null : filter.getKeyword();
        BaseSearchCondition condition = buildSearchCondition(request.getPage());
        // 业务下列举主机未带拓扑节点时，需以业务根节点检索
        List<BizTopoNode> topoNodes = appResourceScope.isBiz()
            ? Collections.singletonList(buildBizRootNode(appResourceScope))
            : null;

        PageData<ApplicationHostDTO> pageData = scopeHostService.searchHost(
            appResourceScope,
            topoNodes,
            null,
            StringUtils.isBlank(keyword) ? null : keyword,
            null,
            null,
            null,
            null,
            condition.getStart().longValue(),
            condition.getLength().longValue()
        );
        return IamV4RespUtil.buildListInstanceData(pageData,
            host -> new V4BaseInstanceDTO(String.valueOf(host.getHostId()), buildDisplayName(host)));
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        if (filter == null || CollectionUtils.isEmpty(filter.getIds())) {
            return Collections.emptyList();
        }
        List<Long> hostIds = parseLongIds(filter.getIds());
        if (hostIds.isEmpty()) {
            return Collections.emptyList();
        }
        // fetch_instance_info 不带 parent，故不能走要求资源范围的 ScopeHostService；主机 ID 全局唯一，按租户直查即可
        List<ApplicationHostDTO> hosts = currentTenantBizHostService.getHostsByHostIds(hostIds);

        boolean needPath = isPathRequired(request);
        List<V4InstanceInfoDTO> result = new ArrayList<>(hosts.size());
        for (ApplicationHostDTO host : hosts) {
            V4InstanceInfoDTO instance = new V4InstanceInfoDTO();
            instance.setId(String.valueOf(host.getHostId()));
            instance.setDisplayName(buildDisplayName(host));
            if (needPath) {
                instance.setBkIamPath(buildHostPaths(host));
            }
            result.add(instance);
        }
        if (result.size() < hostIds.size()) {
            log.warn("Some hosts not found in IAM v4 callback, may be deleted|requested={}|found={}",
                hostIds.size(), result.size());
        }
        return result;
    }

    /**
     * 解析 list_instance 的资源范围。主机只在「业务下主机」视图里可选，parent 恒为业务。
     */
    private AppResourceScope resolveScope(V4CallbackParentDTO parent) {
        if (parent == null || StringUtils.isBlank(parent.getType()) || StringUtils.isBlank(parent.getId())) {
            throw new IllegalArgumentException("Parent biz is required for listing host instances");
        }
        ResourceScope scope = toResourceScope(parent);
        Long appId = appScopeMappingService.getAppIdByScope(scope);
        return new AppResourceScope(appId, scope);
    }

    /**
     * 构造业务根节点，让 searchHost 按整棵业务拓扑展开模块。
     */
    private BizTopoNode buildBizRootNode(AppResourceScope appResourceScope) {
        BizTopoNode bizRootNode = new BizTopoNode();
        bizRootNode.setObjectId(CcNodeTypeEnum.BIZ.getType());
        bizRootNode.setInstanceId(Long.parseLong(appResourceScope.getId()));
        return bizRootNode;
    }

    /**
     * 主机在权限中心的展示名，用 {@code 云区域ID:IPv4}（如 {@code 0:127.0.0.1}）。
     *
     * <p>不能只用 IP：同一 IP 在不同云区域下可以重复，加上云区域 ID 才能唯一标识一台主机。</p>
     */
    private String buildDisplayName(ApplicationHostDTO host) {
        String cloudIp = host.getCloudIp();
        return StringUtils.isNotBlank(cloudIp) ? cloudIp : host.getHostName();
    }

    private String buildHostPaths(ApplicationHostDTO host) {
        if (host.getBizId() == null) {
            return null;
        }
        return buildScopePaths(new ResourceScope(ResourceScopeTypeEnum.BIZ, String.valueOf(host.getBizId())));
    }
}
