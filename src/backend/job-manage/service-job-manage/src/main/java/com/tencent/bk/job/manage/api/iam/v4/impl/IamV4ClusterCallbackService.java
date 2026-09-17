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

import com.tencent.bk.job.common.cc.model.container.KubeClusterDTO;
import com.tencent.bk.job.common.cc.model.query.KubeClusterQuery;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
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
 * 容器集群的 IAM v4 回调实现。
 *
 * <p>实例 ID 用 CMDB 原生集群 ID。{@code list_instance} 的 parent 是业务，可直接查 CMDB；
 * {@code fetch_instance_info} 只传 ids，缺业务信息无法查 CMDB，待 IAM 回调协议补齐祖先信息后实现。</p>
 */
@Slf4j
@Component
public class IamV4ClusterCallbackService extends BaseIamV4CallbackService {

    private final IBizCmdbClient bizCmdbClient;

    @Autowired
    public IamV4ClusterCallbackService(IBizCmdbClient bizCmdbClient,
                                            AppScopeMappingService appScopeMappingService,
                                            TenantEnvService tenantEnvService,
                                            IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.CLUSTER;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        long bizId = parseParentBizId(filter == null ? null : filter.getParent());
        List<KubeClusterDTO> clusters = bizCmdbClient.listKubeClusters(
            KubeClusterQuery.Builder.builder(bizId).build());

        // CMDB 容器接口不支持名称模糊过滤，只能取回后在内存里筛
        String keyword = filter == null ? null : filter.getKeyword();
        List<KubeClusterDTO> matched = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(clusters)) {
            for (KubeClusterDTO cluster : clusters) {
                String displayName = buildDisplayName(cluster);
                if (StringUtils.isNotBlank(keyword)
                    && (displayName == null || !displayName.contains(keyword))) {
                    continue;
                }
                matched.add(cluster);
            }
        }

        BaseSearchCondition condition = buildSearchCondition(request.getPage());
        int start = Math.min(condition.getStart(), matched.size());
        int end = Math.min(start + condition.getLength(), matched.size());
        List<V4BaseInstanceDTO> results = new ArrayList<>(end - start);
        for (KubeClusterDTO cluster : matched.subList(start, end)) {
            results.add(new V4BaseInstanceDTO(String.valueOf(cluster.getId()), buildDisplayName(cluster)));
        }

        V4ListInstanceDataDTO data = new V4ListInstanceDataDTO();
        data.setCount((long) matched.size());
        data.setResults(results);
        return data;
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        // TODO 依赖 IAM v4 回调协议新增的祖先信息字段（字段名待 IAM 侧确定，见 openspec design.md D13）：
        //  届时实现为 从回调请求取 bizId -> 按 (bizId, clusterIds) 批量查 CMDB 取集群 UID，
        //  路径 /biz_container,{bizId}/。
        //  当前 fetch_instance_info 的 filter 只有 ids，而 CMDB 容器接口强制要求 bizId，
        //  仅凭集群 ID 无法定位业务，故返回空列表，权限中心侧会按「已删除」渲染。
        int idCount = request.getFilter() == null || request.getFilter().getIds() == null
            ? 0 : request.getFilter().getIds().size();
        log.warn("fetch_instance_info for cluster is not supported until IAM callback carries "
            + "ancestor info|idCount={}", idCount);
        return Collections.emptyList();
    }

    /**
     * 集群的上级恒为业务（容器视图首段 {@code biz_container}），解析出 CMDB 侧 bizId。
     */
    private long parseParentBizId(V4CallbackParentDTO parent) {
        if (parent == null || StringUtils.isBlank(parent.getType()) || StringUtils.isBlank(parent.getId())) {
            throw new IllegalArgumentException("Parent biz is required for listing cluster instances");
        }
        ResourceScope scope = toResourceScope(parent);
        try {
            return Long.parseLong(scope.getId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal parent biz id: " + parent.getId());
        }
    }

    /**
     * 集群在权限中心的展示名，用集群 UID（如 {@code BCS-K8S-00000}）。
     *
     * <p>不用集群名：UID 才是运维同学在 BCS 上认的标识；UID 缺失时回落集群名兜底。</p>
     */
    private String buildDisplayName(KubeClusterDTO cluster) {
        return StringUtils.isNotBlank(cluster.getUid()) ? cluster.getUid() : cluster.getName();
    }
}
