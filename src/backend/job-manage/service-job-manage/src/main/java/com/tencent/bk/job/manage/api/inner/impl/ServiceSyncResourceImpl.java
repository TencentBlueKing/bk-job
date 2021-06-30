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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.manage.api.inner.ServiceSyncResource;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.service.ApplicationHostService;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceSyncResourceImpl implements ServiceSyncResource {
    private final ApplicationService applicationService;
    private final ApplicationHostService applicationHostService;
    private final SyncService syncService;


    @Autowired
    public ServiceSyncResourceImpl(ApplicationService applicationService,
                                   ApplicationHostService applicationHostService, SyncService syncService) {
        this.applicationService = applicationService;
        this.applicationHostService = applicationHostService;
        this.syncService = syncService;
    }

    @Override
    public List<ServiceApplicationDTO> listAllApps() {
        List<ApplicationInfoDTO> apps = applicationService.listAllAppsFromLocalDB();
        if (apps == null) {
            return null;
        }

        return apps.stream().map(this::convertToServiceApp).collect(Collectors.toList());
    }

    private ServiceApplicationDTO convertToServiceApp(ApplicationInfoDTO appInfo) {
        ServiceApplicationDTO app = new ServiceApplicationDTO();
        app.setName(appInfo.getName());
        app.setId(appInfo.getId());
        app.setOwner(appInfo.getBkSupplierAccount());
        app.setAppType(appInfo.getAppType().getValue());
        app.setSubAppIds(appInfo.getSubAppIds());
        app.setOperateDeptId(appInfo.getOperateDeptId());
        app.setTimeZone(appInfo.getTimeZone());
        return app;
    }


    @Override
    public ServiceResponse<List<ServiceHostInfoDTO>> getHostByAppId(Long appId) {
        try {
            List<ApplicationHostInfoDTO> hosts = applicationHostService.getHostsByAppId(appId);
            List<ServiceHostInfoDTO> serviceHosts = new ArrayList<>();
            if (hosts != null) {
                serviceHosts =
                    hosts.stream().map(host -> convertToServiceHostInfo(appId, host)).collect(Collectors.toList());
            }
            return ServiceResponse.buildSuccessResp(serviceHosts);
        } catch (Exception e) {
            log.warn("Get host by appId exception", e);
            return ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR);
        }
    }

    @Override
    public ServiceResponse<Boolean> syncHostByAppId(Long appId) {
        return ServiceResponse.buildSuccessResp(syncService.syncAppHosts(appId));
    }

    @Override
    public ServiceResponse<Boolean> enableAppWatch() {
        return ServiceResponse.buildSuccessResp(syncService.enableAppWatch());
    }

    @Override
    public ServiceResponse<Boolean> disableAppWatch() {
        return ServiceResponse.buildSuccessResp(syncService.disableAppWatch());
    }

    @Override
    public ServiceResponse<Boolean> enableHostWatch() {
        return ServiceResponse.buildSuccessResp(syncService.enableHostWatch());
    }

    @Override
    public ServiceResponse<Boolean> disableHostWatch() {
        return ServiceResponse.buildSuccessResp(syncService.disableHostWatch());
    }

    @Override
    public ServiceResponse<Boolean> enableSyncApp() {
        return ServiceResponse.buildSuccessResp(syncService.enableSyncApp());
    }

    @Override
    public ServiceResponse<Boolean> disableSyncApp() {
        return ServiceResponse.buildSuccessResp(syncService.disableSyncApp());
    }

    @Override
    public ServiceResponse<Boolean> enableSyncHost() {
        return ServiceResponse.buildSuccessResp(syncService.enableSyncHost());
    }

    @Override
    public ServiceResponse<Boolean> disableSyncHost() {
        return ServiceResponse.buildSuccessResp(syncService.disableSyncHost());
    }

    @Override
    public ServiceResponse<Boolean> enableSyncAgentStatus() {
        return ServiceResponse.buildSuccessResp(syncService.enableSyncAgentStatus());
    }

    @Override
    public ServiceResponse<Boolean> disableSyncAgentStatus() {
        return ServiceResponse.buildSuccessResp(syncService.disableSyncAgentStatus());
    }

    private ServiceHostInfoDTO convertToServiceHostInfo(long appId, ApplicationHostInfoDTO hostInfo) {
        ServiceHostInfoDTO serviceHostInfo = new ServiceHostInfoDTO();
        serviceHostInfo.setAppId(appId);
        serviceHostInfo.setCloudAreaId(hostInfo.getCloudAreaId());
        serviceHostInfo.setIp(hostInfo.getIp());
        serviceHostInfo.setDisplayIp(hostInfo.getDisplayIp());
        serviceHostInfo.setHostId(hostInfo.getHostId());
        return serviceHostInfo;
    }
}
