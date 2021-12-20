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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.service.CommonApplicationService;
import com.tencent.bk.job.execute.client.ApplicationResourceClient;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @since 9/3/2020 10:10
 */
@Slf4j
@Service
public class CommonApplicationServiceImpl implements CommonApplicationService {

    @Autowired
    private ApplicationResourceClient applicationResourceClient;

    @Override
    public Boolean checkAppPermission(long appId, String username) {
        log.debug("Checking permission for app {} user {}", appId, username);
        if (appId < 0 || StringUtils.isBlank(username)) {
            return false;
        }
        InternalResponse<Boolean> checkResp = applicationResourceClient.checkAppPermission(appId, username);
        if (checkResp != null) {
            if (log.isDebugEnabled()) {
                log.debug("Check app perm response|{}|{}|{}", appId, username, checkResp);
            }
            if (checkResp.getData() != null) {
                return checkResp.getData();
            }
        }
        return false;
    }

    @Override
    public ApplicationInfoDTO getAppInfo(long appId) {
        ServiceApplicationDTO serviceApplication = applicationResourceClient.queryAppById(appId);
        if (serviceApplication != null) {
            ApplicationInfoDTO applicationInfo = new ApplicationInfoDTO();
            applicationInfo.setId(serviceApplication.getId());
            applicationInfo.setName(serviceApplication.getName());
            applicationInfo.setAppType(AppTypeEnum.valueOf(serviceApplication.getAppType()));
            applicationInfo.setSubAppIds(serviceApplication.getSubAppIds());
            applicationInfo.setBkSupplierAccount(serviceApplication.getOwner());
            applicationInfo.setOperateDeptId(serviceApplication.getOperateDeptId());
            applicationInfo.setTimeZone(serviceApplication.getTimeZone());
            return applicationInfo;
        } else {
            return null;
        }
    }
}
