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

package com.tencent.bk.job.common.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;

/**
 * 业务与资源范围映射公共接口
 */
public interface AppScopeMappingService {
    Long getAppIdByScope(ResourceScope resourceScope);

    Long getAppIdByScope(String scopeType, String scopeId);

    ResourceScope getScopeByAppId(Long appId);

    Map<Long, ResourceScope> getScopeByAppIds(Collection<Long> appIds);

    Map<ResourceScope, Long> getAppIdByScopeList(Collection<ResourceScope> scopeList);

    /**
     * 根据业务ID或者scopeType&scopeId获取AppResourceScope
     *
     * @param appId     业务ID
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @return AppResourceScope, 包含业务ID、资源范围类型、资源范围ID
     */
    AppResourceScope getAppResourceScope(Long appId, String scopeType, String scopeId);

    /**
     * 根据scopeType&scopeId获取AppResourceScope
     *
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @return AppResourceScope, 包含业务ID、资源范围类型、资源范围ID
     */
    AppResourceScope getAppResourceScope(String scopeType, String scopeId);

    default void fillAppResourceScope(AppResourceScope appResourceScope) {
        if (appResourceScope.getType() != null && StringUtils.isNotBlank(appResourceScope.getId())) {
            if (appResourceScope.getAppId() == null) {
                appResourceScope.setAppId(
                    getAppIdByScope(appResourceScope.getType().getValue(), appResourceScope.getId())
                );
            }
            return;
        }
        if (appResourceScope.getAppId() != null) {
            ResourceScope scope = getScopeByAppId(appResourceScope.getAppId());
            appResourceScope.setType(scope.getType());
            appResourceScope.setId(scope.getId());
            return;
        }
        throw new InternalException("Invalid AppResourceScope", ErrorCode.INTERNAL_ERROR);
    }
}
