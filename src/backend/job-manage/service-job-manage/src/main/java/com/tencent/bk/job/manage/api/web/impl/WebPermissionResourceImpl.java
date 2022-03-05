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
import com.tencent.bk.job.common.app.ResourceScope;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.manage.api.web.WebPermissionResource;
import com.tencent.bk.job.manage.auth.AccountAuthService;
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
    private final WebAuthService webAuthService;

    private final TaskPlanService taskPlanService;

    private final TaskTemplateService taskTemplateService;

    private final AppTransferService appTransferService;

    private final AccountAuthService accountAuthService;

    public WebPermissionResourceImpl(WebAuthService webAuthService, TaskPlanService taskPlanService,
                                     TaskTemplateService taskTemplateService,
                                     AppTransferService appTransferService,
                                     AccountAuthService accountAuthService) {
        this.webAuthService = webAuthService;
        this.taskPlanService = taskPlanService;
        this.taskTemplateService = taskTemplateService;
        this.appTransferService = appTransferService;
        this.accountAuthService = accountAuthService;
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

    private PathInfoDTO buildTaskPlanPathInfo(ResourceScope resourceScope, Long templateId) {
        return PathBuilder.newBuilder(
            IamUtil.getIamResourceTypeIdForResourceScope(resourceScope), resourceScope.getId())
            .child(ResourceTypeEnum.TEMPLATE.getId(), templateId.toString())
            .build();
    }

    private Response<AuthResultVO> checkScriptOperationPermission(
        String username, ResourceScope resourceScope,
        String action, String resourceId,
        boolean isReturnApplyUrl) {
        if (resourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username, ActionId.CREATE_SCRIPT,
                    ResourceTypeEnum.BUSINESS, resourceScope.getId(), IamUtil.buildScopePathInfo(resourceScope)));
            case "view":
            case "execute":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.VIEW_SCRIPT, ResourceTypeEnum.SCRIPT, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)));
            case "edit":
            case "delete":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_SCRIPT, ResourceTypeEnum.SCRIPT, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)));
            case "clone":
                List<PermissionActionResource> actionResources = new ArrayList<>(1);
                PermissionActionResource manageScriptActionResource = new PermissionActionResource();
                manageScriptActionResource.setActionId(ActionId.MANAGE_SCRIPT);
                manageScriptActionResource.addResource(
                    ResourceTypeEnum.SCRIPT,
                    resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)
                );
                actionResources.add(manageScriptActionResource);
                return Response.buildSuccessResp(
                    webAuthService.auth(isReturnApplyUrl, username, actionResources)
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
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_PUBLIC_SCRIPT));
            case "view":
            case "execute":
                return Response.buildSuccessResp(AuthResultVO.pass());
            case "edit":
            case "delete":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE, ResourceTypeEnum.PUBLIC_SCRIPT, resourceId, null));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkJobTemplateOperationPermission(
        String username,
        ResourceScope resourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (resourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_JOB_TEMPLATE, ResourceTypeEnum.BUSINESS, resourceScope
                        .getId(), IamUtil.buildScopePathInfo(resourceScope)));
            case "view":
            case "debug":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.VIEW_JOB_TEMPLATE, ResourceTypeEnum.TEMPLATE, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)));
            case "edit":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.EDIT_JOB_TEMPLATE, ResourceTypeEnum.TEMPLATE, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)));
            case "delete":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.DELETE_JOB_TEMPLATE, ResourceTypeEnum.TEMPLATE, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)));
            case "clone":
                List<PermissionActionResource> actionResources = new ArrayList<>(2);
                PermissionActionResource viewTemplateActionResource = new PermissionActionResource();
                viewTemplateActionResource.setActionId(ActionId.VIEW_JOB_TEMPLATE);
                viewTemplateActionResource.addResource(ResourceTypeEnum.TEMPLATE, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope));
                PermissionActionResource createTemplateActionResource = new PermissionActionResource();
                createTemplateActionResource.setActionId(ActionId.CREATE_JOB_TEMPLATE);
                createTemplateActionResource.addResource(ResourceTypeEnum.BUSINESS, resourceScope.getId(),
                    IamUtil.buildScopePathInfo(resourceScope));
                actionResources.add(viewTemplateActionResource);
                actionResources.add(createTemplateActionResource);
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username, actionResources));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkJobPlanOperationPermission(
        String username,
        ResourceScope resourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (resourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        if (StringUtils.isEmpty(resourceId) || !validateNum(resourceId)) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        TaskTemplateInfoDTO jobTemplate;
        TaskPlanInfoDTO plan;
        Long appId = appTransferService.getAppIdByScope(resourceScope);
        switch (action) {
            case "create":
                Long templateId = Long.valueOf(resourceId);
                jobTemplate = taskTemplateService.getTaskTemplateBasicInfoById(appId, templateId);
                if (jobTemplate == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_JOB_PLAN, ResourceTypeEnum.TEMPLATE, templateId.toString(),
                    buildTaskPlanPathInfo(resourceScope, templateId)));
            case "view":
            case "execute":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.VIEW_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(resourceScope,
                        plan.getTemplateId())));
            case "edit":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.EDIT_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(resourceScope,
                        plan.getTemplateId())));
            case "delete":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.DELETE_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(resourceScope,
                        plan.getTemplateId())));
            case "sync":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.SYNC_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(resourceScope,
                        plan.getTemplateId())));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkAccountOperationPermission(
        String username,
        ResourceScope resourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (resourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                log.info("before transfer, scope={}", resourceScope);
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(accountAuthService.authCreateAccount(username, resourceScope)));
            case "view":
            case "edit":
            case "delete":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_ACCOUNT, ResourceTypeEnum.ACCOUNT, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)));
            case "use":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.USE_ACCOUNT, ResourceTypeEnum.ACCOUNT, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkTagOperationPermission(
        String username,
        ResourceScope resourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (resourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_TAG, ResourceTypeEnum.BUSINESS, resourceScope.getId(),
                    IamUtil.buildScopePathInfo(resourceScope)));
            case "edit":
            case "delete":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_TAG, ResourceTypeEnum.TAG, resourceId, IamUtil.buildScopePathInfo(resourceScope)));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkTicketOperationPermission(
        String username,
        ResourceScope resourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (resourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "use":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.USE_TICKET, ResourceTypeEnum.TICKET, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)));
            case "create":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_TICKET, ResourceTypeEnum.BUSINESS, resourceScope
                        .getId(), IamUtil.buildScopePathInfo(resourceScope)));
            case "edit":
            case "delete":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_TICKET, ResourceTypeEnum.TICKET, resourceId,
                    IamUtil.buildScopePathInfo(resourceScope)));
            default:
                log.error("Unknown operator|{}|{}|{}|{}", username, resourceScope, action, resourceId);
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
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_WHITELIST));
            case "view":
            case "edit":
            case "delete":
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_WHITELIST));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private ResourceScope getScope(Long appId, String scopeType, String scopeId) {
        if (StringUtils.isNotBlank(scopeType) && StringUtils.isNotBlank(scopeId)) {
            return new ResourceScope(scopeType, scopeId);
        } else if (appId != null) {
            return new ResourceScope(appId);
        }
        return null;
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(
        String username,
        Long appId,
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
        ResourceScope resourceScope = getScope(appId, scopeType, scopeId);
        boolean isReturnApplyUrl = returnPermissionDetail == null ? false : returnPermissionDetail;

        switch (resourceType) {
            case "biz":
                if ("access_business".equals(action)) {
                    return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username,
                        ActionId.ACCESS_BUSINESS, ResourceTypeEnum.BUSINESS, resourceId, null));
                }
                break;
            case "script":
                return checkScriptOperationPermission(username, resourceScope, action, resourceId, isReturnApplyUrl);
            case "public_script":
                return checkPublicScriptOperationPermission(username, action, resourceId, isReturnApplyUrl);
            case "job_template":
                return checkJobTemplateOperationPermission(username, resourceScope, action, resourceId,
                    isReturnApplyUrl);
            case "job_plan":
                return checkJobPlanOperationPermission(username, resourceScope, action, resourceId, isReturnApplyUrl);
            case "account":
                return checkAccountOperationPermission(username, resourceScope, action, resourceId, isReturnApplyUrl);
            case "whitelist":
                return checkWhiteIPOperationPermission(username, action, isReturnApplyUrl);
            case "tag":
                return checkTagOperationPermission(username, resourceScope, action, resourceId, isReturnApplyUrl);
            case "ticket":
                return checkTicketOperationPermission(username, resourceScope, action, resourceId, isReturnApplyUrl);
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }
}
