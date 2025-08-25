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

package com.tencent.bk.job.manage.background.sync.tenantset;

import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.model.tenantset.TenantSetInfo;
import com.tencent.bk.job.common.cc.model.tenantset.TenantSetScope;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.ITenantSetCmdbClient;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.model.dto.ApplicationAttrsDO;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.background.sync.BasicAppSyncService;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CMDB租户集同步逻辑
 */
@Slf4j
public class TenantSetSyncService extends BasicAppSyncService implements ITenantSetSyncService {

    private final ApplicationDAO applicationDAO;
    protected final ITenantSetCmdbClient tenantSetCmdbClient;
    private final CmdbConfig cmdbConfig;

    public TenantSetSyncService(ApplicationDAO applicationDAO,
                                NoTenantHostDAO noTenantHostDAO,
                                ApplicationService applicationService,
                                IBizCmdbClient bizCmdbClient,
                                ITenantSetCmdbClient tenantSetCmdbClient,
                                CmdbConfig cmdbConfig) {
        super(applicationDAO, noTenantHostDAO, applicationService, bizCmdbClient);
        this.applicationDAO = applicationDAO;
        this.tenantSetCmdbClient = tenantSetCmdbClient;
        this.cmdbConfig = cmdbConfig;
    }

    /**
     * 从CMDB同步租户集信息到本地DB
     */
    @Override
    public void syncTenantSetFromCMDB() {
        log.info("[{}] Begin to sync tenantSet from cmdb", Thread.currentThread().getName());
        List<TenantSetInfo> ccTenantSets = tenantSetCmdbClient.listAllTenantSet();
        if (log.isInfoEnabled()) {
            log.info("Sync cmdb tenantSet result: {}", JsonUtils.toJson(ccTenantSets));
        }

        // CMDB租户Id
        Set<Long> cmdbTenantSetIds = ccTenantSets.stream()
            .map(TenantSetInfo::getId)
            .collect(Collectors.toSet());
        // CMDB接口空数据保护
        if (cmdbTenantSetIds.isEmpty()) {
            log.warn("CMDB TenantSet data is empty, quit sync");
            return;
        }
        log.info("Cmdb sync tenantSetIds: {}", cmdbTenantSetIds);

        List<ApplicationDTO> localTenantSetApps = applicationDAO.listAllTenantSetAppsWithDeleted();
        // 本地租户bizId
        Set<Long> localTenantSetIds =
            localTenantSetApps.stream()
                .map(localTenantSetApp -> Long.valueOf(localTenantSetApp.getScope().getId()))
                .collect(Collectors.toSet());
        log.info("Local cached tenantSetIds: {}", localTenantSetIds);

        // 对比租户集信息，分出要新增的/要改的/要删的分别处理
        List<ApplicationDTO> insertTenantSets = computeInsertTenantSets(ccTenantSets, localTenantSetIds);
        List<ApplicationDTO> updateTenantSets = computeUpdateTenantSets(ccTenantSets, localTenantSetApps,
            localTenantSetIds);
        List<ApplicationDTO> deleteTenantSets = computeDeleteTenantSets(cmdbTenantSetIds, localTenantSetApps);

        applyAppsChangeByScope(insertTenantSets, deleteTenantSets, updateTenantSets);
    }

    /**
     * 计算新增租户集
     *
     * @param cmdbTenantSets    从cmdb同步的租户集
     * @param localTenantSetIds 本地缓存的租户集ID列表
     * @return 新增的租户
     */
    private List<ApplicationDTO> computeInsertTenantSets(List<TenantSetInfo> cmdbTenantSets,
                                                         Set<Long> localTenantSetIds) {
        // CMDB-本地：计算新增租户集
        List<ApplicationDTO> insertList =
            cmdbTenantSets.stream()
                .filter(ccTenantSet -> !localTenantSetIds.contains(ccTenantSet.getId()))
                .map(this::convertTenantSetToApplication)
                .collect(Collectors.toList());
        log.info("Insert tenantSetIds: {}", String.join(",",
            insertList.stream().map(tenantSetAppInfoDTO -> tenantSetAppInfoDTO.getScope().getId())
                .collect(Collectors.toSet())));
        return insertList;
    }

    /**
     * 计算更新租户集
     *
     * @param cmdbTenantSets     从cmdb同步的租户集
     * @param localTenantSetApps 本次缓存的租户集
     * @param localTenantSetIds  本地缓存的租户集ID列表
     * @return 新增的租户
     */
    private List<ApplicationDTO> computeUpdateTenantSets(List<TenantSetInfo> cmdbTenantSets,
                                                         List<ApplicationDTO> localTenantSetApps,
                                                         Set<Long> localTenantSetIds) {
        // 本地&CMDB交集：计算需要更新的租户集
        List<ApplicationDTO> updateList =
            cmdbTenantSets.stream().filter(ccTenantSetAppInfoDTO ->
                    localTenantSetIds.contains(ccTenantSetAppInfoDTO.getId()))
                .map(this::convertTenantSetToApplication)
                .collect(Collectors.toList());
        log.info("Update tenantSetIds: {}", String.join(",",
            updateList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getScope().getId()).collect(Collectors.toSet())));
        // 将本地租户集的appId赋给CMDB拿到的租户集
        updateAppIdByScope(updateList, genScopeAppIdMap(localTenantSetApps));
        return updateList;
    }

    /**
     * 计算删除的租户集
     *
     * @param cmdbTenantSetIds   cmdb租户集ID列表
     * @param localTenantSetApps 本地缓存的租户集
     * @return 需要删除的租户
     */
    private List<ApplicationDTO> computeDeleteTenantSets(Set<Long> cmdbTenantSetIds,
                                                         List<ApplicationDTO> localTenantSetApps) {
        // 本地-CMDB：计算需要删除的租户集
        List<ApplicationDTO> deleteList = localTenantSetApps.stream().filter(localApp ->
                !cmdbTenantSetIds.contains(Long.valueOf(localApp.getScope().getId())))
            .collect(Collectors.toList());
        log.info("Delete tenantSetIds: {}", String.join(",",
            deleteList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getScope().getId()).collect(Collectors.toSet())));
        return deleteList;
    }

    private ApplicationDTO convertTenantSetToApplication(TenantSetInfo tenantSetInfo) {
        ApplicationDTO appInfoDTO = new ApplicationDTO();
        appInfoDTO.setBkSupplierAccount(cmdbConfig.getDefaultSupplierAccount());
        appInfoDTO.setName(tenantSetInfo.getName());
        TenantSetScope scope = tenantSetInfo.getScope();
        ApplicationAttrsDO attrs = new ApplicationAttrsDO();
        // 全租户
        attrs.setMatchAllTenant(scope != null && scope.isMatchAll());
        appInfoDTO.setAttrs(attrs);
        appInfoDTO.setScope(
            new ResourceScope(ResourceScopeTypeEnum.TENANT_SET, tenantSetInfo.getId().toString()));
        appInfoDTO.setDeFault(tenantSetInfo.getDeFault());
        appInfoDTO.setTenantId(TenantIdConstants.SYSTEM_TENANT_ID);
        return appInfoDTO;
    }
}
