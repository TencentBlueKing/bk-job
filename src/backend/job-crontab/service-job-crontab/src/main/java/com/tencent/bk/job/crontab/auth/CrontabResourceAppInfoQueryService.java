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

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.crontab.client.ServiceApplicationResourceClient;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private ResourceAppInfo convert(ServiceApplicationDTO applicationInfoDTO) {
        if (applicationInfoDTO == null) {
            return null;
        } else {
            ResourceAppInfo resourceAppInfo = new ResourceAppInfo();
            resourceAppInfo.setAppId(applicationInfoDTO.getId());
            resourceAppInfo.setAppType(AppTypeEnum.valueOf(applicationInfoDTO.getAppType()));
            String maintainerStr = applicationInfoDTO.getMaintainers();
            List<String> maintainerList = new ArrayList<>();
            if (StringUtils.isNotBlank(maintainerStr)) {
                String[] maintainers = maintainerStr.split("[,;]");
                maintainerList.addAll(Arrays.asList(maintainers));
            }
            resourceAppInfo.setMaintainerList(maintainerList);
            return resourceAppInfo;
        }
    }

    private ResourceAppInfo getResourceAppInfoById(Long appId) {
        if (appId == null || appId <= 0) {
            return null;
        }
        return convert(applicationService.queryAppById(appId));
    }

    @Override
    public ResourceAppInfo getResourceAppInfo(ResourceTypeEnum resourceType, String resourceId) {
        switch (resourceType) {
            case BUSINESS:
                long appId = Long.parseLong(resourceId);
                if (appId > 0) {
                    return getResourceAppInfoById(appId);
                }
                break;
            case CRON:
                long cronId = Long.parseLong(resourceId);
                if (cronId > 0) {
                    CronJobInfoDTO cronJobInfoDTO = cronJobService.getCronJobInfoById(cronId);
                    if (cronJobInfoDTO != null) {
                        return getResourceAppInfoById(cronJobInfoDTO.getAppId());
                    }
                }
                break;
            default:
                return null;
        }
        return null;
    }
}
