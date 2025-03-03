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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class TenantHostSyncService {


    private final ApplicationDAO applicationDAO;
    private final HostSyncService hostSyncService;

    @Autowired
    public TenantHostSyncService(ApplicationDAO applicationDAO, HostSyncService hostSyncService) {
        this.applicationDAO = applicationDAO;
        this.hostSyncService = hostSyncService;
    }

    public void syncAllBizHosts(String tenantId) {
        log.info("syncAllBizHosts: tenantId={}", tenantId);
        List<ApplicationDTO> appList = applicationDAO.listAllAppsForTenant(tenantId);
        List<ApplicationDTO> failedAppList = new ArrayList<>();
        for (ApplicationDTO applicationDTO : appList) {
            Triple<Set<BasicHostDTO>, Long, Long> triple = hostSyncService.syncBizHostsAtOnce(applicationDTO);
            if (triple == null) {
                failedAppList.add(applicationDTO);
            }
        }
        if (!failedAppList.isEmpty()) {
            log.warn(
                "syncAllBizHosts: tenantId={}, failedAppList={}",
                tenantId,
                failedAppList.stream().map(
                    applicationDTO -> "(" + applicationDTO.getBizIdIfBizApp() + "," + applicationDTO.getName() + ")"
                ).collect(Collectors.toList())
            );
        }
        log.info("syncAllBizHosts end: tenantId={}", tenantId);
    }

}
