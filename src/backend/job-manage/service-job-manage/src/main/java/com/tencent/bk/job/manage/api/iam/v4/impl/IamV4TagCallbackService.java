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
import com.tencent.bk.job.common.iam.util.IamV4RespUtil;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.TagService;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 标签资源的 IAM v4 回调实现。
 */
@Slf4j
@Component
public class IamV4TagCallbackService extends BaseIamV4CallbackService {

    private final TagService tagService;
    private final ApplicationService applicationService;

    @Autowired
    public IamV4TagCallbackService(TagService tagService,
                                        ApplicationService applicationService,
                                        AppScopeMappingService appScopeMappingService,
                                        TenantEnvService tenantEnvService,
                                        IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.tagService = tagService;
        this.applicationService = applicationService;
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.TAG;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();

        TagDTO tagQuery = new TagDTO();
        tagQuery.setAppId(requireAppIdByParent(filter == null ? null : filter.getParent()));
        if (filter != null && StringUtils.isNotBlank(filter.getKeyword())) {
            tagQuery.setName(filter.getKeyword());
        }

        PageData<TagDTO> pageData = tagService.listPageTags(tagQuery, buildSearchCondition(request.getPage()));
        return IamV4RespUtil.buildListInstanceData(pageData,
            tag -> new V4BaseInstanceDTO(String.valueOf(tag.getId()), tag.getName()));
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        if (filter == null || CollectionUtils.isEmpty(filter.getIds())) {
            return Collections.emptyList();
        }
        List<Long> ids = parseLongIds(filter.getIds());
        List<TagDTO> tagList = tagService.listTagInfoByIds(ids);

        Map<Long, TagDTO> tagMap = new HashMap<>(tagList.size());
        Set<Long> appIdSet = new HashSet<>();
        for (TagDTO tag : tagList) {
            tagMap.put(tag.getId(), tag);
            appIdSet.add(tag.getAppId());
        }
        boolean needPath = isPathRequired(request);
        Map<Long, ResourceScope> appIdScopeMap = needPath
            ? applicationService.getScopeByAppIds(appIdSet) : Collections.emptyMap();

        List<V4InstanceInfoDTO> result = new ArrayList<>();
        for (Long id : ids) {
            TagDTO tag = tagMap.get(id);
            if (tag == null) {
                log.warn("Cannot find tag by id {}, may be deleted", id);
                continue;
            }
            V4InstanceInfoDTO instance = new V4InstanceInfoDTO();
            instance.setId(String.valueOf(tag.getId()));
            instance.setDisplayName(tag.getName());
            if (needPath) {
                instance.setBkIamPath(buildScopePaths(appIdScopeMap.get(tag.getAppId())));
            }
            result.add(instance);
        }
        return result;
    }
}
