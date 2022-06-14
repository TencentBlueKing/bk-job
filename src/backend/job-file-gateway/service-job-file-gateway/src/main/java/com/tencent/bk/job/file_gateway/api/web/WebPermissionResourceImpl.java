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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.file_gateway.auth.FileSourceAuthService;
import com.tencent.bk.job.file_gateway.model.req.web.OperationPermissionReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class WebPermissionResourceImpl implements WebPermissionResource {

    private final WebAuthService webAuthService;
    private final FileSourceAuthService fileSourceAuthService;

    public WebPermissionResourceImpl(WebAuthService webAuthService,
                                     FileSourceAuthService fileSourceAuthService) {
        this.webAuthService = webAuthService;
        this.fileSourceAuthService = fileSourceAuthService;
    }

    @Override
    public Response<String> getApplyUrl(String username, OperationPermissionReq req) {
        // authService.
        return null;
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(
        String username, OperationPermissionReq req) {
        return checkOperationPermission(username, req.getScopeType(), req.getScopeId(),
            req.getOperation(), req.getResourceId(), req.isReturnPermissionDetail());
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(String username,
                                                           String scopeType,
                                                           String scopeId,
                                                           String operation,
                                                           String resourceId,
                                                           Boolean returnPermissionDetail) {
        AppResourceScope appResourceScope = new AppResourceScope(scopeType, scopeId, null);
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
                        return Response.buildSuccessResp(
                            webAuthService.toAuthResultVO(
                                isReturnApplyUrl,
                                fileSourceAuthService.authViewFileSource(
                                    username,
                                    appResourceScope,
                                    Integer.valueOf(resourceId),
                                    null
                                )
                            )
                        );
                    case "create":
                        return Response.buildSuccessResp(
                            webAuthService.toAuthResultVO(
                                isReturnApplyUrl,
                                fileSourceAuthService.authCreateFileSource(username, appResourceScope)
                            )
                        );
                    case "edit":
                    case "delete":
                        return Response.buildSuccessResp(
                            webAuthService.toAuthResultVO(
                                isReturnApplyUrl,
                                fileSourceAuthService.authManageFileSource(
                                    username,
                                    appResourceScope,
                                    Integer.valueOf(resourceId),
                                    null
                                )
                            )
                        );
                    default:
                        log.error("Unknown operator|{}|{}|{}|{}|{}", username, appResourceScope, operation, resourceId,
                            returnPermissionDetail);
                }
                break;
            default:
                log.error("Unknown resource type!|{}|{}|{}|{}|{}", username, appResourceScope, operation, resourceId,
                    returnPermissionDetail);
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }
}
