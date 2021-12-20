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
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
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
    public InternalResponse<List<ServiceHostInfoDTO>> getHostByAppId(Long appId) {
        try {
            List<ApplicationHostInfoDTO> hosts = applicationHostService.getHostsByAppId(appId);
            List<ServiceHostInfoDTO> serviceHosts = new ArrayList<>();
            if (hosts != null) {
                serviceHosts =
                    hosts.stream().map(host -> convertToServiceHostInfo(appId, host)).collect(Collectors.toList());
            }
            return InternalResponse.buildSuccessResp(serviceHosts);
        } catch (Exception e) {
            log.warn("Get host by appId exception", e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public InternalResponse<Boolean> syncHostByAppId(Long appId) {
        return InternalResponse.buildSuccessResp(syncService.syncAppHosts(appId));
    }

    @Override
    public InternalResponse<Boolean> enableAppWatch() {
        return InternalResponse.buildSuccessResp(syncService.enableAppWatch());
    }

    @Override
    public InternalResponse<Boolean> disableAppWatch() {
        return InternalResponse.buildSuccessResp(syncService.disableAppWatch());
    }

    @Override
    public InternalResponse<Boolean> enableHostWatch() {
        return InternalResponse.buildSuccessResp(syncService.enableHostWatch());
    }

    @Override
    public InternalResponse<Boolean> disableHostWatch() {
        return InternalResponse.buildSuccessResp(syncService.disableHostWatch());
    }

    @Override
    public InternalResponse<Boolean> enableSyncApp() {
        return InternalResponse.buildSuccessResp(syncService.enableSyncApp());
    }

    @Override
    public InternalResponse<Boolean> disableSyncApp() {
        return InternalResponse.buildSuccessResp(syncService.disableSyncApp());
    }

    @Override
    public InternalResponse<Boolean> enableSyncHost() {
        return InternalResponse.buildSuccessResp(syncService.enableSyncHost());
    }

    @Override
    public InternalResponse<Boolean> disableSyncHost() {
        return InternalResponse.buildSuccessResp(syncService.disableSyncHost());
    }

    @Override
    public InternalResponse<Boolean> enableSyncAgentStatus() {
        return InternalResponse.buildSuccessResp(syncService.enableSyncAgentStatus());
    }

    @Override
    public InternalResponse<Boolean> disableSyncAgentStatus() {
        return InternalResponse.buildSuccessResp(syncService.disableSyncAgentStatus());
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
