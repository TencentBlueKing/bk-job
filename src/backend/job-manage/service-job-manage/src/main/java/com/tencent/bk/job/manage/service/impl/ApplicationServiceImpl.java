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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.manager.app.ApplicationCache;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private final AccountService accountService;

    @Autowired
    public ApplicationServiceImpl(DSLContext dslContext,
                                  ApplicationDAO applicationDAO,
                                  ApplicationCache applicationCache,
                                  AccountService accountService) {
        this.dslContext = dslContext;
        this.applicationDAO = applicationDAO;
        this.applicationCache = applicationCache;
        this.accountService = accountService;
    }

    @Override
    public boolean existBiz(long bizId) {
        return applicationDAO.existBiz(bizId);
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
        checkApplication(application);
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
                log.info("Get app by scope, app not exist in db or cache! resourceScope: {}", resourceScope);
                throw new NotFoundException("App not found, resourceScope:" + resourceScope, ErrorCode.APP_NOT_EXIST);
            }
        }
        checkApplication(application);
        return application;
    }

    private void checkApplication(ApplicationDTO application) {
        if (application == null) {
            return;
        }
        if (application.getId() == null) {
            log.error("Empty appId");
            throw new InternalException("Empty appId for application", ErrorCode.INTERNAL_ERROR);
        }
        if (application.getScope() == null || application.getScope().getType() == null
            || StringUtils.isBlank(application.getScope().getId())) {
            log.error("Invalid resource scope, scope: {}", application.getScope());
            throw new InternalException("Invalid resource scope", ErrorCode.INTERNAL_ERROR);
        }
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
    public List<Long> getRelatedAppIds(Long appId) {
        // 查找包含当前业务的业务集、全业务
        List<Long> fullAppIds = new ArrayList<>();
        fullAppIds.add(appId);
        // 获取所有业务
        List<ApplicationDTO> allAppList = applicationDAO.listAllApps();
        if (CollectionUtils.isEmpty(allAppList)) {
            return Collections.emptyList();
        }

        // 业务集
        List<ApplicationDTO> appSetList = new ArrayList<>();
        // 全业务
        List<ApplicationDTO> allAppSetList = new ArrayList<>();

        ApplicationDTO currentApp = null;
        // 按类型分组
        for (ApplicationDTO appDTO : allAppList) {
            if (appDTO.getId().equals(appId)) {
                currentApp = appDTO;
            }
            if (appDTO.isAllBizSet()) {
                allAppSetList.add(appDTO);
            } else if (appDTO.isBizSet()) {
                appSetList.add(appDTO);
            }
        }

        // 查找包含当前业务的业务集
        if (currentApp != null && currentApp.isBiz()) {
            Long bizId = Long.parseLong(currentApp.getScope().getId());
            appSetList.forEach(appSet -> {
                List<Long> subBizIds = appSet.getSubBizIds();
                if (subBizIds != null && subBizIds.contains(bizId)) {
                    fullAppIds.add(appSet.getId());
                }
            });
        }

        // 处理全业务
        allAppSetList.forEach(record -> fullAppIds.add(record.getId()));
        return fullAppIds;
    }

    @Override
    public List<ApplicationDTO> listAppsByScopeType(ResourceScopeTypeEnum scopeType) {
        return applicationDAO.listAppsByScopeType(scopeType);
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
    public Integer countApps() {
        return applicationDAO.countApps();
    }

    @Override
    public List<ApplicationDTO> listAllApps() {
        return applicationDAO.listAllApps();
    }

    @Override
    public void updateApp(ApplicationDTO application) {
        log.info("Update app: {}", JsonUtils.toJson(application));
        applicationDAO.updateApp(dslContext, application);
        applicationCache.addOrUpdateApp(application);
    }

    @Override
    public void deleteApp(Long appId) {
        log.info("Delete app[{}]", appId);
        applicationDAO.deleteAppByIdSoftly(dslContext, appId);
        applicationCache.deleteApp(appId);
    }

    @Override
    public void restoreDeletedApp(long appId) {
        log.info("Restore deleted app[{}]", appId);
        applicationDAO.restoreDeletedApp(dslContext, appId);
        ApplicationDTO restoredApplication = applicationDAO.getAppById(appId);
        applicationCache.addOrUpdateApp(restoredApplication);
    }

    @Override
    public ApplicationDTO getAppByScopeIncludingDeleted(ResourceScope scope) {
        return applicationDAO.getAppByScopeIncludingDeleted(scope);
    }
}
