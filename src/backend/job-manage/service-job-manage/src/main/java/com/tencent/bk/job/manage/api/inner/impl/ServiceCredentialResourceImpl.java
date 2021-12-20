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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.inner.ServiceCredentialResource;
import com.tencent.bk.job.manage.model.inner.resp.ServiceBasicCredentialDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceCredentialDTO;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import com.tencent.bk.job.manage.service.CredentialService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
public class ServiceCredentialResourceImpl implements ServiceCredentialResource {

    private final AuthService authService;
    private final CredentialService credentialService;

    @Autowired
    public ServiceCredentialResourceImpl(AuthService authService, CredentialService credentialService) {
        this.authService = authService;
        this.credentialService = credentialService;
    }

    @Override
    public InternalResponse<ServiceCredentialDTO> getCredentialById(Long appId, String id) {
        ServiceCredentialDTO serviceCredentialDTO = credentialService.getServiceCredentialById(appId, id);
        return InternalResponse.buildSuccessResp(serviceCredentialDTO);
    }

    @Override
    public InternalResponse<ServiceBasicCredentialDTO> createCredential(
        String username,
        Long appId,
        CredentialCreateUpdateReq createUpdateReq
    ) {
        return saveCredential(username, appId, createUpdateReq);
    }

    @Override
    public InternalResponse<ServiceBasicCredentialDTO> updateCredential(
        String username,
        Long appId,
        CredentialCreateUpdateReq createUpdateReq
    ) {
        return saveCredential(username, appId, createUpdateReq);
    }

    private InternalResponse<ServiceBasicCredentialDTO> saveCredential(
        String username,
        Long appId,
        CredentialCreateUpdateReq createUpdateReq
    ) {
        AuthResult authResult = null;
        if (StringUtils.isBlank(createUpdateReq.getId())) {
            authResult = checkCreateTicketPermission(username, appId);
        } else {
            authResult = checkManageTicketPermission(username, appId, createUpdateReq.getId());
        }
        if (!authResult.isPass()) {
            return InternalResponse.buildAuthFailResp(AuthResult.toAuthResultDTO(authResult));
        }
        String credentialId = credentialService.saveCredential(username, appId, createUpdateReq);
        return InternalResponse.buildSuccessResp(new ServiceBasicCredentialDTO(credentialId));
    }

    public AuthResult checkCreateTicketPermission(String username, Long appId) {
        // 需要拥有在业务下创建凭证的权限
        return authService.auth(true, username, ActionId.CREATE_TICKET, ResourceTypeEnum.BUSINESS,
            appId.toString(), null);
    }

    public AuthResult checkManageTicketPermission(String username, Long appId, String credentialId) {
        // 需要拥有在业务下管理某个具体凭证的权限
        return authService.auth(true, username, ActionId.MANAGE_TICKET, ResourceTypeEnum.TICKET,
            credentialId, PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build());
    }
}
