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

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.manage.api.web.WebAppResource;
import com.tencent.bk.job.manage.model.dto.UserAppScopeDTO;
import com.tencent.bk.job.manage.model.web.request.app.FavorAppReq;
import com.tencent.bk.job.manage.model.web.vo.AppVO;
import com.tencent.bk.job.manage.model.web.vo.ScopeGroupPanel;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.impl.ApplicationFavorService;
import com.tencent.bk.job.manage.service.scope.ScopePanelService;
import com.tencent.bk.job.manage.service.scope.UserAppScopeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebAppResourceImpl implements WebAppResource {

    private final ApplicationService applicationService;
    private final ApplicationFavorService applicationFavorService;
    private final ScopePanelService scopePanelService;
    private final UserAppScopeQueryService userAppScopeQueryService;

    @Autowired
    public WebAppResourceImpl(ApplicationService applicationService,
                              ApplicationFavorService applicationFavorService,
                              ScopePanelService scopePanelService,
                              UserAppScopeQueryService userAppScopeQueryService) {
        this.applicationService = applicationService;
        this.applicationFavorService = applicationFavorService;
        this.scopePanelService = scopePanelService;
        this.userAppScopeQueryService = userAppScopeQueryService;
    }

    @Override
    public Response<PageData<AppVO>> listPagedAppWithFavor(String username,
                                                           Integer start,
                                                           Integer pageSize) {
        List<AppVO> finalAppList = listSortedAppWithFavor(username);
        PageData<AppVO> pageData = PageUtil.pageInMem(finalAppList, start, pageSize);
        return Response.buildSuccessResp(pageData);
    }

    @Override
    public Response<ScopeGroupPanel> getScopeGroupPanel(String username) {
        List<AppVO> finalAppList = listSortedAppWithFavor(username);
        ScopeGroupPanel scopeGroupPanel = scopePanelService.buildScopeGroupPanel(username, finalAppList);
        return Response.buildSuccessResp(scopeGroupPanel);
    }

    private List<AppVO> listSortedAppWithFavor(String username) {
        return userAppScopeQueryService.listSortedAppWithFavor(username).stream()
            .map(this::toAppVO)
            .collect(Collectors.toList());
    }

    private AppVO toAppVO(UserAppScopeDTO scope) {
        return new AppVO(
            scope.getAppId(),
            scope.getScopeType(),
            scope.getScopeId(),
            scope.isBuiltIn(),
            scope.isAllBizSet(),
            scope.getName(),
            scope.getHasPermission(),
            scope.getTimeZone(),
            scope.getFavor(),
            scope.getFavorTime()
        );
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
