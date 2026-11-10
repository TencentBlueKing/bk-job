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

package com.tencent.bk.job.file_gateway.api.iam.v4.impl;

import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.common.iam.util.IamV4RespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.file_gateway.api.iam.v4.IamV4FileSourceCallbackResource;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBasicInfoDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackFilterDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4BaseInstanceDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4CallbackResp;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4InstanceInfoDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4ListInstanceDataDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件源资源的 IAM v4 回调实现。文件源实例 ID 为 int，单独解析。
 */
@Slf4j
@RestController
public class IamV4FileSourceCallbackResourceImpl extends BaseIamV4CallbackService
    implements IamV4FileSourceCallbackResource {

    private final FileSourceService fileSourceService;

    @Autowired
    public IamV4FileSourceCallbackResourceImpl(FileSourceService fileSourceService,
                                               AppScopeMappingService appScopeMappingService,
                                               TenantEnvService tenantEnvService,
                                               IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.fileSourceService = fileSourceService;
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.FILE_SOURCE;
    }

    @Override
    public ResponseEntity<V4CallbackResp<?>> callback(String tenantId,
                                                      String authorization,
                                                      String requestId,
                                                      V4CallbackRequestDTO callbackRequest) {
        return baseCallback(tenantId, authorization, requestId, callbackRequest);
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        long appId = requireAppIdByParent(filter == null ? null : filter.getParent());
        String alias = filter == null ? null : filter.getKeyword();
        BaseSearchCondition condition = buildSearchCondition(request.getPage());

        List<FileSourceDTO> fileSourceList = fileSourceService.listWorkTableFileSource(
            appId, null, alias, condition.getStart(), condition.getLength());
        long total = fileSourceService.countWorkTableFileSource(appId, null, alias).longValue();

        PageData<FileSourceDTO> pageData = new PageData<>(
            condition.getStart(), condition.getLength(), total, fileSourceList);
        return IamV4RespUtil.buildListInstanceData(pageData,
            fileSource -> new V4BaseInstanceDTO(String.valueOf(fileSource.getId()), fileSource.getAlias()));
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        if (filter == null || CollectionUtils.isEmpty(filter.getIds())) {
            return Collections.emptyList();
        }
        List<Integer> ids = parseIntIds(filter.getIds());
        List<FileSourceBasicInfoDTO> fileSourceList = fileSourceService.listFileSourceByIds(ids);

        Map<Integer, FileSourceBasicInfoDTO> fileSourceMap = new HashMap<>(fileSourceList.size());
        Set<Long> appIdSet = new HashSet<>();
        for (FileSourceBasicInfoDTO fileSource : fileSourceList) {
            fileSourceMap.put(fileSource.getId(), fileSource);
            appIdSet.add(fileSource.getAppId());
        }
        boolean needPath = isPathRequired(request);
        Map<Long, ResourceScope> appIdScopeMap = needPath
            ? appScopeMappingService.getScopeByAppIds(appIdSet) : Collections.emptyMap();

        List<V4InstanceInfoDTO> result = new ArrayList<>();
        for (Integer id : ids) {
            FileSourceBasicInfoDTO fileSource = fileSourceMap.get(id);
            if (fileSource == null) {
                log.warn("Cannot find file source by id {}, may be deleted", id);
                continue;
            }
            V4InstanceInfoDTO instance = new V4InstanceInfoDTO();
            instance.setId(String.valueOf(fileSource.getId()));
            instance.setDisplayName(fileSource.getAlias());
            if (needPath) {
                instance.setBkIamPath(buildScopePaths(appIdScopeMap.get(fileSource.getAppId())));
            }
            result.add(instance);
        }
        return result;
    }

    private List<Integer> parseIntIds(List<String> ids) {
        List<Integer> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            try {
                result.add(Integer.parseInt(id));
            } catch (NumberFormatException e) {
                log.warn("Illegal file source id in IAM v4 callback, skip|id={}", id);
            }
        }
        return result;
    }
}
