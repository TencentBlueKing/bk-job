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
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CMDB业务同步逻辑
 */
@Slf4j
@Service
public class BizSyncService {

    private final DSLContext dslContext;
    private final ApplicationDAO applicationDAO;
    private final ApplicationHostDAO applicationHostDAO;
    private final ApplicationService applicationService;

    @Autowired
    public BizSyncService(DSLContext dslContext, ApplicationDAO applicationDAO,
                          ApplicationHostDAO applicationHostDAO,
                          ApplicationService applicationService) {
        this.dslContext = dslContext;
        this.applicationDAO = applicationDAO;
        this.applicationHostDAO = applicationHostDAO;
        this.applicationService = applicationService;
    }


    private void deleteAppFromDb(ApplicationDTO applicationDTO) {
        log.info("deleteAppFromDb:" + applicationDTO.getId());
        //先删业务对应主机
        applicationHostDAO.deleteAppHostInfoByAppId(dslContext, applicationDTO.getId());
        //再删业务本身
        applicationDAO.deleteAppInfoById(dslContext, applicationDTO.getId());
    }

    private void addAppToDb(ApplicationDTO applicationDTO,
                            List<ApplicationHostInfoDTO> applicationHostInfoDTOList) {
        log.info("addAppToDb:" + applicationDTO.getId() + "," + applicationHostInfoDTOList.size() + "hosts");
        //先添加业务本身
        log.info("insertAppInfo:" + JsonUtils.toJson(applicationDTO));
        applicationService.createApp(applicationDTO);
        //再添加业务对应主机
        applicationHostInfoDTOList.forEach(applicationHostInfoDTO -> {
            log.info("insertAppHostInfo:" + JsonUtils.toJson(applicationHostInfoDTO));
            applicationHostDAO.insertAppHostInfo(dslContext, applicationHostInfoDTO);
        });
    }


    public void syncBizFromCMDB() {
        log.info(Thread.currentThread().getName() + ":begin to sync app from cc");
        CcClient ccClient = CcClientFactory.getCcClient();
        List<ApplicationDTO> ccApps = ccClient.getAllApps();

        //对比业务信息，分出要删的/要改的/要新增的分别处理
        List<ApplicationDTO> insertList;
        List<ApplicationDTO> updateList;
        List<ApplicationDTO> deleteList;
        //对比库中数据与接口数据
        List<ApplicationDTO> localApps = applicationDAO.listAllApps();
        Set<Long> ccAppIds = ccApps.stream().map(ApplicationDTO::getId).collect(Collectors.toSet());
        //CC接口空数据保护
        if (ccAppIds.isEmpty()) {
            log.warn("CC App data is empty, quit sync");
            return;
        }
        log.info(String.format("ccAppIds:%s", String.join(",",
            ccAppIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        Set<Long> localAppIds =
            localApps.stream().map(ApplicationDTO::getId).collect(Collectors.toSet());
        log.info(String.format("localAppIds:%s", String.join(",",
            localAppIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        insertList =
            ccApps.stream().filter(applicationInfoDTO ->
                !localAppIds.contains(applicationInfoDTO.getId())).collect(Collectors.toList());
        log.info(String.format("app insertList:%s", String.join(",",
            insertList.stream().map(applicationInfoDTO -> applicationInfoDTO.getId().toString())
                .collect(Collectors.toSet()))));
        // 当前CC无业务类型数据，业务类型数据只能从本地数据判断
        List<Long> intersectLocalAppIds =
            localAppIds.stream().filter(ccAppIds::contains).collect(Collectors.toList());
        List<Long> updateIdList =
            localApps.stream().filter(applicationInfoDTO ->
                    applicationInfoDTO.getAppType() == AppTypeEnum.NORMAL
                        && intersectLocalAppIds.contains(applicationInfoDTO.getId()))
                .map(ApplicationDTO::getId).collect(Collectors.toList());
        updateList =
            ccApps.stream().filter(applicationInfoDTO ->
                updateIdList.contains(applicationInfoDTO.getId())).collect(Collectors.toList());
        log.info(String.format("app updateList:%s", String.join(",",
            updateList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getId().toString()).collect(Collectors.toSet()))));
        deleteList =
            localApps.stream().filter(applicationInfoDTO ->
                applicationInfoDTO.getAppType() == AppTypeEnum.NORMAL
                    && !ccAppIds.contains(applicationInfoDTO.getId())).collect(Collectors.toList());
        log.info(String.format("app deleteList:%s", String.join(",",
            deleteList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getId().toString()).collect(Collectors.toSet()))));
        insertList.forEach(applicationInfoDTO -> {
            try {
                addAppToDb(applicationInfoDTO, Collections.emptyList());
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
