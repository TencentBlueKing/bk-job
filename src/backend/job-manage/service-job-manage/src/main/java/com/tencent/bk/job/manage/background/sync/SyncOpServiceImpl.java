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

import com.tencent.bk.job.manage.service.SyncOpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class SyncOpServiceImpl implements SyncOpService {

    private final AllTenantHostSyncService allTenantHostSyncService;
    private final AgentStatusSyncService agentStatusSyncService;
    private final AppSyncService appSyncService;

    @Autowired
    public SyncOpServiceImpl(AgentStatusSyncService agentStatusSyncService,
                             AllTenantHostSyncService allTenantHostSyncService,
                             AppSyncService appSyncService) {
        this.agentStatusSyncService = agentStatusSyncService;
        this.allTenantHostSyncService = allTenantHostSyncService;
        this.appSyncService = appSyncService;
    }

    @Override
    public Boolean enableSyncApp() {
        return appSyncService.enableSyncApp();
    }

    @Override
    public Boolean disableSyncApp() {
        return appSyncService.disableSyncApp();
    }

    @Override
    public Boolean enableSyncHost() {
        return allTenantHostSyncService.enableSyncHost();
    }

    @Override
    public Boolean disableSyncHost() {
        return allTenantHostSyncService.disableSyncHost();
    }

    @Override
    public Boolean enableSyncAgentStatus() {
        return agentStatusSyncService.enableSyncAgentStatus();
    }

    @Override
    public Boolean disableSyncAgentStatus() {
        return agentStatusSyncService.disableSyncAgentStatus();
    }

}
