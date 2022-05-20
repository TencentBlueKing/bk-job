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
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.manage.api.web.WebPermissionResource;
import com.tencent.bk.job.manage.auth.AccountAuthService;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.job.manage.auth.ScriptAuthService;
import com.tencent.bk.job.manage.auth.TagAuthService;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.auth.TicketAuthService;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.web.request.OperationPermissionReq;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class WebPermissionResourceImpl implements WebPermissionResource {
    private final WebAuthService webAuthService;

    private final BusinessAuthService businessAuthService;

    private final TaskPlanService taskPlanService;

    private final TaskTemplateService taskTemplateService;

    private final ApplicationService applicationService;

    private final AccountAuthService accountAuthService;

    private final TagAuthService tagAuthService;

    private final ScriptAuthService scriptAuthService;

    private final TemplateAuthService templateAuthService;

    private final PlanAuthService planAuthService;

    private final TicketAuthService ticketAuthService;

    private final NoResourceScopeAuthService noResourceScopeAuthService;

    public WebPermissionResourceImpl(WebAuthService webAuthService,
                                     BusinessAuthService businessAuthService,
                                     TaskPlanService taskPlanService,
                                     TaskTemplateService taskTemplateService,
                                     ApplicationService applicationService,
                                     AccountAuthService accountAuthService,
                                     TagAuthService tagAuthService,
                                     ScriptAuthService scriptAuthService,
                                     TemplateAuthService templateAuthService,
                                     PlanAuthService planAuthService,
                                     TicketAuthService ticketAuthService,
                                     NoResourceScopeAuthService noResourceScopeAuthService) {
        this.webAuthService = webAuthService;
        this.businessAuthService = businessAuthService;
        this.taskPlanService = taskPlanService;
        this.taskTemplateService = taskTemplateService;
        this.applicationService = applicationService;
        this.accountAuthService = accountAuthService;
        this.tagAuthService = tagAuthService;
        this.scriptAuthService = scriptAuthService;
        this.templateAuthService = templateAuthService;
        this.planAuthService = planAuthService;
        this.ticketAuthService = ticketAuthService;
        this.noResourceScopeAuthService = noResourceScopeAuthService;
    }

    @Override
    public Response<String> getApplyUrl(String username, OperationPermissionReq req) {
        return null;
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(String username, OperationPermissionReq req) {
        return checkOperationPermission(username, req.getScopeType(), req.getScopeId(),
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

    private Response<AuthResultVO> checkScriptOperationPermission(String username,
                                                                  AppResourceScope appResourceScope,
                                                                  String action,
                                                                  String resourceId,
                                                                  boolean isReturnApplyUrl) {
        if (appResourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        scriptAuthService.authCreateScript(username, appResourceScope)
                    )
                );
            case "view":
            case "execute":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        scriptAuthService.authViewScript(username, appResourceScope, resourceId, null)
                    )
                );
            case "edit":
            case "delete":
            case "clone":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        scriptAuthService.authManageScript(username, appResourceScope, resourceId, null)
                    )
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
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        noResourceScopeAuthService.authCreatePublicScript(username)
                    )
                );
            case "view":
            case "execute":
                return Response.buildSuccessResp(AuthResultVO.pass());
            case "edit":
            case "delete":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        noResourceScopeAuthService.authManagePublicScript(username, resourceId)
                    )
                );
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkJobTemplateOperationPermission(
        String username,
        AppResourceScope appResourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (appResourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        Long templateId;
        switch (action) {
            case "create":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        templateAuthService.authCreateJobTemplate(username, appResourceScope)
                    )
                );
            case "view":
            case "debug":
                templateId = Long.valueOf(resourceId);
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        templateAuthService.authViewJobTemplate(username, appResourceScope, templateId)
                    )
                );
            case "edit":
                templateId = Long.valueOf(resourceId);
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        templateAuthService.authEditJobTemplate(username, appResourceScope, templateId)
                    )
                );
            case "delete":
                templateId = Long.valueOf(resourceId);
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        templateAuthService.authDeleteJobTemplate(username, appResourceScope, templateId)
                    )
                );
            case "clone":
                List<PermissionActionResource> actionResources = new ArrayList<>(2);
                PermissionActionResource viewTemplateActionResource = new PermissionActionResource();
                viewTemplateActionResource.setActionId(ActionId.VIEW_JOB_TEMPLATE);
                viewTemplateActionResource.addResource(ResourceTypeEnum.TEMPLATE, resourceId,
                    IamUtil.buildScopePathInfo(appResourceScope));
                PermissionActionResource createTemplateActionResource = new PermissionActionResource();
                createTemplateActionResource.setActionId(ActionId.CREATE_JOB_TEMPLATE);
                createTemplateActionResource.addResource(
                    IamUtil.getIamResourceTypeForResourceScope(appResourceScope),
                    appResourceScope.getId(),
                    IamUtil.buildScopePathInfo(appResourceScope));
                actionResources.add(viewTemplateActionResource);
                actionResources.add(createTemplateActionResource);
                return Response.buildSuccessResp(webAuthService.auth(isReturnApplyUrl, username, actionResources));
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkJobPlanOperationPermission(
        String username,
        AppResourceScope appResourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (appResourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        if (StringUtils.isEmpty(resourceId) || !validateNum(resourceId)) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        TaskTemplateInfoDTO jobTemplate;
        TaskPlanInfoDTO plan;
        Long appId = appResourceScope.getAppId();
        if (appId == null) {
            appId = applicationService.getAppIdByScope(appResourceScope);
        }
        switch (action) {
            case "create":
                Long templateId = Long.valueOf(resourceId);
                jobTemplate = taskTemplateService.getTaskTemplateBasicInfoById(appId, templateId);
                if (jobTemplate == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        planAuthService.authCreateJobPlan(username, appResourceScope, templateId, null)
                    )
                );
            case "view":
            case "execute":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        planAuthService.authViewJobPlan(
                            username, appResourceScope,
                            plan.getTemplateId(), plan.getId(), null
                        )
                    )
                );
            case "edit":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        planAuthService.authEditJobPlan(
                            username, appResourceScope,
                            plan.getTemplateId(), plan.getId(), null
                        )
                    )
                );
            case "delete":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        planAuthService.authDeleteJobPlan(
                            username, appResourceScope,
                            plan.getTemplateId(), plan.getId(), null
                        )
                    )
                );
            case "sync":
                plan = taskPlanService.getTaskPlanById(appId, Long.valueOf(resourceId));
                if (plan == null) {
                    return Response.buildSuccessResp(AuthResultVO.fail());
                }
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        planAuthService.authSyncJobPlan(
                            username, appResourceScope,
                            plan.getTemplateId(), plan.getId(), null
                        )
                    )
                );
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkAccountOperationPermission(
        String username,
        AppResourceScope appResourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (appResourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        Long accountId;
        switch (action) {
            case "create":
                log.info("before transfer, scope={}", appResourceScope);
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        accountAuthService.authCreateAccount(username, appResourceScope)
                    )
                );
            case "view":
            case "edit":
            case "delete":
                accountId = Long.valueOf(resourceId);
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        accountAuthService.authManageAccount(username, appResourceScope, accountId, null)
                    )
                );
            case "use":
                accountId = Long.valueOf(resourceId);
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        accountAuthService.authUseAccount(username, appResourceScope, accountId, null)
                    )
                );
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkTagOperationPermission(
        String username,
        AppResourceScope appResourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (appResourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        tagAuthService.authCreateTag(username, appResourceScope)
                    )
                );
            case "edit":
            case "delete":
                long tagId = Long.parseLong(resourceId);
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        tagAuthService.authManageTag(username, appResourceScope, tagId, null)
                    )
                );
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private Response<AuthResultVO> checkTicketOperationPermission(
        String username,
        AppResourceScope appResourceScope,
        String action,
        String resourceId,
        boolean isReturnApplyUrl
    ) {
        if (appResourceScope == null) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"});
        }
        switch (action) {
            case "create":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        ticketAuthService.authCreateTicket(username, appResourceScope)
                    )
                );
            case "edit":
            case "delete":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        ticketAuthService.authManageTicket(username, appResourceScope, resourceId, null)
                    )
                );
            case "use":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        ticketAuthService.authUseTicket(username, appResourceScope, resourceId, null)
                    )
                );
            default:
                log.error("Unknown operator|{}|{}|{}|{}", username, appResourceScope, action, resourceId);
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
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        noResourceScopeAuthService.authCreateWhiteList(username)
                    )
                );
            case "view":
            case "edit":
            case "delete":
                return Response.buildSuccessResp(
                    webAuthService.toAuthResultVO(
                        isReturnApplyUrl,
                        noResourceScopeAuthService.authManageWhiteList(username)
                    )
                );
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(
        String username,
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
        AppResourceScope appResourceScope = new AppResourceScope(scopeType, scopeId, null);
        boolean isReturnApplyUrl = returnPermissionDetail != null && returnPermissionDetail;

        switch (resourceType) {
            case "biz":
                if ("access_business".equals(action)) {
                    return Response.buildSuccessResp(
                        webAuthService.toAuthResultVO(
                            isReturnApplyUrl,
                            businessAuthService.authAccessBusiness(username, appResourceScope)
                        )
                    );
                }
                break;
            case "script":
                return checkScriptOperationPermission(username, appResourceScope, action, resourceId, isReturnApplyUrl);
            case "public_script":
                return checkPublicScriptOperationPermission(username, action, resourceId, isReturnApplyUrl);
            case "job_template":
                return checkJobTemplateOperationPermission(username, appResourceScope, action, resourceId,
                    isReturnApplyUrl);
            case "job_plan":
                return checkJobPlanOperationPermission(username, appResourceScope, action, resourceId,
                    isReturnApplyUrl);
            case "account":
                return checkAccountOperationPermission(username, appResourceScope, action, resourceId,
                    isReturnApplyUrl);
            case "whitelist":
                return checkWhiteIPOperationPermission(username, action, isReturnApplyUrl);
            case "tag":
                return checkTagOperationPermission(username, appResourceScope, action, resourceId, isReturnApplyUrl);
            case "ticket":
                return checkTicketOperationPermission(username, appResourceScope, action, resourceId, isReturnApplyUrl);
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }
}
