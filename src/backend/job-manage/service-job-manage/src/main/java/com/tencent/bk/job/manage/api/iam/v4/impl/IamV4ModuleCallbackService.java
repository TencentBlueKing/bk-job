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
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackFilterDTO;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 模块（CMDB 拓扑节点）的 IAM v4 回调实现。
 *
 * <p>{@code list_instance} 从 {@code filter.ancestors} 取 bizId、从 parent 取 setId，
 * 再走拓扑树缓存取该集群下的模块。{@code fetch_instance_info} 用 {@code filter.root_resource_id}
 * 作为 bizId，在同一棵拓扑树里取模块名与所属 set。</p>
 */
@Slf4j
@Component
public class IamV4ModuleCallbackService extends BaseIamV4CallbackService {

    private final IBizCmdbClient bizCmdbClient;

    @Autowired
    public IamV4ModuleCallbackService(IBizCmdbClient bizCmdbClient,
                                          AppScopeMappingService appScopeMappingService,
                                          TenantEnvService tenantEnvService,
                                          IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.MODULE;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        long bizId = parseAncestorBizId(filter);
        long setId = parseParentInstanceId(filter == null ? null : filter.getParent(), IamV4ResourceTypeId.SET);
        InstanceTopologyDTO topo = bizCmdbClient.getBizInstTopologyPreferCache(
            JobContextUtil.getTenantId(), bizId);

        List<InstanceTopologyDTO> modules = new ArrayList<>();
        InstanceTopologyDTO setNode = findTopoNode(topo, IamV4ResourceTypeId.SET, setId);
        if (setNode != null && CollectionUtils.isNotEmpty(setNode.getChild())) {
            String keyword = filter.getKeyword();
            for (InstanceTopologyDTO child : setNode.getChild()) {
                if (!IamV4ResourceTypeId.MODULE.equals(child.getObjectId())) {
                    continue;
                }
                if (StringUtils.isNotBlank(keyword)
                    && (child.getInstanceName() == null || !child.getInstanceName().contains(keyword))) {
                    continue;
                }
                modules.add(child);
            }
        }

        BaseSearchCondition condition = buildSearchCondition(request.getPage());
        V4ListInstanceDataDTO data = new V4ListInstanceDataDTO();
        data.setCount((long) modules.size());
        data.setResults(pageResults(modules, condition));
        return data;
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        if (filter == null || CollectionUtils.isEmpty(filter.getIds())) {
            return Collections.emptyList();
        }
        List<Long> moduleIds = parseLongIds(filter.getIds());
        if (moduleIds.isEmpty()) {
            return Collections.emptyList();
        }
        long bizId = parseRootResourceBizId(filter);
        InstanceTopologyDTO topo = bizCmdbClient.getBizInstTopologyPreferCache(
            JobContextUtil.getTenantId(), bizId);

        boolean needPath = isPathRequired(request);
        Set<Long> wanted = new HashSet<>(moduleIds);
        List<V4InstanceInfoDTO> result = new ArrayList<>();
        collectModules(topo, wanted, bizId, needPath, result);
        if (result.size() < moduleIds.size()) {
            log.warn("Some modules not found in IAM v4 callback, may be deleted|bizId={}|requested={}|found={}",
                bizId, moduleIds.size(), result.size());
        }
        return result;
    }

    /**
     * 遍历拓扑树收集模块；路径需要所属 set，故用当前节点作为 parent 往下看 child。
     */
    private void collectModules(InstanceTopologyDTO node,
                                Set<Long> wanted,
                                long bizId,
                                boolean needPath,
                                List<V4InstanceInfoDTO> result) {
        if (node == null || CollectionUtils.isEmpty(node.getChild()) || wanted.isEmpty()) {
            return;
        }
        for (InstanceTopologyDTO child : node.getChild()) {
            if (IamV4ResourceTypeId.MODULE.equals(child.getObjectId())
                && child.getInstanceId() != null
                && wanted.remove(child.getInstanceId())) {
                V4InstanceInfoDTO instance = new V4InstanceInfoDTO();
                instance.setId(String.valueOf(child.getInstanceId()));
                instance.setDisplayName(child.getInstanceName());
                if (needPath && IamV4ResourceTypeId.SET.equals(node.getObjectId())
                    && node.getInstanceId() != null) {
                    instance.setBkIamPath(buildIamPath(
                        IamV4ResourceTypeId.BIZ, String.valueOf(bizId),
                        IamV4ResourceTypeId.SET, String.valueOf(node.getInstanceId())));
                }
                result.add(instance);
            }
            collectModules(child, wanted, bizId, needPath, result);
        }
    }

    private InstanceTopologyDTO findTopoNode(InstanceTopologyDTO node, String objectId, long instId) {
        if (node == null) {
            return null;
        }
        if (objectId.equals(node.getObjectId())
            && node.getInstanceId() != null
            && node.getInstanceId() == instId) {
            return node;
        }
        if (CollectionUtils.isEmpty(node.getChild())) {
            return null;
        }
        for (InstanceTopologyDTO child : node.getChild()) {
            InstanceTopologyDTO hit = findTopoNode(child, objectId, instId);
            if (hit != null) {
                return hit;
            }
        }
        return null;
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
