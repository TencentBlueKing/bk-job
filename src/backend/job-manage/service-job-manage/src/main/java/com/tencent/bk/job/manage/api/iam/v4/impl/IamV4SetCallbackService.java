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

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.JobContextUtil;
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
 * 集群（CMDB 拓扑节点）的 IAM v4 回调实现。
 *
 * <p>实例取自业务拓扑树（{@code getBizInstTopologyPreferCache}，客户端带 30s 缓存），命中缓存不回源 CMDB。
 * CMDB 没有「按父节点分页列举子节点」的接口，只能整树取回后在内存里过滤分页。</p>
 */
@Slf4j
@Component
public class IamV4SetCallbackService extends BaseIamV4CallbackService {

    private final IBizCmdbClient bizCmdbClient;

    @Autowired
    public IamV4SetCallbackService(IBizCmdbClient bizCmdbClient,
                                        AppScopeMappingService appScopeMappingService,
                                        TenantEnvService tenantEnvService,
                                        IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.SET;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        long bizId = parseParentBizId(filter == null ? null : filter.getParent());
        InstanceTopologyDTO topo = bizCmdbClient.getBizInstTopologyPreferCache(
            JobContextUtil.getTenantId(), bizId);

        List<InstanceTopologyDTO> sets = new ArrayList<>();
        if (topo != null && CollectionUtils.isNotEmpty(topo.getChild())) {
            String keyword = filter == null ? null : filter.getKeyword();
            for (InstanceTopologyDTO child : topo.getChild()) {
                if (!IamV4ResourceTypeId.SET.equals(child.getObjectId())) {
                    continue;
                }
                if (StringUtils.isNotBlank(keyword)
                    && (child.getInstanceName() == null || !child.getInstanceName().contains(keyword))) {
                    continue;
                }
                sets.add(child);
            }
        }

        BaseSearchCondition condition = buildSearchCondition(request.getPage());
        V4ListInstanceDataDTO data = new V4ListInstanceDataDTO();
        data.setCount((long) sets.size());
        data.setResults(pageResults(sets, condition));
        return data;
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        // TODO 依赖 IAM v4 回调协议新增的祖先信息字段（字段名待 IAM 侧确定，见 openspec design.md D13）：
        //  届时实现为 从回调请求取 bizId -> 拓扑树缓存取集群名称，路径 /biz,{bizId}/。
        //  当前 fetch_instance_info 只传 ids，而 CMDB 拓扑接口的 bk_biz_id 均为 URL 必填，
        //  仅凭 setId 无法定位业务，故返回空列表，权限中心侧会按「已删除」渲染。
        int idCount = request.getFilter() == null || request.getFilter().getIds() == null
            ? 0 : request.getFilter().getIds().size();
        log.warn("fetch_instance_info for set is not supported until IAM callback carries "
            + "ancestor info|idCount={}", idCount);
        return Collections.emptyList();
    }

    /**
     * 集群的上级恒为业务，解析出 CMDB 侧 bizId。
     */
    private long parseParentBizId(V4CallbackParentDTO parent) {
        if (parent == null || StringUtils.isBlank(parent.getType()) || StringUtils.isBlank(parent.getId())) {
            throw new IllegalArgumentException("Parent biz is required for listing set instances");
        }
        ResourceScope scope = toResourceScope(parent);
        try {
            return Long.parseLong(scope.getId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal parent biz id: " + parent.getId());
        }
    }

    private List<V4BaseInstanceDTO> pageResults(List<InstanceTopologyDTO> nodes, BaseSearchCondition condition) {
        int start = Math.min(condition.getStart(), nodes.size());
        int end = Math.min(start + condition.getLength(), nodes.size());
        List<V4BaseInstanceDTO> results = new ArrayList<>(end - start);
        for (InstanceTopologyDTO node : nodes.subList(start, end)) {
            results.add(new V4BaseInstanceDTO(String.valueOf(node.getInstanceId()), node.getInstanceName()));
        }
        return results;
    }
}
