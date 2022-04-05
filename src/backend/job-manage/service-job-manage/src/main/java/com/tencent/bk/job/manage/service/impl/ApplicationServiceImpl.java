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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.manager.app.ApplicationCache;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final DSLContext dslContext;
    private final ApplicationDAO applicationDAO;
    private final ApplicationCache applicationCache;
    private final TopologyHelper topologyHelper;
    private final AccountService accountService;

    @Autowired
    public ApplicationServiceImpl(DSLContext dslContext,
                                  ApplicationDAO applicationDAO,
                                  ApplicationCache applicationCache,
                                  TopologyHelper topologyHelper,
                                  AccountService accountService) {
        this.dslContext = dslContext;
        this.applicationDAO = applicationDAO;
        this.applicationCache = applicationCache;
        this.topologyHelper = topologyHelper;
        this.accountService = accountService;
    }

    @Override
    public Long getAppIdByScope(ResourceScope resourceScope) {
        ApplicationDTO applicationDTO = getAppByScope(resourceScope);
        if (applicationDTO == null) {
            return null;
        }
        return applicationDTO.getId();
    }

    @Override
    public ResourceScope getScopeByAppId(Long appId) {
        ApplicationDTO applicationDTO = getAppByAppId(appId);
        if (applicationDTO == null) {
            return null;
        }
        return getAppByAppId(appId).getScope();
    }

    @Override
    public Map<Long, ResourceScope> getScopeByAppIds(Collection<Long> appIds) {
        List<ApplicationDTO> applications = listAppsByAppIds(appIds);
        return applications.stream().collect(Collectors.toMap(ApplicationDTO::getId, ApplicationDTO::getScope));
    }

    @Override
    public Map<ResourceScope, Long> getAppIdByScopeList(Collection<ResourceScope> scopeList) {
        Map<ResourceScope, Long> map = new HashMap<>();
        for (ResourceScope resourceScope : scopeList) {
            ApplicationDTO appDTO = applicationCache.getApplication(resourceScope);
            if (appDTO != null) {
                map.put(resourceScope, appDTO.getId());
            }
        }
        return map;
    }

    @Override
    public ApplicationDTO getAppByAppId(Long appId) throws NotFoundException {
        ApplicationDTO application = applicationCache.getApplication(appId);
        if (application == null) {
            application = applicationDAO.getAppById(appId);
            if (application != null) {
                applicationCache.addOrUpdateApp(application);
            } else {
                throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
            }
        }
        return application;
    }

    @Override
    public ApplicationDTO getAppByScope(ResourceScope resourceScope) {
        ApplicationDTO application = applicationCache.getApplication(resourceScope);
        if (application == null) {
            application = applicationDAO.getAppByScope(resourceScope);
            if (application != null) {
                applicationCache.addOrUpdateApp(application);
            } else {
                throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
            }
        }
        return application;
    }

    @Override
    public ApplicationDTO getAppByScope(String scopeType, String scopeId) {
        return getAppByScope(new ResourceScope(scopeType, scopeId));
    }

    @Override
    public List<ApplicationDTO> listAppsByAppIds(Collection<Long> appIds) {
        return applicationCache.listApplicationsByAppIds(appIds);
    }

    @Override
    public List<ApplicationDTO> listBizAppsByBizIds(Collection<Long> bizIds) {
        return applicationDAO.listBizAppsByBizIds(bizIds);
    }

    @Override
    public List<Long> getBizSetAppIdsForBiz(Long appId) {
        //1.查找包含当前业务的业务集、全业务
        List<Long> fullAppIds = new ArrayList<>();
        fullAppIds.add(appId);
        List<ApplicationDTO> appSets = applicationDAO.listAppsByType(AppTypeEnum.APP_SET);
        if (appSets != null && !appSets.isEmpty()) {
            appSets.forEach(appSet -> {
                List<Long> subAppIds = topologyHelper.getBizSetSubBizIds(appSet);
                if (subAppIds.contains(appId)) {
                    fullAppIds.add(appSet.getId());
                }
            });
        }
        List<ApplicationDTO> allAppRecords = applicationDAO.listAppsByType(AppTypeEnum.ALL_APP);
        if (allAppRecords != null && !allAppRecords.isEmpty()) {
            allAppRecords.forEach(record -> fullAppIds.add(record.getId()));
        }
        return fullAppIds;
    }

    @Override
    public List<ApplicationDTO> listAppsByType(AppTypeEnum appType) {
        return applicationDAO.listAppsByType(appType);
    }

    @Override
    public Long createApp(ApplicationDTO application) {
        Long appId = applicationDAO.insertApp(dslContext, application);
        application.setId(appId);
        applicationCache.addOrUpdateApp(application);
        try {
            // 创建默认账号
            accountService.createDefaultAccounts(appId);
        } catch (Exception e) {
            log.warn("Fail to create default accounts for appId={}", appId);
        }
        return appId;
    }

    @Override
    public Long createAppWithSpecifiedAppId(ApplicationDTO application) {
        Long appId = applicationDAO.insertAppWithSpecifiedAppId(dslContext, application);
        application.setId(appId);
        applicationCache.addOrUpdateApp(application);
        try {
            // 创建默认账号
            accountService.createDefaultAccounts(appId);
        } catch (Exception e) {
            log.warn("Fail to create default accounts for appId={}", appId);
        }
        return appId;
    }

    @Override
    public Integer countApps() {
        return applicationDAO.countApps();
    }

    @Override
    public List<ApplicationDTO> listAllApps() {
        return applicationDAO.listAllApps();
    }

    @Override
    public boolean checkAppPermission(long appId, String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        username = username.trim();
        ApplicationDTO appInfo = getAppByAppId(appId);
        if (log.isDebugEnabled()) {
            log.debug("Get app info for {}|{}", appId, appInfo);
        }
        if (appInfo == null) {
            log.warn("Check app permission, app[{}] is not exist!", appId);
            return false;
        }
        String maintainers = appInfo.getMaintainers();
        if (log.isDebugEnabled()) {
            log.debug("Checking app {} user {}|{}", appId, username, maintainers);
        }
        if (StringUtils.isNotBlank(maintainers)) {
            for (String maintainer : maintainers.split("[,;]")) {
                if (StringUtils.isBlank(maintainer)) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Checking...|{}|{}|{}", appId, username, maintainer);
                }
                if (username.equals(maintainer.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void updateApp(ApplicationDTO application) {
        applicationDAO.updateApp(dslContext, application);
        applicationCache.addOrUpdateApp(application);
    }

    @Override
    public void deleteApp(Long appId) {
        applicationDAO.deleteAppByIdSoftly(dslContext, appId);
        applicationCache.deleteApp(appId);
    }

    @Override
    public void restoreDeletedApp(long appId) {
        applicationDAO.restoreDeletedApp(dslContext, appId);
        ApplicationDTO restoredApplication = applicationDAO.getAppById(appId);
        applicationCache.addOrUpdateApp(restoredApplication);
    }

    @Override
    public ApplicationDTO getAppByScopeIncludingDeleted(ResourceScope scope) {
        return applicationDAO.getAppByScopeIncludingDeleted(scope);
    }
}
