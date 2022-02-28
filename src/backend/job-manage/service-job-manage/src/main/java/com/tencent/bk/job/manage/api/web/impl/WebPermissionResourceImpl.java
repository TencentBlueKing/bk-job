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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.app.AppTransferService;
import com.tencent.bk.job.common.app.Scope;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.manage.api.web.WebPermissionResource;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.web.request.OperationPermissionReq;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class WebPermissionResourceImpl implements WebPermissionResource {
    private final WebAuthService authService;

    private final TaskPlanService taskPlanService;

    private final TaskTemplateService taskTemplateService;

    private final AppTransferService appTransferService;

    public WebPermissionResourceImpl(WebAuthService authService, TaskPlanService taskPlanService,
                                     TaskTemplateService taskTemplateService,
                                     AppTransferService appTransferService) {
        this.authService = authService;
        this.taskPlanService = taskPlanService;
        this.taskTemplateService = taskTemplateService;
        this.appTransferService = appTransferService;
    }

    @Override
    public Response<String> getApplyUrl(String username, OperationPermissionReq req) {
        return null;
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(String username, OperationPermissionReq req) {
        return checkOperationPermission(username, req.getAppId(), req.getScopeType(), req.getScopeId(),
            req.getOperation(), req.getResourceId(),
            req.isReturnPermissionDetail());
    }

    private boolean validateNum(String str) {
        try {
            Long.valueOf(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private PathInfoDTO buildScopePathInfo(Scope scope) {
        return PathBuilder.newBuilder(scope.getType(), scope.getId()).build();
    }

    private PathInfoDTO buildTaskPlanPathInfo(Scope scope, Long templateId) {
        return PathBuilder.newBuilder(scope.getType(), scope.getId())
            .child(ResourceTypeEnum.TEMPLATE.getId(), templateId.toString())
            .build();
    }

    private Response<AuthResultVO> checkScriptOperationPermission(
        String username, Scope scope,
        String action, String resourceId,
        boolean isReturnApplyUrl) {
        if (scope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_SCRIPT, ResourceTypeEnum.BUSINESS,
                    scope.getId(), buildScopePathInfo(scope)));
            case "view":
            case "execute":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.VIEW_SCRIPT, ResourceTypeEnum.SCRIPT, resourceId, buildScopePathInfo(scope)));
            case "edit":
            case "delete":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_SCRIPT, ResourceTypeEnum.SCRIPT, resourceId, buildScopePathInfo(scope)));
            case "clone":
                List<PermissionActionResource> actionResources = new ArrayList<>(1);
                PermissionActionResource manageScriptActionResource = new PermissionActionResource();
                manageScriptActionResource.setActionId(ActionId.MANAGE_SCRIPT);
                manageScriptActionResource.addResource(
                    ResourceTypeEnum.SCRIPT,
                    resourceId,
                    buildScopePathInfo(scope)
                );
                actionResources.add(manageScriptActionResource);
                return Response.buildSuccessResp(
                    authService.auth(isReturnApplyUrl, username, actionResources)
                );
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkPublicScriptOperationPermission(
        String username,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        switch (action) {
            case "create":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_PUBLIC_SCRIPT));
            case "view":
            case "execute":
                return Response.buildSuccessResp(AuthResultVO.pass());
            case "edit":
            case "delete":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE, ResourceTypeEnum.PUBLIC_SCRIPT, resourceId, null));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkJobTemplateOperationPermission(
        String username,
        Scope scope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (scope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_JOB_TEMPLATE, ResourceTypeEnum.BUSINESS, scope.getId(), buildScopePathInfo(scope)));
            case "view":
            case "debug":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.VIEW_JOB_TEMPLATE, ResourceTypeEnum.TEMPLATE, resourceId, buildScopePathInfo(scope)));
            case "edit":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.EDIT_JOB_TEMPLATE, ResourceTypeEnum.TEMPLATE, resourceId, buildScopePathInfo(scope)));
            case "delete":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.DELETE_JOB_TEMPLATE, ResourceTypeEnum.TEMPLATE, resourceId, buildScopePathInfo(scope)));
            case "clone":
                List<PermissionActionResource> actionResources = new ArrayList<>(2);
                PermissionActionResource viewTemplateActionResource = new PermissionActionResource();
                viewTemplateActionResource.setActionId(ActionId.VIEW_JOB_TEMPLATE);
                viewTemplateActionResource.addResource(ResourceTypeEnum.TEMPLATE, resourceId,
                    buildScopePathInfo(scope));
                PermissionActionResource createTemplateActionResource = new PermissionActionResource();
                createTemplateActionResource.setActionId(ActionId.CREATE_JOB_TEMPLATE);
                createTemplateActionResource.addResource(ResourceTypeEnum.BUSINESS, scope.getId(),
                    buildScopePathInfo(scope));
                actionResources.add(viewTemplateActionResource);
                actionResources.add(createTemplateActionResource);
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username, actionResources));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkJobPlanOperationPermission(
        String username,
        Scope scope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (scope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        if (StringUtils.isEmpty(resourceId) || !validateNum(resourceId)) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        TaskTemplateInfoDTO jobTemplate;
        TaskPlanInfoDTO plan;
        Long appId = appTransferService.getAppIdByScope(scope);
        switch (action) {
            case "create":
                Long templateId = Long.valueOf(resourceId);
                jobTemplate = taskTemplateService.getTaskTemplateBasicInfoById(appId, templateId);
                if (jobTemplate == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_JOB_PLAN, ResourceTypeEnum.TEMPLATE, templateId.toString(),
                    buildTaskPlanPathInfo(scope, templateId)));
            case "view":
            case "execute":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.VIEW_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(scope,
                        plan.getTemplateId())));
            case "edit":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.EDIT_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(scope,
                        plan.getTemplateId())));
            case "delete":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.DELETE_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(scope,
                        plan.getTemplateId())));
            case "sync":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.SYNC_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(scope,
                        plan.getTemplateId())));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkAccountOperationPermission(
        String username,
        Scope scope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (scope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_ACCOUNT, ResourceTypeEnum.BUSINESS, scope.getId(), buildScopePathInfo(scope)));
            case "view":
            case "edit":
            case "delete":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_ACCOUNT, ResourceTypeEnum.ACCOUNT, resourceId, buildScopePathInfo(scope)));
            case "use":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.USE_ACCOUNT, ResourceTypeEnum.ACCOUNT, resourceId, buildScopePathInfo(scope)));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkTagOperationPermission(
        String username,
        Scope scope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (scope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_TAG, ResourceTypeEnum.BUSINESS, scope.getId(), buildScopePathInfo(scope)));
            case "edit":
            case "delete":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_TAG, ResourceTypeEnum.TAG, resourceId, buildScopePathInfo(scope)));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkTicketOperationPermission(
        String username,
        Scope scope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (scope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "use":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.USE_TICKET, ResourceTypeEnum.TICKET, resourceId, buildScopePathInfo(scope)));
            case "create":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_TICKET, ResourceTypeEnum.BUSINESS, scope.getId(), buildScopePathInfo(scope)));
            case "edit":
            case "delete":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_TICKET, ResourceTypeEnum.TICKET, resourceId, buildScopePathInfo(scope)));
            default:
                log.error("Unknown operator|{}|{}|{}|{}", username, scope, action, resourceId);
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkWhiteIPOperationPermission(
        String username,
        String action,
        boolean isReturnApplyUrl
    ) {
        switch (action) {
            case "create":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_WHITELIST));
            case "view":
            case "edit":
            case "delete":
                return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_WHITELIST));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Scope getScope(Long bizId, String scopeType, String scopeId) {
        if (StringUtils.isNotBlank(scopeType) && StringUtils.isNotBlank(scopeId)) {
            return new Scope(scopeType, scopeId);
        } else if (bizId != null) {
            return new Scope(ResourceId.BIZ, bizId.toString());
        }
        return null;
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(
        String username,
        Long bizId,
        String scopeType,
        String scopeId,
        String operation,
        String resourceId,
        Boolean returnPermissionDetail
    ) {
        if (StringUtils.isEmpty(operation)) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        String[] resourceAndAction = operation.split("/");
        if (resourceAndAction.length != 2) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        String resourceType = resourceAndAction[0];
        String action = resourceAndAction[1];
        Scope scope = getScope(bizId, scopeType, scopeId);
        boolean isReturnApplyUrl = returnPermissionDetail == null ? false : returnPermissionDetail;

        switch (resourceType) {
            case "biz":
                if ("access_business".equals(action)) {
                    return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                        ActionId.LIST_BUSINESS, ResourceTypeEnum.BUSINESS, resourceId, null));
                }
                break;
            case "script":
                return checkScriptOperationPermission(username, scope, action, resourceId, isReturnApplyUrl);
            case "public_script":
                return checkPublicScriptOperationPermission(username, action, resourceId, isReturnApplyUrl);
            case "job_template":
                return checkJobTemplateOperationPermission(username, scope, action, resourceId, isReturnApplyUrl);
            case "job_plan":
                return checkJobPlanOperationPermission(username, scope, action, resourceId, isReturnApplyUrl);
            case "account":
                return checkAccountOperationPermission(username, scope, action, resourceId, isReturnApplyUrl);
            case "whitelist":
                return checkWhiteIPOperationPermission(username, action, isReturnApplyUrl);
            case "tag":
                return checkTagOperationPermission(username, scope, action, resourceId, isReturnApplyUrl);
            case "ticket":
                return checkTicketOperationPermission(username, scope, action, resourceId, isReturnApplyUrl);
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }
}
