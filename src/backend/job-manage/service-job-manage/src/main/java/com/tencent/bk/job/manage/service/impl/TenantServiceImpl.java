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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.paas.user.IUserApiClient;
import com.tencent.bk.job.manage.model.inner.resp.TenantDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TenantServiceImpl implements TenantService {

    private final IUserApiClient userMgrApiClient;
    private final ApplicationService applicationService;

    @Autowired
    public TenantServiceImpl(IUserApiClient userMgrApiClient,
                             // 此处存在循环依赖，暂时用Lazy打破
                             @Lazy ApplicationService applicationService) {
        this.userMgrApiClient = userMgrApiClient;
        this.applicationService = applicationService;
    }


    @Override
    public List<TenantDTO> listEnabledTenant() {
        List<OpenApiTenant> openApiTenantList = userMgrApiClient.listAllTenant();
        if (CollectionUtils.isEmpty(openApiTenantList)) {
            return Collections.emptyList();
        }
        return openApiTenantList.stream()
            .filter(OpenApiTenant::isEnabled)
            .map(openApiTenant -> new TenantDTO(openApiTenant.getId(), openApiTenant.getName()))
            .collect(Collectors.toList());
    }

    @Override
    public String getTenantIdByAppId(long appId) {
        String tenantId = applicationService.getTenantIdByAppId(appId);
        if (tenantId == null) {
            log.warn("Cannot get tenant id by appId:{}", appId);
        }
        return tenantId;
    }
}
