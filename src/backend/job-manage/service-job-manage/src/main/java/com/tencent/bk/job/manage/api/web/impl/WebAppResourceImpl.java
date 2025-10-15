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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.dto.AppResourceScopeResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.CompareUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.manage.api.web.WebAppResource;
import com.tencent.bk.job.manage.model.dto.ApplicationFavorDTO;
import com.tencent.bk.job.manage.model.web.request.app.FavorAppReq;
import com.tencent.bk.job.manage.model.web.vo.AppVO;
import com.tencent.bk.job.manage.model.web.vo.PageDataWithAvailableIdList;
import com.tencent.bk.job.manage.model.web.vo.ScopeGroupWithAvailableScopeIdList;
import com.tencent.bk.job.manage.model.web.vo.ScopeVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.impl.ApplicationFavorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebAppResourceImpl implements WebAppResource {

    private final ApplicationService applicationService;
    private final ApplicationFavorService applicationFavorService;
    private final AppAuthService appAuthService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public WebAppResourceImpl(ApplicationService applicationService,
                              ApplicationFavorService applicationFavorService,
                              AppAuthService appAuthService,
                              AppScopeMappingService appScopeMappingService) {
        this.applicationService = applicationService;
        this.applicationFavorService = applicationFavorService;
        this.appAuthService = appAuthService;
        this.appScopeMappingService = appScopeMappingService;
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
                    // 如果业务不存在，那么忽略
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void setFavorState(String username, List<AppVO> finalAppList) {
        List<ApplicationFavorDTO> applicationFavorDTOList = applicationFavorService.getAppFavorListByUsername(username);
        Map<Long, Long> appIdFavorTimeMap = new HashMap<>();
        for (ApplicationFavorDTO applicationFavorDTO : applicationFavorDTOList) {
            appIdFavorTimeMap.put(applicationFavorDTO.getAppId(), applicationFavorDTO.getFavorTime());
        }
        for (AppVO appVO : finalAppList) {
            if (appIdFavorTimeMap.containsKey(appVO.getId())) {
                appVO.setFavor(true);
                appVO.setFavorTime(appIdFavorTimeMap.get(appVO.getId()));
            } else {
                appVO.setFavor(false);
                appVO.setFavorTime(null);
            }
        }
    }

    @Override
    public Response<PageDataWithAvailableIdList<AppVO, Long>> listAppWithFavor(String username,
                                                                               Integer start,
                                                                               Integer pageSize) {
        List<ApplicationDTO> appList = applicationService.listAllApps();
        List<AppResourceScope> appResourceScopeList =
            appList.stream()
                .map(app -> new AppResourceScope(app.getId(), app.getScope()))
                .collect(Collectors.toList());

        // IAM鉴权
        AppResourceScopeResult appResourceScopeResult =
            appAuthService.getAppResourceScopeList(username, appResourceScopeList);

        // 可用的普通业务
        List<Long> authorizedAppIdList = extractAuthorizedAppIdList(appResourceScopeResult);

        List<AppVO> finalAppList = new ArrayList<>();
        // 所有可用的AppId
        List<Long> availableAppIds = new ArrayList<>();
        if (appResourceScopeResult.getAny()) {
            for (ApplicationDTO app : appList) {
                AppVO appVO = new AppVO(app.getId(), app.getScope().getType().getValue(),
                    app.getScope().getId(), app.getName(), true, null, null);
                finalAppList.add(appVO);
                availableAppIds.add(app.getId());
            }
        } else {
            // 根据权限中心结果鉴权
            for (ApplicationDTO app : appList) {
                AppVO appVO = new AppVO(app.getId(), app.getScope().getType().getValue(),
                    app.getScope().getId(), app.getName(), true, null, null);
                appVO.setHasPermission(authorizedAppIdList.contains(app.getId()));
                finalAppList.add(appVO);
            }
        }
        // 设置收藏状态
        setFavorState(username, finalAppList);
        // 排序
        sortApps(finalAppList);
        // 分页
        PageData<AppVO> pageData = PageUtil.pageInMem(finalAppList, start, pageSize);
        PageDataWithAvailableIdList<AppVO, Long> pageDataWithAvailableIdList =
            new PageDataWithAvailableIdList<>(pageData, availableAppIds);
        return Response.buildSuccessResp(pageDataWithAvailableIdList);
    }

    @Override
    public Response<List<ScopeGroupWithAvailableScopeIdList>> listGroupedScopeWithFavor(String username) {
        // Mock数据
        List<ScopeGroupWithAvailableScopeIdList> list = new ArrayList<>();
        ScopeGroupWithAvailableScopeIdList group1 = new ScopeGroupWithAvailableScopeIdList();
        group1.setId(ResourceScopeTypeEnum.BIZ.getValue());
        group1.setName("业务");
        List<ScopeVO> children = new ArrayList<>();
        ScopeVO scopeVO = new ScopeVO();
        scopeVO.setId("1");
        scopeVO.setName("Mock业务1");
        scopeVO.setHasPermission(true);
        scopeVO.setFavor(true);
        scopeVO.setFavorTime(TimeUtil.parseIsoZonedTimeToMillis("2025-10-15T10:00:00.000+08:00"));
        children.add(scopeVO);
        scopeVO = new ScopeVO();
        scopeVO.setId("2");
        scopeVO.setName("Mock业务2");
        scopeVO.setHasPermission(true);
        scopeVO.setFavor(false);
        scopeVO.setFavorTime(null);
        children.add(scopeVO);
        scopeVO = new ScopeVO();
        scopeVO.setId("3");
        scopeVO.setName("Mock业务3");
        scopeVO.setHasPermission(false);
        scopeVO.setFavor(true);
        scopeVO.setFavorTime(TimeUtil.parseIsoZonedTimeToMillis("2025-10-15T12:00:00.000+08:00"));
        children.add(scopeVO);
        group1.setChildren(children);
        list.add(group1);

        ScopeGroupWithAvailableScopeIdList group2 = new ScopeGroupWithAvailableScopeIdList();
        group2.setId(ResourceScopeTypeEnum.BIZ_SET.getValue());
        group2.setName("业务集");
        children = new ArrayList<>();
        scopeVO = new ScopeVO();
        scopeVO.setId("1");
        scopeVO.setName("Mock业务集1");
        scopeVO.setHasPermission(true);
        scopeVO.setFavor(true);
        scopeVO.setFavorTime(TimeUtil.parseIsoZonedTimeToMillis("2025-10-15T09:00:00.000+08:00"));
        children.add(scopeVO);
        scopeVO = new ScopeVO();
        scopeVO.setId("2");
        scopeVO.setName("Mock业务集2");
        scopeVO.setHasPermission(true);
        scopeVO.setFavor(false);
        scopeVO.setFavorTime(null);
        children.add(scopeVO);
        scopeVO = new ScopeVO();
        scopeVO.setId("3");
        scopeVO.setName("Mock业务集3");
        scopeVO.setHasPermission(false);
        scopeVO.setFavor(true);
        scopeVO.setFavorTime(TimeUtil.parseIsoZonedTimeToMillis("2025-10-15T13:00:00.000+08:00"));
        children.add(scopeVO);
        group2.setChildren(children);
        list.add(group2);

        ScopeGroupWithAvailableScopeIdList group3 = new ScopeGroupWithAvailableScopeIdList();
        group3.setId("tenant_set");
        group3.setName("租户集");
        children = new ArrayList<>();
        scopeVO = new ScopeVO();
        scopeVO.setId("1");
        scopeVO.setName("Mock租户集1");
        scopeVO.setHasPermission(true);
        scopeVO.setFavor(false);
        scopeVO.setFavorTime(TimeUtil.parseIsoZonedTimeToMillis("2025-10-15T15:00:00.000+08:00"));
        children.add(scopeVO);
        group3.setChildren(children);
        list.add(group3);
        return Response.buildSuccessResp(list);
    }

    /**
     * 对Job业务进行排序
     *
     * @param appList Job业务列表
     */
    private void sortApps(List<AppVO> appList) {
        // 排序：有无权限、是否收藏、收藏时间倒序
        appList.sort((o1, o2) -> {
            int result = o2.getHasPermission().compareTo(o1.getHasPermission());
            if (result != 0) {
                return result;
            } else {
                result = CompareUtil.safeCompareNullFront(o2.getFavor(), o1.getFavor());
            }
            if (result != 0) {
                return result;
            } else {
                return CompareUtil.safeCompareNullFront(o2.getFavorTime(), o1.getFavorTime());
            }
        });
    }

    @Override
    public Response<Integer> favorApp(String username,
                                      AppResourceScope appResourceScope,
                                      String scopeType,
                                      String scopeId,
                                      FavorAppReq favorAppReq) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(
            favorAppReq.getScopeType(),
            favorAppReq.getScopeId()
        );
        return Response.buildSuccessResp(
            applicationFavorService.favorApp(username, applicationDTO.getId())
        );
    }

    @Override
    public Response<Integer> cancelFavorApp(String username,
                                            AppResourceScope appResourceScope,
                                            String scopeType,
                                            String scopeId,
                                            FavorAppReq favorAppReq) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(
            favorAppReq.getScopeType(),
            favorAppReq.getScopeId()
        );
        return Response.buildSuccessResp(
            applicationFavorService.cancelFavorApp(username, applicationDTO.getId())
        );
    }
}
