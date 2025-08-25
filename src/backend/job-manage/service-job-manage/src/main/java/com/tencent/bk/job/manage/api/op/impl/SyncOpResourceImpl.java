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

package com.tencent.bk.job.manage.api.op.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.api.op.SyncOpResource;
import com.tencent.bk.job.manage.background.event.cmdb.CmdbEventManager;
import com.tencent.bk.job.manage.background.sync.AppSyncService;
import com.tencent.bk.job.manage.background.sync.BizHostSyncService;
import com.tencent.bk.job.manage.background.sync.TenantHostSyncService;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.service.SyncOpService;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class SyncOpResourceImpl implements SyncOpResource {
    private final NoTenantHostService noTenantHostService;
    private final SyncOpService syncOpService;
    private final AppSyncService appSyncService;
    private final BizHostSyncService bizHostSyncService;
    private final CmdbEventManager cmdbEventManager;
    private final TenantHostSyncService tenantHostSyncService;

    @Autowired
    public SyncOpResourceImpl(NoTenantHostService noTenantHostService,
                              SyncOpService syncOpService,
                              AppSyncService appSyncService,
                              BizHostSyncService bizHostSyncService,
                              CmdbEventManager cmdbEventManager,
                              TenantHostSyncService tenantHostSyncService) {
        this.noTenantHostService = noTenantHostService;
        this.syncOpService = syncOpService;
        this.appSyncService = appSyncService;
        this.bizHostSyncService = bizHostSyncService;
        this.cmdbEventManager = cmdbEventManager;
        this.tenantHostSyncService = tenantHostSyncService;
    }

    @Override
    public InternalResponse<List<ServiceHostInfoDTO>> getHostByAppId(Long appId) {
        try {
            List<ApplicationHostDTO> hosts = noTenantHostService.getHostsByAppId(appId);
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
    public InternalResponse<Boolean> syncApp() {
        return InternalResponse.buildSuccessResp(appSyncService.syncApp());
    }

    @Override
    public InternalResponse<Void> syncHost(String tenantId) {
        tenantHostSyncService.syncAllBizHostsAtOnce(tenantId);
        return InternalResponse.buildSuccessResp(null);
    }

    @Override
    public InternalResponse<Boolean> syncHostByBizId(Long bizId) {
        return InternalResponse.buildSuccessResp(
            bizHostSyncService.syncBizHosts(bizId)
        );
    }

    @Override
    public InternalResponse<Boolean> enableBizWatch() {
        return InternalResponse.buildSuccessResp(cmdbEventManager.enableBizWatch());
    }

    @Override
    public InternalResponse<Boolean> disableBizWatch() {
        return InternalResponse.buildSuccessResp(cmdbEventManager.disableBizWatch());
    }

    @Override
    public InternalResponse<Boolean> enableHostWatch() {
        return InternalResponse.buildSuccessResp(cmdbEventManager.enableHostWatch());
    }

    @Override
    public InternalResponse<Boolean> disableHostWatch() {
        return InternalResponse.buildSuccessResp(cmdbEventManager.disableHostWatch());
    }

    @Override
    public InternalResponse<Boolean> enableSyncApp() {
        return InternalResponse.buildSuccessResp(syncOpService.enableSyncApp());
    }

    @Override
    public InternalResponse<Boolean> disableSyncApp() {
        return InternalResponse.buildSuccessResp(syncOpService.disableSyncApp());
    }

    @Override
    public InternalResponse<Boolean> enableSyncHost() {
        return InternalResponse.buildSuccessResp(syncOpService.enableSyncHost());
    }

    @Override
    public InternalResponse<Boolean> disableSyncHost() {
        return InternalResponse.buildSuccessResp(syncOpService.disableSyncHost());
    }

    @Override
    public InternalResponse<Boolean> enableSyncAgentStatus() {
        return InternalResponse.buildSuccessResp(syncOpService.enableSyncAgentStatus());
    }

    @Override
    public InternalResponse<Boolean> disableSyncAgentStatus() {
        return InternalResponse.buildSuccessResp(syncOpService.disableSyncAgentStatus());
    }

    private ServiceHostInfoDTO convertToServiceHostInfo(long appId, ApplicationHostDTO hostInfo) {
        ServiceHostInfoDTO serviceHostInfo = new ServiceHostInfoDTO();
        serviceHostInfo.setAppId(appId);
        serviceHostInfo.setCloudAreaId(hostInfo.getCloudAreaId());
        serviceHostInfo.setIp(hostInfo.getIp());
        serviceHostInfo.setIpv6(hostInfo.getIpv6());
        serviceHostInfo.setHostId(hostInfo.getHostId());
        return serviceHostInfo;
    }
}
