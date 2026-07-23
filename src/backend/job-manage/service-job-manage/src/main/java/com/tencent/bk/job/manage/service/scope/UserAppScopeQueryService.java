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

package com.tencent.bk.job.manage.service.scope;

import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.dto.AppResourceScopeResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.CompareUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.manage.model.dto.ApplicationFavorDTO;
import com.tencent.bk.job.manage.model.dto.UserAppScopeDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.impl.ApplicationFavorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户资源范围（业务/业务集等）查询服务
 */
@Slf4j
@Service
public class UserAppScopeQueryService {

    private final ApplicationService applicationService;
    private final ApplicationFavorService applicationFavorService;
    private final AppAuthService appAuthService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public UserAppScopeQueryService(ApplicationService applicationService,
                                    ApplicationFavorService applicationFavorService,
                                    AppAuthService appAuthService,
                                    AppScopeMappingService appScopeMappingService) {
        this.applicationService = applicationService;
        this.applicationFavorService = applicationFavorService;
        this.appAuthService = appAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }

    /**
     * 获取当前用户所见的已排序、带收藏信息的全部 Job 业务列表
     *
     * @param username 当前用户名
     * @return Job 业务列表
     */
    public List<UserAppScopeDTO> listSortedAppWithFavor(String username) {
        User user = JobContextUtil.getUser();
        List<ApplicationDTO> appList = applicationService.listAllAppsForTenant(user.getTenantId());
        List<AppResourceScope> appResourceScopeList =
            appList.stream()
                .map(app -> new AppResourceScope(app.getId(), app.getScope()))
                .collect(Collectors.toList());

        AppResourceScopeResult appResourceScopeResult =
            appAuthService.getAppResourceScopeList(user, appResourceScopeList);

        List<Long> authorizedAppIdList = extractAuthorizedAppIdList(appResourceScopeResult);

        List<UserAppScopeDTO> finalAppList = new ArrayList<>();
        if (Boolean.TRUE.equals(appResourceScopeResult.getAny())) {
            for (ApplicationDTO app : appList) {
                finalAppList.add(buildUserAppScope(app, true));
            }
        } else {
            for (ApplicationDTO app : appList) {
                finalAppList.add(buildUserAppScope(app, authorizedAppIdList.contains(app.getId())));
            }
        }
        setFavorState(username, finalAppList);
        sortApps(finalAppList);
        return finalAppList;
    }

    /**
     * 仅返回当前用户有权限的资源范围，并按内存分页
     *
     * @param username 当前用户名
     * @param offset   分页起始偏移（对应 PageUtil 的 start）
     * @param length   每页大小（对应 PageUtil 的 pageSize）
     * @return 分页结果
     */
    public PageData<UserAppScopeDTO> listAuthorizedScopesPaged(String username, int offset, int length) {
        List<UserAppScopeDTO> authorizedScopes = listSortedAppWithFavor(username).stream()
            .filter(scope -> Boolean.TRUE.equals(scope.getHasPermission()))
            .collect(Collectors.toList());
        return PageUtil.pageInMem(authorizedScopes, offset, length);
    }

    private UserAppScopeDTO buildUserAppScope(ApplicationDTO app, boolean hasPermission) {
        return new UserAppScopeDTO(
            app.getId(),
            app.getScope().getType().getValue(),
            app.getScope().getId(),
            app.isBuiltInResource(),
            app.isAllBizSet(),
            app.getName(),
            hasPermission,
            app.getTimeZone(),
            null,
            null
        );
    }

    private List<Long> extractAuthorizedAppIdList(AppResourceScopeResult appResourceScopeResult) {
        List<AppResourceScope> authorizedAppResourceScopes = appResourceScopeResult.getAppResourceScopeList();
        return authorizedAppResourceScopes.stream()
            .map(appResourceScope -> {
                if (appResourceScope.getAppId() != null) {
                    return appResourceScope.getAppId();
                }
                try {
                    return appScopeMappingService.getAppIdByScope(
                        appResourceScope.getType().getValue(), appResourceScope.getId());
                } catch (NotFoundException e) {
                    log.warn("Invalid scope", e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void setFavorState(String username, List<UserAppScopeDTO> finalAppList) {
        List<ApplicationFavorDTO> applicationFavorDTOList = applicationFavorService.getAppFavorListByUsername(username);
        Map<Long, Long> appIdFavorTimeMap = new HashMap<>();
        for (ApplicationFavorDTO applicationFavorDTO : applicationFavorDTOList) {
            appIdFavorTimeMap.put(applicationFavorDTO.getAppId(), applicationFavorDTO.getFavorTime());
        }
        for (UserAppScopeDTO appScope : finalAppList) {
            if (appIdFavorTimeMap.containsKey(appScope.getAppId())) {
                appScope.setFavor(true);
                appScope.setFavorTime(appIdFavorTimeMap.get(appScope.getAppId()));
            } else {
                appScope.setFavor(false);
                appScope.setFavorTime(null);
            }
        }
    }

    /**
     * 排序：有无权限、是否收藏、收藏时间倒序
     */
    private void sortApps(List<UserAppScopeDTO> appList) {
        appList.sort((o1, o2) -> {
            int result = o2.getHasPermission().compareTo(o1.getHasPermission());
            if (result != 0) {
                return result;
            }
            result = CompareUtil.safeCompareNullFront(o2.getFavor(), o1.getFavor());
            if (result != 0) {
                return result;
            }
            return CompareUtil.safeCompareNullFront(o2.getFavorTime(), o1.getFavorTime());
        });
    }
}
