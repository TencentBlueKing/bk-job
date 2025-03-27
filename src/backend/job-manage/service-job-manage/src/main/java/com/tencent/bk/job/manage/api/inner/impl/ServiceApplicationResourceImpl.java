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

import com.tencent.bk.job.common.cc.model.bizset.BizSetInfo;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.api.inner.ServiceApplicationResource;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationAttrsDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceListAppByAppIdListReq;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceApplicationResourceImpl implements ServiceApplicationResource {
    private final ApplicationService applicationService;
    private final ApplicationDAO applicationDAO;
    private final IBizCmdbClient bizCmdbClient;
    private final IBizSetCmdbClient bizSetCmdbClient;

    @Autowired
    public ServiceApplicationResourceImpl(ApplicationService applicationService,
                                          ApplicationDAO applicationDAO,
                                          IBizCmdbClient bizCmdbClient,
                                          IBizSetCmdbClient bizSetCmdbClient) {
        this.applicationService = applicationService;
        this.applicationDAO = applicationDAO;
        this.bizCmdbClient = bizCmdbClient;
        this.bizSetCmdbClient = bizSetCmdbClient;
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
        app.setTimeZone(appInfo.getTimeZone());
        app.setLanguage(appInfo.getLanguage());
        if (appInfo.getAttrs() != null) {
            ServiceApplicationAttrsDTO attrs = new ServiceApplicationAttrsDTO();
            attrs.setMatchAllBiz(appInfo.getAttrs().getMatchAllBiz());
            attrs.setSubBizIds(appInfo.getAttrs().getSubBizIds());
            app.setAttrs(attrs);
        }
        app.setTenantId(appInfo.getTenantId());
        return app;
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
    public List<ServiceApplicationDTO> listAppsByAppIdList(ServiceListAppByAppIdListReq req) {
        List<Long> appIdList = req.getAppIdList();
        List<ApplicationDTO> applications = applicationService.listAppsByAppIds(appIdList);
        return applications.stream().map(this::convertToServiceApp).collect(Collectors.toList());
    }

    @Override
    public InternalResponse<List<ServiceApplicationDTO>> listApps(String scopeType) {
        List<ApplicationDTO> appList = applicationService.listAllApps();
        if (CollectionUtils.isEmpty(appList)) {
            InternalResponse.buildSuccessResp(appList);
        }

        if (scopeType != null) {
            appList = appList.stream()
                .filter(app -> app.getScope().getType() == ResourceScopeTypeEnum.from(scopeType))
                .collect(Collectors.toList());
        }

        List<ServiceApplicationDTO> resultList =
            appList.stream().map(this::convertToServiceApp).collect(Collectors.toList());
        return InternalResponse.buildSuccessResp(resultList);
    }

    @Override
    public InternalResponse<List<Long>> listAllAppIdOfArchivedScope() {
        List<ApplicationDTO> loaclAllDeletedApps = applicationService.listAllDeletedApps();
        log.debug("find archived app from local, size={}", loaclAllDeletedApps.size());

        Map<String, List<ApplicationDTO>> tenantBizApps = loaclAllDeletedApps.stream()
            .filter(app -> ResourceScopeTypeEnum.BIZ.getValue().equals(app.getScope().getType().getValue()))
            .collect(Collectors.groupingBy(ApplicationDTO::getTenantId));
        Map<String, List<ApplicationDTO>> tenantBizSetApps = loaclAllDeletedApps.stream()
            .filter(app -> ResourceScopeTypeEnum.BIZ_SET.getValue().equals(app.getScope().getType().getValue()))
            .collect(Collectors.groupingBy(ApplicationDTO::getTenantId));
        List<Long> archivedIds = new ArrayList<>();

        // 逐个租户筛选业务
        for (Map.Entry<String, List<ApplicationDTO>> entry : tenantBizApps.entrySet()) {
            String tenantId = entry.getKey();
            List<ApplicationDTO> bizApps = entry.getValue();
            List<Long> bizIds = bizApps.stream()
                .map(bizApp -> Long.valueOf(bizApp.getScope().getId()))
                .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bizApps)) {
                List<ApplicationDTO> ccAllBizApps = bizCmdbClient.listBizAppByIds(tenantId, bizIds);
                Set<String> ccBizAppScopeIds = ccAllBizApps.stream()
                    .map(ccBizApp -> ccBizApp.getScope().getId())
                    .collect(Collectors.toSet());
                archivedIds.addAll(bizApps.stream().filter(bizAppInfoDTO ->
                        !ccBizAppScopeIds.contains(bizAppInfoDTO.getScope().getId()))
                    .map(ApplicationDTO::getId)
                    .collect(Collectors.toList()));
            }
        }

        // 逐个租户筛选业务集
        for (Map.Entry<String, List<ApplicationDTO>> entry : tenantBizSetApps.entrySet()) {
            String tenantId = entry.getKey();
            List<ApplicationDTO> bizSetApps = entry.getValue();
            List<Long> bizSetIds = bizSetApps.stream()
                .map(bizSetApp -> Long.valueOf(bizSetApp.getScope().getId()))
                .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bizSetApps)) {
                List<BizSetInfo> bizSetInfos = bizSetCmdbClient.listBizSetByIds(tenantId, bizSetIds);
                Set<String> ccBizSetAppScopeIds = bizSetInfos.stream()
                    .map(ccBizSetApp -> String.valueOf(ccBizSetApp.getId()))
                    .collect(Collectors.toSet());
                archivedIds.addAll(bizSetApps.stream().filter(bizAppInfoDTO ->
                        !ccBizSetAppScopeIds.contains(bizAppInfoDTO.getScope().getId()))
                    .map(ApplicationDTO::getId)
                    .collect(Collectors.toList()));
            }
        }
        log.debug("finally find archived appIds={}", archivedIds);
        return InternalResponse.buildSuccessResp(archivedIds);
    }

    @Override
    public InternalResponse<Boolean> existsAppById(Long appId) {
        try {
            ApplicationDTO appInfo = applicationService.getAppByAppId(appId);
            return InternalResponse.buildSuccessResp(appInfo != null);
        } catch (NotFoundException e) {
            log.info("biz/bizSet not exist, appId={}", appId);
            return InternalResponse.buildSuccessResp(false);
        }
    }

    public InternalResponse<List<Long>> listAppIdByTenant(String tenantId) {
        return InternalResponse.buildSuccessResp(applicationDAO.listAppIdByTenant(tenantId));
    }

    public InternalResponse<List<ServiceApplicationDTO>> listAppByTenant(String tenantId) {
        return InternalResponse.buildSuccessResp(
            applicationDAO.listAllAppsForTenant(tenantId)
                .stream()
                .map(ServiceApplicationDTO::fromApplicationDTO)
                .collect(Collectors.toList())
        );
    }
}
