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

package com.tencent.bk.job.file_gateway.api.web;

import com.tencent.bk.job.common.app.Scope;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.file_gateway.model.req.web.OperationPermissionReq;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class WebPermissionResourceImpl implements WebPermissionResource {

    private final WebAuthService authService;

    public WebPermissionResourceImpl(WebAuthService authService) {
        this.authService = authService;
    }

    @Override
    public Response<String> getApplyUrl(String username, OperationPermissionReq req) {
        // authService.
        return null;
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
        String username, OperationPermissionReq req) {
        return checkOperationPermission(
            username, req.getAppId(), req.getScopeType(), req.getScopeId(),
            req.getOperation(), req.getResourceId(), req.isReturnPermissionDetail());
    }

    private PathInfoDTO buildScopePathInfo(Scope scope) {
        return PathBuilder.newBuilder(scope.getType(), scope.getId()).build();
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(String username,
                                                           Long bizId,
                                                           String scopeType,
                                                           String scopeId,
                                                           String operation,
                                                           String resourceId,
                                                           Boolean returnPermissionDetail) {
        Scope scope = getScope(bizId, scopeType, scopeId);
        if (scope == null) {
            return Response.buildCommonFailResp(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"appId/scopeType,scopeId", "appId/scopeType,scopeId cannot be null or empty"}
            );
        }
        if (StringUtils.isEmpty(operation)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        String[] resourceAndAction = operation.split("/");
        if (resourceAndAction.length != 2) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        String resourceType = resourceAndAction[0];
        String action = resourceAndAction[1];
        boolean isReturnApplyUrl = returnPermissionDetail == null ? false : returnPermissionDetail;

        switch (resourceType) {
            case "file_source":
                switch (action) {
                    case "view":
                        return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                            ActionId.VIEW_FILE_SOURCE, ResourceTypeEnum.FILE_SOURCE, resourceId,
                            buildScopePathInfo(scope)));
                    case "create":
                        return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                            ActionId.CREATE_FILE_SOURCE, ResourceTypeEnum.BUSINESS, scopeId,
                            buildScopePathInfo(scope)));
                    case "edit":
                    case "delete":
                        return Response.buildSuccessResp(authService.auth(isReturnApplyUrl, username,
                            ActionId.MANAGE_FILE_SOURCE, ResourceTypeEnum.FILE_SOURCE, resourceId,
                            buildScopePathInfo(scope)));
                    default:
                        log.error("Unknown operator|{}|{}|{}|{}|{}", username, scope, operation, resourceId,
                            returnPermissionDetail);
                }
                break;
            default:
                log.error("Unknown resource type!|{}|{}|{}|{}|{}", username, scope, operation, resourceId,
                    returnPermissionDetail);
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }
}
