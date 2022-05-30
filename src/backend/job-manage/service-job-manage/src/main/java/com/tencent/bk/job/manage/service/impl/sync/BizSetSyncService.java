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

import com.tencent.bk.job.common.cc.model.bizset.BizInfo;
import com.tencent.bk.job.common.cc.model.bizset.BizSetInfo;
import com.tencent.bk.job.common.cc.model.bizset.BizSetScope;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationAttrsDO;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.impl.BizSetService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
    private final BizSetService bizSetService;

    @Autowired
    public BizSetSyncService(DSLContext dslContext,
                             ApplicationDAO applicationDAO,
                             ApplicationHostDAO applicationHostDAO,
                             ApplicationService applicationService,
                             IBizSetCmdbClient bizSetCmdbClient,
                             BizSetService bizSetService) {
        super(dslContext, applicationDAO, applicationHostDAO, applicationService);
        this.applicationDAO = applicationDAO;
        this.bizSetCmdbClient = bizSetCmdbClient;
        this.bizSetService = bizSetService;
    }

    public void syncBizSetFromCMDB() {
        if (!bizSetService.isBizSetMigratedToCMDB()) {
            log.warn("Job BizSets have not been migrated to CMDB, " +
                "do not sync bizSet from CMDB, " +
                "please use upgrader in package to migrate as soon as possible"
            );
            return;
        }
        log.info("[{}] Begin to sync bizSet from cmdb", Thread.currentThread().getName());
        List<BizSetInfo> ccBizSets = bizSetCmdbClient.getAllBizSetApps();
        if (log.isInfoEnabled()) {
            log.info("Sync cmdb bizSet result: {}", JsonUtils.toJson(ccBizSets));
        }

        // CMDB业务bizId
        Set<Long> cmdbBizSetIds = ccBizSets.stream()
            .map(BizSetInfo::getId)
            .collect(Collectors.toSet());
        // CMDB接口空数据保护
        if (cmdbBizSetIds.isEmpty()) {
            log.warn("CMDB BizSet data is empty, quit sync");
            return;
        }
        log.info("Cmdb sync bizSetIds: {}", cmdbBizSetIds);

        List<ApplicationDTO> localBizSetApps = applicationDAO.listAllBizSetAppsWithDeleted();
        // 本地业务bizId
        Set<Long> localBizSetIds =
            localBizSetApps.stream()
                .map(localBizSetApp -> Long.valueOf(localBizSetApp.getScope().getId()))
                .collect(Collectors.toSet());
        log.info("Local cached bizSetIds: {}", localBizSetIds);

        // 对比业务集信息，分出要新增的/要改的/要删的分别处理
        List<ApplicationDTO> insertApps = computeInsertApps(ccBizSets, localBizSetIds);
        List<ApplicationDTO> updateApps = computeUpdateApps(ccBizSets, localBizSetApps, localBizSetIds);
        List<ApplicationDTO> deleteApps = computeDeleteApps(cmdbBizSetIds, localBizSetApps);

        applyAppsChangeByScope(insertApps, deleteApps, updateApps);
    }

    /**
     * 计算新增业务集
     *
     * @param cmdbBizSets    从cmdb同步的业务集
     * @param localBizSetIds 本地缓存的业务集ID列表
     * @return 新增的业务
     */
    private List<ApplicationDTO> computeInsertApps(List<BizSetInfo> cmdbBizSets, Set<Long> localBizSetIds) {
        // CMDB-本地：计算新增业务集
        List<ApplicationDTO> insertList =
            cmdbBizSets.stream()
                .filter(ccBizSet -> !localBizSetIds.contains(ccBizSet.getId()))
                .map(this::convertBizSetToApplication)
                .collect(Collectors.toList());
        log.info("Insert bizSetIds: {}", String.join(",",
            insertList.stream().map(bizSetAppInfoDTO -> bizSetAppInfoDTO.getScope().getId())
                .collect(Collectors.toSet())));
        return insertList;
    }

    /**
     * 计算更新业务集
     *
     * @param cmdbBizSets     从cmdb同步的业务集
     * @param localBizSetApps 本次缓存的业务集
     * @param localBizSetIds  本地缓存的业务集ID列表
     * @return 新增的业务
     */
    private List<ApplicationDTO> computeUpdateApps(List<BizSetInfo> cmdbBizSets,
                                                   List<ApplicationDTO> localBizSetApps,
                                                   Set<Long> localBizSetIds) {
        // 本地&CMDB交集：计算需要更新的业务集
        List<ApplicationDTO> updateList =
            cmdbBizSets.stream().filter(ccBizSetAppInfoDTO ->
                localBizSetIds.contains(ccBizSetAppInfoDTO.getId()))
                .map(this::convertBizSetToApplication)
                .collect(Collectors.toList());
        log.info("Update bizSetIds: {}", String.join(",",
            updateList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getScope().getId()).collect(Collectors.toSet())));
        // 将本地业务集的appId赋给CMDB拿到的业务集
        updateAppIdByScope(updateList, genScopeAppIdMap(localBizSetApps));
        // 过滤掉未接入cmdb的业务集（原Job业务集)
        updateList = updateList.stream()
            .filter(app -> FeatureToggle.isCmdbBizSetEnabledForApp(app.getId()))
            .collect(Collectors.toList());
        return updateList;
    }

    /**
     * 计算删除的业务集
     *
     * @param cmdbBizSetIds   cmdb业务集ID列表
     * @param localBizSetApps 本地缓存的业务集
     * @return 需要删除的业务
     */
    private List<ApplicationDTO> computeDeleteApps(Set<Long> cmdbBizSetIds, List<ApplicationDTO> localBizSetApps) {
        // 本地-CMDB：计算需要删除的业务集
        List<ApplicationDTO> deleteList = localBizSetApps.stream().filter(localApp ->
            !cmdbBizSetIds.contains(Long.valueOf(localApp.getScope().getId())))
            .collect(Collectors.toList());
        log.info("Delete bizSetIds: {}", String.join(",",
            deleteList.stream().map(applicationInfoDTO ->
                applicationInfoDTO.getScope().getId()).collect(Collectors.toSet())));
        // 过滤掉未接入cmdb的业务集（原Job业务集)
        deleteList = deleteList.stream()
            .filter(app -> FeatureToggle.isCmdbBizSetEnabledForApp(app.getId()))
            .collect(Collectors.toList());
        return deleteList;
    }

    private ApplicationDTO convertBizSetToApplication(BizSetInfo bizSetInfo) {
        ApplicationDTO appInfoDTO = new ApplicationDTO();
        appInfoDTO.setBkSupplierAccount(bizSetInfo.getSupplierAccount());
        appInfoDTO.setName(bizSetInfo.getName());
        appInfoDTO.setMaintainers(bizSetInfo.getMaintainer());
        appInfoDTO.setOperateDeptId(bizSetInfo.getOperateDeptId());
        appInfoDTO.setTimeZone(bizSetInfo.getTimezone());
        BizSetScope scope = bizSetInfo.getScope();
        ApplicationAttrsDO attrs = new ApplicationAttrsDO();
        if (scope != null && scope.isMatchAll()) {
            // 全业务
            appInfoDTO.setAppType(AppTypeEnum.ALL_APP);
            attrs.setMatchAllBiz(true);
        } else {
            // 普通业务集
            appInfoDTO.setAppType(AppTypeEnum.APP_SET);
            attrs.setMatchAllBiz(false);
            if (CollectionUtils.isNotEmpty(bizSetInfo.getBizList())) {
                attrs.setSubBizIds(bizSetInfo.getBizList().stream().map(BizInfo::getId).collect(Collectors.toList()));
            }
        }
        appInfoDTO.setAttrs(attrs);
        appInfoDTO.setScope(
            new ResourceScope(ResourceScopeTypeEnum.BIZ_SET, bizSetInfo.getId().toString()));
        return appInfoDTO;
    }
}
