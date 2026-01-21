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

package com.tencent.bk.job.common.paas.user;

import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 进行了数据安全过滤的用户管理 API 客户端，用于过滤预期外的数据
 */
@Slf4j
public class SafeUserMgrApiClient implements IUserApiClient {

    @Delegate
    private final IUserApiClient delegate;

    private final TenantEnvService tenantEnvService;

    public SafeUserMgrApiClient(IUserApiClient delegate,
                                TenantEnvService tenantEnvService) {
        this.delegate = delegate;
        this.tenantEnvService = tenantEnvService;
    }

    /**
     * 获取全量租户
     */
    @Override
    public List<OpenApiTenant> listAllTenant() {
        List<OpenApiTenant> tenantList = delegate.listAllTenant();
        if (tenantEnvService.isTenantEnabled() || CollectionUtils.isEmpty(tenantList)) {
            return tenantList;
        }
        // 单租户模式下只应当有一个default租户，如果遇到其他非法租户数据，过滤并忽略
        List<OpenApiTenant> filteredTenantList = new ArrayList<>();
        for (OpenApiTenant tenant : tenantList) {
            if (!TenantIdConstants.DEFAULT_TENANT_ID.equals(tenant.getId())) {
                log.info("Unexpected tenant:{}, ignore", tenant);
                continue;
            }
            if (filteredTenantList.isEmpty()) {
                filteredTenantList.add(tenant);
            } else {
                log.info("More than one default tenant:{}, ignore", tenant);
            }
        }
        return filteredTenantList;
    }

}
