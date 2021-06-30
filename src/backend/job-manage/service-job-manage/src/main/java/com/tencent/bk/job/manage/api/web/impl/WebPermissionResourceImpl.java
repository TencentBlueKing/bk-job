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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.ServiceResponse;
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

    private final MessageI18nService i18nService;

    public WebPermissionResourceImpl(WebAuthService authService, TaskPlanService taskPlanService,
                                     TaskTemplateService taskTemplateService, MessageI18nService i18nService) {
        this.authService = authService;
        this.taskPlanService = taskPlanService;
        this.taskTemplateService = taskTemplateService;
        this.i18nService = i18nService;
    }

    @Override
    public ServiceResponse<String> getApplyUrl(String username, OperationPermissionReq req) {
        return null;
    }

    @Override
    public ServiceResponse<AuthResultVO> checkOperationPermission(String username, OperationPermissionReq req) {
        return checkOperationPermission(username, req.getAppId(), req.getOperation(), req.getResourceId(),
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

    private PathInfoDTO buildAppPathInfo(String appId) {
        return PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId).build();
    }

    private PathInfoDTO buildTaskPlanPathInfo(String appId, Long templateId) {
        return PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId)
            .child(ResourceTypeEnum.TEMPLATE.getId(), templateId.toString())
            .build();
    }

    private ServiceResponse<AuthResultVO> checkScriptOperationPermission(
        String username, String appIdStr,
        String action, String resourceId,
        boolean isReturnApplyUrl) {
        if (appIdStr == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId", "appId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_SCRIPT, ResourceTypeEnum.BUSINESS, appIdStr, null));
            case "view":
            case "execute":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.VIEW_SCRIPT, ResourceTypeEnum.SCRIPT, resourceId, buildAppPathInfo(appIdStr)));
            case "edit":
            case "delete":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_SCRIPT, ResourceTypeEnum.SCRIPT, resourceId, buildAppPathInfo(appIdStr)));
            case "clone":
                List<PermissionActionResource> actionResources = new ArrayList<>(1);
                PermissionActionResource manageScriptActionResource = new PermissionActionResource();
                manageScriptActionResource.setActionId(ActionId.MANAGE_SCRIPT);
                manageScriptActionResource.addResource(
                    ResourceTypeEnum.SCRIPT,
                    resourceId,
                    buildAppPathInfo(appIdStr)
                );
                actionResources.add(manageScriptActionResource);
                return ServiceResponse.buildSuccessResp(
                    authService.auth(isReturnApplyUrl, username, actionResources)
                );
        }
        return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
    }

    private ServiceResponse<AuthResultVO> checkPublicScriptOperationPermission(
        String username,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        switch (action) {
            case "create":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_PUBLIC_SCRIPT));
            case "view":
            case "execute":
                return ServiceResponse.buildSuccessResp(AuthResultVO.pass());
            case "edit":
            case "delete":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE, ResourceTypeEnum.PUBLIC_SCRIPT, resourceId, null));
        }
        return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
    }

    private ServiceResponse<AuthResultVO> checkJobTemplateOperationPermission(
        String username,
        String appIdStr,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (appIdStr == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId", "appId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_JOB_TEMPLATE, ResourceTypeEnum.BUSINESS, appIdStr, null));
            case "view":
            case "debug":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.VIEW_JOB_TEMPLATE, ResourceTypeEnum.TEMPLATE, resourceId, buildAppPathInfo(appIdStr)));
            case "edit":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.EDIT_JOB_TEMPLATE, ResourceTypeEnum.TEMPLATE, resourceId, buildAppPathInfo(appIdStr)));
            case "delete":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.DELETE_JOB_TEMPLATE, ResourceTypeEnum.TEMPLATE, resourceId, buildAppPathInfo(appIdStr)));
            case "clone":
                List<PermissionActionResource> actionResources = new ArrayList<>(2);
                PermissionActionResource viewTemplateActionResource = new PermissionActionResource();
                viewTemplateActionResource.setActionId(ActionId.VIEW_JOB_TEMPLATE);
                viewTemplateActionResource.addResource(ResourceTypeEnum.TEMPLATE, resourceId,
                    buildAppPathInfo(appIdStr));
                PermissionActionResource createTemplateActionResource = new PermissionActionResource();
                createTemplateActionResource.setActionId(ActionId.CREATE_JOB_TEMPLATE);
                createTemplateActionResource.addResource(ResourceTypeEnum.BUSINESS, appIdStr, null);
                actionResources.add(viewTemplateActionResource);
                actionResources.add(createTemplateActionResource);
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username, actionResources));
        }
        return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
    }

    private ServiceResponse<AuthResultVO> checkJobPlanOperationPermission(
        String username,
        Long appId,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        String appIdStr = appId == null ? null : appId.toString();
        if (appIdStr == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId", "appId cannot be null or empty"});
        }
        if (StringUtils.isEmpty(resourceId) || !validateNum(resourceId)) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        TaskTemplateInfoDTO jobTemplate = null;
        TaskPlanInfoDTO plan = null;
        switch (action) {
            case "create":
                Long templateId = Long.valueOf(resourceId);
                jobTemplate = taskTemplateService.getTaskTemplateBasicInfoById(appId, templateId);
                if (jobTemplate == null) {
                    return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
                }
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_JOB_PLAN, ResourceTypeEnum.TEMPLATE, templateId.toString(),
                    buildTaskPlanPathInfo(appIdStr, templateId)));
            case "view":
            case "execute":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
                }
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.VIEW_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(appIdStr,
                        plan.getTemplateId())));
            case "edit":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
                }
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.EDIT_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(appIdStr,
                        plan.getTemplateId())));
            case "delete":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
                }
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.DELETE_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(appIdStr,
                        plan.getTemplateId())));
            case "sync":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
                }
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.SYNC_JOB_PLAN, ResourceTypeEnum.PLAN, resourceId, buildTaskPlanPathInfo(appIdStr,
                        plan.getTemplateId())));
        }
        return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
    }

    private ServiceResponse<AuthResultVO> checkAccountOperationPermission(
        String username,
        String appIdStr,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (appIdStr == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId", "appId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_ACCOUNT, ResourceTypeEnum.BUSINESS, appIdStr, null));
            case "view":
            case "edit":
            case "delete":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_ACCOUNT, ResourceTypeEnum.ACCOUNT, resourceId, buildAppPathInfo(appIdStr)));
            case "use":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.USE_ACCOUNT, ResourceTypeEnum.ACCOUNT, resourceId, buildAppPathInfo(appIdStr)));
        }
        return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
    }

    private ServiceResponse<AuthResultVO> checkTagOperationPermission(
        String username,
        String appIdStr,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (appIdStr == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId", "appId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.CREATE_TAG, ResourceTypeEnum.BUSINESS, appIdStr, null));
            case "edit":
            case "delete":
                return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                    ActionId.MANAGE_TAG, ResourceTypeEnum.TAG, resourceId, buildAppPathInfo(appIdStr)));
        }
        return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
    }

    @Override
    public ServiceResponse<AuthResultVO> checkOperationPermission(
        String username,
        Long appId,
        String operation,
        String resourceId,
        Boolean returnPermissionDetail
    ) {
        if (StringUtils.isEmpty(operation)) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        String[] resourceAndAction = operation.split("/");
        if (resourceAndAction.length != 2) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        String resourceType = resourceAndAction[0];
        String action = resourceAndAction[1];
        String appIdStr = appId == null ? null : appId.toString();
        boolean isReturnApplyUrl = returnPermissionDetail == null ? false : returnPermissionDetail;

        switch (resourceType) {
            case "biz":
                switch (action) {
                    case "access_business":
                        return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                            ActionId.LIST_BUSINESS, ResourceTypeEnum.BUSINESS, resourceId, null));
                }
                break;
            case "script":
                return checkScriptOperationPermission(username, appIdStr, action, resourceId, isReturnApplyUrl);
            case "public_script":
                return checkPublicScriptOperationPermission(username, action, resourceId, isReturnApplyUrl);
            case "job_template":
                return checkJobTemplateOperationPermission(username, appIdStr, action, resourceId, isReturnApplyUrl);
            case "job_plan":
                return checkJobPlanOperationPermission(username, appId, action, resourceId, isReturnApplyUrl);
            case "account":
                return checkAccountOperationPermission(username, appIdStr, action, resourceId, isReturnApplyUrl);
            case "whitelist":
                switch (action) {
                    case "create":
                        return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                            ActionId.CREATE_WHITELIST));
                    case "view":
                    case "edit":
                    case "delete":
                        return ServiceResponse.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                            ActionId.MANAGE_WHITELIST));
                }
                break;
            case "tag":
                return checkTagOperationPermission(username, appIdStr, action, resourceId, isReturnApplyUrl);
        }
        return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
    }
}
