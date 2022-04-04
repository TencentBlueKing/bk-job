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

package com.tencent.bk.job.crontab.auth;

import com.tencent.bk.job.common.app.ApplicationUtil;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.crontab.client.ServiceApplicationResourceClient;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.service.CronJobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CrontabResourceAppInfoQueryService implements ResourceAppInfoQueryService {

    private final ServiceApplicationResourceClient applicationService;
    private final CronJobService cronJobService;

    @Autowired
    public CrontabResourceAppInfoQueryService(ServiceApplicationResourceClient applicationService,
                                              CronJobService cronJobService) {
        this.applicationService = applicationService;
        this.cronJobService = cronJobService;
    }

    private ResourceAppInfo getResourceAppInfoByScope(String scopeType, String scopeId) {
        if (StringUtils.isBlank(scopeType) || StringUtils.isBlank(scopeId)) {
            log.warn("scope({},{}) is invalid", scopeType, scopeId);
            return null;
        }
        return ApplicationUtil.convertToResourceApp(applicationService.queryAppByScope(scopeType, scopeId));
    }

    private ResourceAppInfo getResourceAppInfoById(Long appId) {
        if (appId == null || appId <= 0) {
            log.warn("appId({}) is invalid", appId);
            return null;
        }
        return ApplicationUtil.convertToResourceApp(applicationService.queryAppById(appId));
    }

    private ResourceAppInfo getCronApp(String resourceId) {
        long cronId = Long.parseLong(resourceId);
        if (cronId <= 0) {
            log.warn("cronId({}) is invalid", resourceId);
            return null;
        }
        CronJobInfoDTO cronJobInfoDTO = cronJobService.getCronJobInfoById(cronId);
        if (cronJobInfoDTO == null) {
            log.warn("Cannot find cron by resourceId:{}", resourceId);
            return null;
        }
        Long appId = cronJobInfoDTO.getAppId();
        if (appId <= 0) {
            log.warn("appId({}) of cron {} is invalid", appId, resourceId);
            return null;
        }
        return getResourceAppInfoById(appId);
    }

    @Override
    public ResourceAppInfo getResourceAppInfo(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case BUSINESS:
                return getResourceAppInfoByScope(ResourceScopeTypeEnum.BIZ.getValue(), resourceId);
            case BUSINESS_SET:
                return getResourceAppInfoByScope(ResourceScopeTypeEnum.BIZ_SET.getValue(), resourceId);
            case CRON:
                return getCronApp(resourceId);
            default:
                log.warn("Not support resourceType:{}, return null", resourceType);
                return null;
        }
    }
}
