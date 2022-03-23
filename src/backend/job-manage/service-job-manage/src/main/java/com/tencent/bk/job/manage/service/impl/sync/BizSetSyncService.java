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

import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CMDB业务集同步逻辑
 */
@Slf4j
@Service
public class BizSetSyncService extends BasicAppSyncService {

    private final ApplicationDAO applicationDAO;
    protected final IBizSetCmdbClient bizSetCmdbClient;

    @Autowired
    public BizSetSyncService(DSLContext dslContext,
                             ApplicationDAO applicationDAO,
                             ApplicationHostDAO applicationHostDAO,
                             ApplicationService applicationService,
                             IBizSetCmdbClient bizSetCmdbClient) {
        super(dslContext, applicationDAO, applicationHostDAO, applicationService);
        this.applicationDAO = applicationDAO;
        this.bizSetCmdbClient = bizSetCmdbClient;
    }

    public void syncBizSetFromCMDB() {
        log.info("[{}] Begin to sync bizSet from cmdb", Thread.currentThread().getName());
        List<ApplicationDTO> ccBizSetApps = bizSetCmdbClient.getAllBizSetApps();

        // 对比业务集信息，分出要新增的/要改的/要删的分别处理
        List<ApplicationDTO> insertList;
        List<ApplicationDTO> updateList;
        List<ApplicationDTO> deleteList;
        // 对比库中数据与接口数据
        List<ApplicationDTO> localBizSetApps = applicationDAO.listAllBizSetAppsWithDeleted();
        // CMDB业务ScopeId
        Set<String> ccBizSetAppScopeIds = ccBizSetApps.stream()
            .map(ccBizApp -> ccBizApp.getScope().getId())
            .collect(Collectors.toSet());
        // CMDB接口空数据保护
        if (ccBizSetAppScopeIds.isEmpty()) {
            log.warn("CMDB BizSet data is empty, quit sync");
            return;
        }
        log.info("Cmdb sync bizSetIds: {}", String.join(",", ccBizSetAppScopeIds));

        // 本地业务ScopeId
        Set<String> localBizSetAppScopeIds =
            localBizSetApps.stream().
                map(localBizSetApp -> localBizSetApp.getScope().getId())
                .collect(Collectors.toSet());
        log.info("Local bizSetIds: {}", String.join(",", localBizSetAppScopeIds));

        // CMDB-本地：计算新增业务集
        insertList =
            ccBizSetApps.stream().filter(ccBizSetApp ->
                !localBizSetAppScopeIds.contains(ccBizSetApp.getScope().getId())).collect(Collectors.toList());
        log.info("Insert bizSetIds: {}", String.join(",",
            insertList.stream().map(bizSetAppInfoDTO -> bizSetAppInfoDTO.getScope().getId())
                .collect(Collectors.toSet())));
        // 本地&CMDB交集：计算需要更新的业务集
        updateList =
            ccBizSetApps.stream().filter(ccBizSetAppInfoDTO ->
                localBizSetAppScopeIds.contains(ccBizSetAppInfoDTO.getScope().getId()))
                .collect(Collectors.toList());
        log.info(String.format("bizSet app updateList scopeIds:%s", String.join(",",
            updateList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getScope().getId()).collect(Collectors.toSet()))));
        // 将本地业务集的appId赋给CMDB拿到的业务集
        updateAppIdByScope(updateList, genScopeAppIdMap(localBizSetApps));
        // 本地-CMDB：计算需要删除的业务集
        deleteList =
            localBizSetApps.stream().filter(bizSetAppInfoDTO ->
                !ccBizSetAppScopeIds.contains(bizSetAppInfoDTO.getScope().getId()))
                .collect(Collectors.toList());
        log.info(String.format("bizSet app deleteList scopeIds:%s", String.join(",",
            deleteList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getScope().getId()).collect(Collectors.toSet()))));
        applyAppsChangeByScope(insertList, deleteList, updateList);
    }
}
