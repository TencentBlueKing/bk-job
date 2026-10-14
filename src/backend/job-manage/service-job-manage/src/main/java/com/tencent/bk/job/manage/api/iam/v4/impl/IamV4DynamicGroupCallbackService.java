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

import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.model.dto.DynamicGroupDTO;
import com.tencent.bk.job.manage.service.impl.BizDynamicGroupService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 动态分组的 IAM v4 回调实现。
 *
 * <p>Job 侧对动态分组没有缓存，{@code list_instance} 直接实时查 CMDB。CMDB 的动态分组接口不支持分页与
 * 名称模糊，故整批取回后在内存里过滤分页。</p>
 */
@Slf4j
@Component
public class IamV4DynamicGroupCallbackService extends BaseIamV4CallbackService {

    private final BizDynamicGroupService bizDynamicGroupService;

    @Autowired
    public IamV4DynamicGroupCallbackService(BizDynamicGroupService bizDynamicGroupService,
                                                 AppScopeMappingService appScopeMappingService,
                                                 TenantEnvService tenantEnvService,
                                                 IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.bizDynamicGroupService = bizDynamicGroupService;
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.DYNAMIC_GROUP;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        long bizId = parseParentBizId(filter == null ? null : filter.getParent());
        List<DynamicGroupDTO> groups = bizDynamicGroupService.listDynamicGroup(
            JobContextUtil.getTenantId(), bizId);

        String keyword = filter == null ? null : filter.getKeyword();
        List<DynamicGroupDTO> matched = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(groups)) {
            for (DynamicGroupDTO group : groups) {
                if (StringUtils.isNotBlank(keyword)
                    && (group.getName() == null || !group.getName().contains(keyword))) {
                    continue;
                }
                matched.add(group);
            }
        }

        BaseSearchCondition condition = buildSearchCondition(request.getPage());
        int start = Math.min(condition.getStart(), matched.size());
        int end = Math.min(start + condition.getLength(), matched.size());
        List<V4BaseInstanceDTO> results = new ArrayList<>(end - start);
        for (DynamicGroupDTO group : matched.subList(start, end)) {
            results.add(new V4BaseInstanceDTO(group.getId(), group.getName()));
        }

        V4ListInstanceDataDTO data = new V4ListInstanceDataDTO();
        data.setCount((long) matched.size());
        data.setResults(results);
        return data;
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        if (filter == null || CollectionUtils.isEmpty(filter.getIds())) {
            return Collections.emptyList();
        }
        long bizId = parseRootResourceBizId(filter);
        Set<String> wanted = new HashSet<>(filter.getIds());
        List<DynamicGroupDTO> groups = bizDynamicGroupService.listDynamicGroup(
            JobContextUtil.getTenantId(), bizId);

        boolean needPath = isPathRequired(request);
        String path = needPath
            ? buildIamPath(IamV4ResourceTypeId.BIZ_DYNAMIC_GROUP, String.valueOf(bizId))
            : null;
        List<V4InstanceInfoDTO> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(groups)) {
            for (DynamicGroupDTO group : groups) {
                if (group.getId() == null || !wanted.contains(group.getId())) {
                    continue;
                }
                V4InstanceInfoDTO instance = new V4InstanceInfoDTO();
                instance.setId(group.getId());
                instance.setDisplayName(group.getName());
                instance.setBkIamPath(path);
                result.add(instance);
            }
        }
        if (result.size() < wanted.size()) {
            log.warn("Some dynamic groups not found in IAM v4 callback, may be deleted"
                + "|bizId={}|requested={}|found={}", bizId, wanted.size(), result.size());
        }
        return result;
    }

    /**
     * 动态分组的上级恒为业务（动态分组视图首段 {@code biz_dynamic_group}），解析出 CMDB 侧 bizId。
     */
    private long parseParentBizId(V4CallbackParentDTO parent) {
        if (parent == null || StringUtils.isBlank(parent.getType()) || StringUtils.isBlank(parent.getId())) {
            throw new IllegalArgumentException("Parent biz is required for listing dynamic group instances");
        }
        ResourceScope scope = toResourceScope(parent);
        try {
            return Long.parseLong(scope.getId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal parent biz id: " + parent.getId());
        }
    }
}
