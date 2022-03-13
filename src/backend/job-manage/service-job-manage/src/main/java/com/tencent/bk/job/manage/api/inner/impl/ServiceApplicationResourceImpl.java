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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.api.inner.ServiceApplicationResource;
import com.tencent.bk.job.manage.model.inner.ServiceAppBaseInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceApplicationResourceImpl implements ServiceApplicationResource {
    private final ApplicationService applicationService;

    @Autowired
    public ServiceApplicationResourceImpl(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public InternalResponse<List<ServiceAppBaseInfoDTO>> listNormalApps() {
        List<ApplicationDTO> appList = applicationService.listAllApps();
        List<ServiceAppBaseInfoDTO> resultList =
            appList.parallelStream().filter(app -> app.getAppType() == AppTypeEnum.NORMAL)
                .map(this::convertToServiceAppBaseInfo).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(resultList);
    }

    @Override
    public InternalResponse<List<ServiceApplicationDTO>> listBizSetApps() {
        AppTypeEnum[] appTypeEnums = AppTypeEnum.values();
        List<ApplicationDTO> applicationInfoDTOList = new ArrayList<>();
        for (AppTypeEnum appTypeEnum : appTypeEnums) {
            if (appTypeEnum != AppTypeEnum.NORMAL) {
                applicationInfoDTOList.addAll(applicationService.listAppsByType(appTypeEnum));
            }
        }
        List<ServiceApplicationDTO> resultList =
            applicationInfoDTOList.parallelStream().map(this::convertToServiceApp).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(resultList);
    }

    @Override
    public ServiceApplicationDTO queryAppById(Long appId) {
        ApplicationDTO appInfo = applicationService.getAppByAppId(appId);
        if (appInfo == null) {
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
        return convertToServiceApp(appInfo);
    }

    private ServiceApplicationDTO convertToServiceApp(ApplicationDTO appInfo) {
        if (appInfo == null) {
            return null;
        }
        ServiceApplicationDTO app = new ServiceApplicationDTO();
        app.setId(appInfo.getId());
        app.setScopeType(appInfo.getScope().getType().getValue());
        app.setScopeId(appInfo.getScope().getId());
        app.setName(appInfo.getName());
        app.setAppType(appInfo.getAppType().getValue());
        app.setSubAppIds(appInfo.getSubAppIds());
        app.setMaintainers(appInfo.getMaintainers());
        app.setOwner(appInfo.getBkSupplierAccount());
        app.setOperateDeptId(appInfo.getOperateDeptId());
        app.setTimeZone(appInfo.getTimeZone());
        app.setLanguage(appInfo.getLanguage());
        return app;
    }

    private ServiceAppBaseInfoDTO convertToServiceAppBaseInfo(ApplicationDTO appInfo) {
        ServiceAppBaseInfoDTO appBaseInfoDTO = new ServiceAppBaseInfoDTO();
        appBaseInfoDTO.setScopeType(appInfo.getScope().getType().getValue());
        appBaseInfoDTO.setScopeId(appInfo.getScope().getId());
        appBaseInfoDTO.setAppId(appInfo.getId());
        appBaseInfoDTO.setName(appInfo.getName());
        return appBaseInfoDTO;
    }

    @Override
    public ServiceApplicationDTO queryAppByScope(String scopeType, String scopeId) {
        ApplicationDTO appInfo = applicationService.getAppByScope(new ResourceScope(scopeType, scopeId));
        if (appInfo == null) {
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
        return convertToServiceApp(appInfo);
    }

    @Override
    public List<ServiceApplicationDTO> listAppsByAppIds(String appIds) {
        Set<Long> appIdList = Arrays.stream(appIds.split(","))
            .map(Long::parseLong).collect(Collectors.toSet());
        List<ApplicationDTO> applications = applicationService.listAppsByAppIds(appIdList);
        if (CollectionUtils.isEmpty(applications)) {
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
        return applications.stream().map(this::convertToServiceApp).collect(Collectors.toList());
    }

    @Override
    public InternalResponse<Boolean> checkAppPermission(Long appId, String username) {
        if (appId == null || appId < 0) {
            return InternalResponse.buildSuccessResp(false);
        }
        return InternalResponse.buildSuccessResp(applicationService.checkAppPermission(appId, username));
    }

    @Override
    public InternalResponse<List<ServiceApplicationDTO>> listApps(Integer appType, String scopeType) {
        List<ApplicationDTO> appList = applicationService.listAllApps();
        if (CollectionUtils.isEmpty(appList)) {
            InternalResponse.buildSuccessResp(appList);
        }

        if (appType != null) {
            appList = appList.stream().filter(app -> app.getAppType() == AppTypeEnum.valueOf(appType))
                .collect(Collectors.toList());
        } else if (StringUtils.isNotEmpty(scopeType)) {
            appList = appList.stream().filter(app -> app.getScope().getType() == ResourceScopeTypeEnum.from(scopeType))
                .collect(Collectors.toList());
        }
        List<ServiceApplicationDTO> resultList =
            appList.stream().map(this::convertToServiceApp).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(resultList);
    }
}
