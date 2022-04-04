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

package com.tencent.bk.job.common.app;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.iam.model.ResourceAppInfo;
import com.tencent.bk.job.common.model.ServiceApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApplicationUtil {

    /**
     * 将服务间调用的业务对象转为通用Job业务对象
     *
     * @param app 服务间调用业务对象
     * @return 通用Job业务对象
     */
    public static ApplicationDTO convertToApplicationInfoDTO(ServiceApplicationDTO app) {
        ApplicationDTO applicationInfo = new ApplicationDTO();
        applicationInfo.setId(app.getId());
        applicationInfo.setName(app.getName());
        applicationInfo.setAppType(AppTypeEnum.valueOf(app.getAppType()));
        applicationInfo.setScope(new ResourceScope(app.getScopeType(), app.getScopeId()));
        applicationInfo.setSubAppIds(app.getSubAppIds());
        applicationInfo.setMaintainers(app.getMaintainers());
        applicationInfo.setOperateDeptId(app.getOperateDeptId());
        applicationInfo.setLanguage(app.getLanguage());
        return applicationInfo;
    }

    /**
     * 将通用Job业务对象转为服务间调用的业务对象
     *
     * @param appInfo 通用Job业务对象
     * @return 服务间调用业务对象
     */
    public static ServiceApplicationDTO convertToServiceApp(ApplicationDTO appInfo) {
        if (appInfo == null) {
            return null;
        }
        ServiceApplicationDTO app = new ServiceApplicationDTO();
        app.setId(appInfo.getId());
        app.setSubAppIds(appInfo.getSubAppIds());
        app.setName(appInfo.getName());
        app.setAppType(appInfo.getAppType().getValue());
        app.setScopeType(appInfo.getScope().getType().getValue());
        app.setScopeId(appInfo.getScope().getId());
        app.setOwner(appInfo.getBkSupplierAccount());
        app.setMaintainers(appInfo.getMaintainers());
        app.setOperateDeptId(appInfo.getOperateDeptId());
        app.setTimeZone(appInfo.getTimeZone());
        return app;
    }

    /**
     * 将Job业务对象转换为鉴权需要的业务资源对象
     *
     * @param applicationInfoDTO 业务对象
     * @return 鉴权需要的业务资源对象
     */
    public static ResourceAppInfo convertToResourceApp(ServiceApplicationDTO applicationInfoDTO) {
        if (applicationInfoDTO == null) {
            return null;
        } else {
            ResourceAppInfo resourceAppInfo = new ResourceAppInfo();
            resourceAppInfo.setAppId(applicationInfoDTO.getId());
            resourceAppInfo.setAppType(AppTypeEnum.valueOf(applicationInfoDTO.getAppType()));
            resourceAppInfo.setScopeType(applicationInfoDTO.getScopeType());
            resourceAppInfo.setScopeId(applicationInfoDTO.getScopeId());
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

    /**
     * 将Job通用业务对象转换为鉴权需要的业务资源对象
     *
     * @param appInfo Job通用业务对象
     * @return 鉴权需要的业务资源对象
     */
    public static ResourceAppInfo convertToResourceApp(ApplicationDTO appInfo) {
        return convertToResourceApp(convertToServiceApp(appInfo));
    }
}
