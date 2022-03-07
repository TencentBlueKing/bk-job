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

package com.tencent.bk.job.crontab.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.crontab.api.web.WebPermissionResource;
import com.tencent.bk.job.crontab.client.ServiceApplicationResourceClient;
import com.tencent.bk.job.crontab.model.OperationPermissionReq;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class WebPermissionResourceImpl implements WebPermissionResource {
    private final WebAuthService authService;
    private final ServiceApplicationResourceClient applicationResourceClient;

    public WebPermissionResourceImpl(WebAuthService authService,
                                     ServiceApplicationResourceClient applicationResourceClient) {
        this.authService = authService;
        this.applicationResourceClient = applicationResourceClient;
    }

    @Override
    public Response<String> getApplyUrl(String username, OperationPermissionReq req) {
        // authService.
        return null;
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(String username, OperationPermissionReq req) {
        return checkOperationPermission(
            username, req.getAppId(), req.getScopeType(), req.getScopeId(),
            req.getOperation(), req.getResourceId(), req.isReturnPermissionDetail());
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(String username,
                                                           Long appId,
                                                           String scopeType,
                                                           String scopeId,
                                                           String operation,
                                                           String resourceId,
                                                           Boolean returnPermissionDetail) {
        ResourceScope resourceScope = toResourceScope(appId, scopeType, scopeId);
        if (StringUtils.isEmpty(operation)) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        String[] resourceAndAction = operation.split("/");
        if (resourceAndAction.length != 2) {
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        String resourceType = resourceAndAction[0];
        String action = resourceAndAction[1];
        boolean isReturnApplyUrl = returnPermissionDetail != null && returnPermissionDetail;

        switch (resourceType) {
            case "cron":
                switch (action) {
                    case "create":
                        return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                            ActionId.CREATE_CRON, ResourceTypeEnum.BUSINESS, scopeId,
                            IamUtil.buildScopePathInfo(resourceScope)));
                    case "view":
                    case "edit":
                    case "delete":
                    case "manage":
                        return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                            ActionId.MANAGE_CRON, ResourceTypeEnum.CRON, resourceId,
                            IamUtil.buildScopePathInfo(resourceScope)));
                    default:
                        log.error("Unknown operator|{}|{}|{}|{}|{}", username, resourceScope, operation, resourceId,
                            returnPermissionDetail);
                }
                break;
            default:
                log.error("Unknown resource type!|{}|{}|{}|{}|{}", username, resourceScope, operation, resourceId,
                    returnPermissionDetail);
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }

    private ResourceScope toResourceScope(Long appId, String scopeType, String scopeId) {
        if (scopeType != null) {
            return new ResourceScope(scopeType, scopeId);
        } else if (appId != null) {
            ServiceApplicationDTO application = applicationResourceClient.queryAppById(appId);
            return new ResourceScope(application.getScopeType(), application.getScopeId());
        }
        return null;
    }
}
