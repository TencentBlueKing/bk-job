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

import com.tencent.bk.job.common.cc.sdk.CcClient;
import com.tencent.bk.job.common.cc.sdk.CcClientFactory;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Job业务操作公共逻辑
 */
@Slf4j
public class BasicAppSyncService {

    private final DSLContext dslContext;
    private final ApplicationDAO applicationDAO;
    private final ApplicationHostDAO applicationHostDAO;
    private final ApplicationService applicationService;
    protected final CcClient ccClient = CcClientFactory.getCcClient();

    @Autowired
    public BasicAppSyncService(DSLContext dslContext, ApplicationDAO applicationDAO,
                               ApplicationHostDAO applicationHostDAO,
                               ApplicationService applicationService) {
        this.dslContext = dslContext;
        this.applicationDAO = applicationDAO;
        this.applicationHostDAO = applicationHostDAO;
        this.applicationService = applicationService;
    }

    protected void deleteAppFromDb(ApplicationDTO applicationDTO) {
        log.info("deleteAppFromDb:" + applicationDTO.getId());
        //先删Job业务对应主机
        applicationHostDAO.deleteAppHostInfoByAppId(dslContext, applicationDTO.getId());
        //再删Job业务本身
        applicationDAO.deleteAppInfoById(dslContext, applicationDTO.getId());
    }

    protected void addAppToDb(ApplicationDTO applicationDTO) {
        //先添加Job业务本身
        log.info("insertAppInfo:" + JsonUtils.toJson(applicationDTO));
        applicationService.createApp(applicationDTO);
    }

    protected void applyAppsChangeByScope(List<ApplicationDTO> insertList,
                                          List<ApplicationDTO> deleteList,
                                          List<ApplicationDTO> updateList) {
        insertList.forEach(applicationInfoDTO -> {
            try {
                addAppToDb(applicationInfoDTO);
            } catch (Throwable t) {
                log.error("FATAL: insertApp fail:appId=" + applicationInfoDTO.getId(), t);
            }
        });
        updateList.forEach(applicationInfoDTO -> {
            try {
                applicationDAO.updateApp(dslContext, applicationInfoDTO);
            } catch (Throwable t) {
                log.error("FATAL: updateApp fail:appId=" + applicationInfoDTO.getId(), t);
            }
        });
        deleteList.forEach(applicationInfoDTO -> {
            try {
                deleteAppFromDb(applicationInfoDTO);
            } catch (Throwable t) {
                log.error("FATAL: deleteApp fail:appId=" + applicationInfoDTO.getId(), t);
            }
        });
    }
}
