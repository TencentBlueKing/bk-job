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

package com.tencent.bk.job.manage.background.sync;

import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.paas.user.IUserApiClient;
import com.tencent.bk.job.manage.config.JobManageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 同步所有租户的主机服务
 */
@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class AllTenantHostSyncService {

    private volatile boolean enableSyncHost;
    private final TenantHostSyncService tenantHostSyncService;
    private final IUserApiClient userMgrApiClient;

    private volatile boolean isRunning = false;

    @Autowired
    public AllTenantHostSyncService(JobManageConfig jobManageConfig,
                                    TenantHostSyncService tenantHostSyncService,
                                    IUserApiClient userMgrApiClient) {
        this.enableSyncHost = jobManageConfig.isEnableSyncHost();
        this.tenantHostSyncService = tenantHostSyncService;
        this.userMgrApiClient = userMgrApiClient;
    }

    public void syncHost() {
        if (!enableSyncHost) {
            log.info(
                "syncHost not enabled, skip, you can enable it in config file"
            );
            return;
        }
        if (isRunning) {
            log.info("last syncAllTenantHost is running, skip");
            return;
        }
        try {
            isRunning = true;
            doSyncHost();
        } catch (Throwable t) {
            log.error("Fail to syncAllTenantHost", t);
        } finally {
            isRunning = false;
        }
    }

    private void doSyncHost() {
        List<OpenApiTenant> tenantList = userMgrApiClient.listAllTenant();
        // 遍历所有租户同步主机
        for (OpenApiTenant openApiTenant : tenantList) {
            tenantHostSyncService.addSyncHostTaskIfNotExist(
                openApiTenant.getId()
            );
        }
    }

    public Boolean enableSyncHost() {
        enableSyncHost = true;
        return true;
    }

    public Boolean disableSyncHost() {
        enableSyncHost = false;
        return true;
    }

}
