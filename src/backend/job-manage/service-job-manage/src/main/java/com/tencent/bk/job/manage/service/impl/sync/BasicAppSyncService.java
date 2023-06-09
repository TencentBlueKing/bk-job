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

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Job业务操作公共逻辑
 */
@Slf4j
public class BasicAppSyncService {

    private final ApplicationDAO applicationDAO;
    private final ApplicationHostDAO applicationHostDAO;
    private final ApplicationService applicationService;
    protected final IBizCmdbClient bizCmdbClient;

    @Autowired
    public BasicAppSyncService(ApplicationDAO applicationDAO,
                               ApplicationHostDAO applicationHostDAO,
                               ApplicationService applicationService,
                               IBizCmdbClient bizCmdbClient) {
        this.applicationDAO = applicationDAO;
        this.applicationHostDAO = applicationHostDAO;
        this.applicationService = applicationService;
        this.bizCmdbClient = bizCmdbClient;
    }

    protected Map<String, Long> genScopeAppIdMap(List<ApplicationDTO> appList) {
        Map<String, Long> scopeAppIdMap = new HashMap<>();
        for (ApplicationDTO app : appList) {
            ResourceScope scope = app.getScope();
            scopeAppIdMap.put(
                scope.getType().getValue() + "_" + scope.getId(),
                app.getId()
            );
        }
        return scopeAppIdMap;
    }

    protected void updateAppIdByScope(List<ApplicationDTO> appList, Map<String, Long> scopeAppIdMap) {
        for (ApplicationDTO appDTO : appList) {
            ResourceScope scope = appDTO.getScope();
            appDTO.setId(scopeAppIdMap.get(scope.getType().getValue() + "_" + scope.getId()));
        }
    }

    protected void deleteApp(ApplicationDTO applicationDTO) {
        log.info("deleteAppFromDb:" + applicationDTO.getId());
        //先删Job业务对应主机
        if (applicationDTO.getScope().getType() == ResourceScopeTypeEnum.BIZ) {
            long bizId = Long.parseLong(applicationDTO.getScope().getId());
            int deletedHostNum = applicationHostDAO.deleteBizHostInfoByBizId(bizId);
            log.info("{} hosts of biz {} deleted", deletedHostNum, bizId);
        }
        //再删Job业务本身
        applicationService.deleteApp(applicationDTO.getId());
    }

    protected void applyAppsChangeByScope(List<ApplicationDTO> insertApps,
                                          List<ApplicationDTO> deleteApps,
                                          List<ApplicationDTO> updateApps) {
        insertApps.forEach(application -> {
            try {
                log.info("Insert app: {}", JsonUtils.toJson(application));
                applicationService.createApp(application);
            } catch (Throwable t) {
                log.error("FATAL: insertApp fail:appId=" + application.getId(), t);
            }
        });
        updateApps.forEach(applicationInfoDTO -> {
            try {
                log.info("Update app: {}", JsonUtils.toJson(applicationInfoDTO));
                applicationService.updateApp(applicationInfoDTO);
                applicationDAO.restoreDeletedApp(applicationInfoDTO.getId());
            } catch (Throwable t) {
                log.error("FATAL: updateApp fail:appId=" + applicationInfoDTO.getId(), t);
            }
        });
        deleteApps.forEach(applicationInfoDTO -> {
            try {
                log.info("Delete app: {}", JsonUtils.toJson(applicationInfoDTO));
                deleteApp(applicationInfoDTO);
            } catch (Throwable t) {
                log.error("FATAL: deleteApp fail:appId=" + applicationInfoDTO.getId(), t);
            }
        });
    }
}
