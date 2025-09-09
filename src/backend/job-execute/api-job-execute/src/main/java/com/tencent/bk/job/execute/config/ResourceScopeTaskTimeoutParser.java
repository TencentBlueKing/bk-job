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

package com.tencent.bk.job.execute.config;

import com.tencent.bk.job.common.service.AppScopeMappingService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ResourceScopeTaskTimeoutParser {

    private final Map<Long, Integer> appTimeoutMap;
    private final AppScopeMappingService appScopeMappingService;

    public ResourceScopeTaskTimeoutParser(ResourceScopeTaskTimeoutProperties resourceScopeTaskTimeoutProperties,
                                          AppScopeMappingService appScopeMappingService) {
        this.appScopeMappingService = appScopeMappingService;
        this.appTimeoutMap = new HashMap<>();
        resourceScopeTaskTimeoutProperties.getCustom().forEach(scopeTimeout -> {
            String scopeType = scopeTimeout.getScopeType();
            String scopeId = scopeTimeout.getScopeId();
            Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
            int maxTimeout = scopeTimeout.getMaxTimeout();
            appTimeoutMap.put(appId, maxTimeout);
            logInitiateCustomTimeout(scopeType, scopeId, appId, maxTimeout);
        });

    }

    /**
     * 根据资源获取最大超时时间
     *
     * @param scopeType 资源类型
     * @param scopeId 资源ID
     * @param defaultMaxTimeout 平台默认最大超时时间
     * @return 最大超时时间
     */
    public int getMaxTimeoutOrDefault(String scopeType, String scopeId, int defaultMaxTimeout) {
        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
        return appTimeoutMap.getOrDefault(appId, defaultMaxTimeout);
    }

    /**
     * 根据appID获取最大超时时间
     *
     * @param appId appID
     * @param defaultMaxTimeout 平台默认最大超时时间
     * @return 最大超时时间
     */
    public int getMaxTimeoutOrDefault(Long appId, int defaultMaxTimeout) {
        return appTimeoutMap.getOrDefault(appId, defaultMaxTimeout);
    }

    private void logInitiateCustomTimeout(String scopeType, String scopeId, Long appId, int maxTimeout) {
        log.info(
            "Initiate custom timeout for scopeType={}, scopeId={}, appId={}, maxTimeout={}",
            scopeType,
            scopeId,
            appId,
            maxTimeout);
    }

}
