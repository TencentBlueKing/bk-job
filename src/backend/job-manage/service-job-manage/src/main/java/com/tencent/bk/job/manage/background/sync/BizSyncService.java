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

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CMDB业务同步逻辑
 */
@Slf4j
@Service
public class BizSyncService extends BasicAppSyncService {

    private final ApplicationDAO applicationDAO;
    private final JobManageConfig jobManageConfig;

    @Autowired
    public BizSyncService(ApplicationDAO applicationDAO,
                          NoTenantHostDAO noTenantHostDAO,
                          ApplicationService applicationService,
                          IBizCmdbClient bizCmdbClient,
                          JobManageConfig jobManageConfig) {
        super(applicationDAO, noTenantHostDAO, applicationService, bizCmdbClient);
        this.applicationDAO = applicationDAO;
        this.jobManageConfig = jobManageConfig;
    }

    public void syncBizFromCMDB(String tenantId) {
        log.info("tenantId={}, begin to syncBizFromCMDB", tenantId);
        List<ApplicationDTO> ccBizApps = bizCmdbClient.getAllBizApps(tenantId);

        // 对比业务信息，分出要新增的/要改的/要删的分别处理
        List<ApplicationDTO> insertList;
        List<ApplicationDTO> updateList;
        List<ApplicationDTO> deleteList;
        // 对比库中数据与接口数据
        List<ApplicationDTO> localBizApps = applicationDAO.listAllBizAppsWithDeleted(tenantId);
        // CMDB业务ScopeId
        Set<String> ccBizAppScopeIds = ccBizApps.stream()
            .map(ccBizApp -> ccBizApp.getScope().getId())
            .collect(Collectors.toSet());
        // CMDB接口空数据保护
        if (ccBizAppScopeIds.isEmpty()) {
            log.warn("CMDB Biz App data is empty, quit sync");
            return;
        }
        log.info(String.format("ccBizAppScopeIds:%s", String.join(",",
            ccBizAppScopeIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        // 本地业务ScopeId
        Set<String> localBizAppScopeIds =
            localBizApps.stream().
                map(localBizApp -> localBizApp.getScope().getId())
                .collect(Collectors.toSet());
        log.info(String.format("localBizAppScopeIds:%s", String.join(",",
            localBizAppScopeIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        // CMDB-本地：计算新增业务
        insertList =
            ccBizApps.stream().filter(ccBizApp ->
                !localBizAppScopeIds.contains(ccBizApp.getScope().getId())).collect(Collectors.toList());
        log.info(String.format("biz app insertList scopeIds:%s", String.join(",",
            insertList.stream().map(bizAppInfoDTO -> bizAppInfoDTO.getScope().getId())
                .collect(Collectors.toSet()))));
        // 本地&CMDB交集：计算需要更新的业务
        updateList =
            ccBizApps.stream().filter(ccBizAppInfoDTO ->
                    localBizAppScopeIds.contains(ccBizAppInfoDTO.getScope().getId()))
                .collect(Collectors.toList());
        log.info(String.format("biz app updateList scopeIds:%s", String.join(",",
            updateList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getScope().getId()).collect(Collectors.toSet()))));
        // 将本地业务的appId赋给CMDB拿到的业务
        updateAppIdByScope(updateList, genScopeAppIdMap(localBizApps));
        // 本地-CMDB：计算需要删除的业务
        deleteList =
            localBizApps.stream().filter(bizAppInfoDTO ->
                    !ccBizAppScopeIds.contains(bizAppInfoDTO.getScope().getId()))
                .collect(Collectors.toList());
        log.info(String.format("app deleteList scopeIds:%s", String.join(",",
            deleteList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getScope().getId()).collect(Collectors.toSet()))));
        if (isSafeToApplyChange(localBizApps, deleteList)) {
            applyAppsChangeByScope(insertList, deleteList, updateList);
        }
    }

    /**
     * 根据配置信息判断业务数据变更是否安全
     *
     * @param localBizApps 本地DB中的业务信息
     * @param deleteList   需要设置为删除状态的业务列表
     * @return 本次业务数据变更是否安全
     */
    private boolean isSafeToApplyChange(List<ApplicationDTO> localBizApps, List<ApplicationDTO> deleteList) {
        if (!jobManageConfig.isEnableAppDeleteProtect()) {
            return true;
        }
        // 在本地未被删除的业务
        List<ApplicationDTO> notDeleteLocalBizApps = localBizApps.stream()
            .filter(app -> !app.isDeleted())
            .collect(Collectors.toList());
        // 在本地已经被软删除的业务
        List<ApplicationDTO> deletedLocalBizApps = localBizApps.stream()
            .filter(ApplicationDTO::isDeleted)
            .collect(Collectors.toList());
        if (notDeleteLocalBizApps.isEmpty()) {
            return true;
        }
        // 计算新删除的业务数量
        int newDeleteNum = deleteList.size() - deletedLocalBizApps.size();
        float deleteRatio = (float) newDeleteNum / notDeleteLocalBizApps.size();
        if (log.isDebugEnabled()) {
            log.debug(
                "notDeleteLocalBizApps.size={}, deletedLocalBizApps.size={}, " +
                    "deleteList.size={}, newDeleteNum={}, deleteRatio={}",
                notDeleteLocalBizApps.size(),
                deletedLocalBizApps.size(),
                deleteList.size(),
                newDeleteNum,
                deleteRatio
            );
        }
        // 根据最大删除比例阈值判断本次变更是否安全
        if (deleteRatio > jobManageConfig.getMaxAppDeleteRatio()) {
            log.error(
                "FATAL|deleteRatio({}/{}={}) is beyond maxAppDeleteRatio({}), dangerous! Not apply app change",
                newDeleteNum,
                notDeleteLocalBizApps.size(),
                deleteRatio,
                jobManageConfig.getMaxAppDeleteRatio()
            );
            return false;
        }
        return true;
    }
}
