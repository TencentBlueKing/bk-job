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

package com.tencent.bk.job.manage.auth.impl;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行方案相关操作鉴权接口
 */
@Service
public class PlanAuthServiceImpl implements PlanAuthService {

    private final AuthService authService;
    private final AppAuthService appAuthService;

    @Autowired
    public PlanAuthServiceImpl(AuthService authService,
                               AppAuthService appAuthService) {
        this.authService = authService;
        this.appAuthService = appAuthService;
    }

    private PathInfoDTO buildAppScopePath(AppResourceScope appResourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).build();
    }

    private PathInfoDTO buildAppScopeResourcePath(AppResourceScope appResourceScope,
                                                  String resourceId) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).child(ResourceTypeEnum.TEMPLATE.getId(), resourceId).build();
    }

    @Override
    public AuthResult authCreateJobPlan(String username,
                                        AppResourceScope appResourceScope,
                                        Long jobTemplateId,
                                        String jobTemplateName) {
        return authService.auth(true, username, ActionId.CREATE_JOB_PLAN, ResourceTypeEnum.TEMPLATE,
            jobTemplateId.toString(), buildAppScopePath(appResourceScope));
    }

    @Override
    public AuthResult authViewJobPlan(String username,
                                      AppResourceScope appResourceScope,
                                      Long jobTemplateId,
                                      Long jobPlanId,
                                      String jobPlanName) {
        return authService.auth(true, username, ActionId.VIEW_JOB_PLAN, ResourceTypeEnum.PLAN,
            jobPlanId.toString(), buildAppScopeResourcePath(appResourceScope, jobTemplateId.toString()));
    }

    @Override
    public AuthResult authEditJobPlan(String username,
                                      AppResourceScope appResourceScope,
                                      Long jobTemplateId,
                                      Long jobPlanId,
                                      String jobPlanName) {
        return authService.auth(true, username, ActionId.EDIT_JOB_PLAN, ResourceTypeEnum.PLAN,
            jobPlanId.toString(), buildAppScopeResourcePath(appResourceScope, jobTemplateId.toString()));
    }

    @Override
    public AuthResult authDeleteJobPlan(String username,
                                        AppResourceScope appResourceScope,
                                        Long jobTemplateId,
                                        Long jobPlanId,
                                        String jobPlanName) {
        return authService.auth(true, username, ActionId.DELETE_JOB_PLAN, ResourceTypeEnum.PLAN,
            jobPlanId.toString(), buildAppScopeResourcePath(appResourceScope, jobTemplateId.toString()));
    }

    @Override
    public AuthResult authSyncJobPlan(String username,
                                      AppResourceScope appResourceScope,
                                      Long jobTemplateId,
                                      Long jobPlanId,
                                      String jobPlanName) {
        return authService.auth(true, username, ActionId.SYNC_JOB_PLAN, ResourceTypeEnum.PLAN,
            jobPlanId.toString(), buildAppScopeResourcePath(appResourceScope, jobTemplateId.toString()));
    }

    private List<PermissionResource> buildPlanPermissionResource(AppResourceScope appResourceScope,
                                                                 List<Long> jobTemplateIdList,
                                                                 List<Long> jobPlanIdList) {
        List<PermissionResource> planPermissionResourceList = new ArrayList<>();
        for (int i = 0; i < jobPlanIdList.size(); i++) {
            Long planId = jobPlanIdList.get(i);
            Long templateId = jobTemplateIdList.get(i);
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(planId.toString());
            resource.setResourceType(ResourceTypeEnum.PLAN);
            resource.setPathInfo(buildAppScopeResourcePath(appResourceScope, templateId.toString()));
            planPermissionResourceList.add(resource);
        }
        return planPermissionResourceList;
    }

    @Override
    public List<Long> batchAuthViewJobPlan(String username,
                                           AppResourceScope appResourceScope,
                                           List<Long> jobTemplateIdList,
                                           List<Long> jobPlanIdList) {
        List<PermissionResource> planPermissionResourceList = buildPlanPermissionResource(
            appResourceScope, jobTemplateIdList, jobPlanIdList);
        List<String> allowedPlanIdList = appAuthService.batchAuth(username, ActionId.VIEW_JOB_PLAN, appResourceScope,
            planPermissionResourceList).parallelStream().collect(Collectors.toList());
        return allowedPlanIdList.parallelStream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<Long> batchAuthEditJobPlan(String username,
                                           AppResourceScope appResourceScope,
                                           List<Long> jobTemplateIdList,
                                           List<Long> jobPlanIdList) {
        List<PermissionResource> planPermissionResourceList = buildPlanPermissionResource(
            appResourceScope, jobTemplateIdList, jobPlanIdList);
        List<String> allowedPlanIdList = appAuthService.batchAuth(username, ActionId.EDIT_JOB_PLAN, appResourceScope,
            planPermissionResourceList).parallelStream().collect(Collectors.toList());
        return allowedPlanIdList.parallelStream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<Long> batchAuthDeleteJobPlan(String username,
                                             AppResourceScope appResourceScope,
                                             List<Long> jobTemplateIdList,
                                             List<Long> jobPlanIdList) {
        List<PermissionResource> planPermissionResourceList = buildPlanPermissionResource(
            appResourceScope, jobTemplateIdList, jobPlanIdList);
        List<String> allowedPlanIdList = appAuthService.batchAuth(username, ActionId.DELETE_JOB_PLAN, appResourceScope,
            planPermissionResourceList).parallelStream().collect(Collectors.toList());
        return allowedPlanIdList.parallelStream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public boolean registerPlan(Long id, String name, String creator) {
        return authService.registerResource(id.toString(), name, ResourceTypeId.PLAN, creator, null);
    }
}
