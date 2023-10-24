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

package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.execute.api.web.WebPermissionResource;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.web.request.OperationPermissionReq;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@RestController("jobExecuteWebPermissionResourceImpl")
@Slf4j
public class WebPermissionResourceImpl implements WebPermissionResource {
    private final WebAuthService webAuthService;
    private final ExecuteAuthService executeAuthService;
    private final TaskInstanceService taskInstanceService;

    public WebPermissionResourceImpl(WebAuthService webAuthService,
                                     ExecuteAuthService executeAuthService,
                                     TaskInstanceService taskInstanceService) {
        this.webAuthService = webAuthService;
        this.executeAuthService = executeAuthService;
        this.taskInstanceService = taskInstanceService;
    }

    @Override
    public Response<AuthResultVO> checkOperationPermission(String username, OperationPermissionReq req) {
        return checkOperationPermission(
            username, req.getScopeType(), req.getScopeId(),
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
        boolean isReturnApplyUrl = returnPermissionDetail != null && returnPermissionDetail;

        switch (resourceType) {
            case "task_instance":
                long taskInstanceId = Long.parseLong(resourceId);
                TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
                switch (action) {
                    case "view":
                    case "redo":
                        AuthResult authResult = executeAuthService.checkViewTaskInstancePermission(username,
                            appResourceScope, taskInstance);
                        return Response.buildSuccessResp(webAuthService.toAuthResultVO(isReturnApplyUrl, authResult));
                }
                break;
        }
        return Response.buildSuccessResp(AuthResultVO.fail());
    }
}
