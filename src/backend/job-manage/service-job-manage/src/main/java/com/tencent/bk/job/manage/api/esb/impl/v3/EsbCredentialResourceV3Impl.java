/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.common.constants.CredentialTypeEnum;
import com.tencent.bk.job.manage.api.esb.v3.EsbCredentialV3Resource;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateOrUpdateCredentialV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetCredentialDetailV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbCredentialSimpleInfoV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbCredentialV3DTO;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import com.tencent.bk.job.manage.service.CredentialService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
public class EsbCredentialResourceV3Impl implements EsbCredentialV3Resource {
    private final CredentialService credentialService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public EsbCredentialResourceV3Impl(CredentialService credentialService,
                                       AppScopeMappingService appScopeMappingService) {
        this.credentialService = credentialService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @AuditEntry(
        actionId = ActionId.CREATE_TICKET
    )
    public EsbResp<EsbCredentialSimpleInfoV3DTO> createCredential(
        String username,
        String appCode,
        @AuditRequestBody EsbCreateOrUpdateCredentialV3Req req) {
        checkCreateParam(req);

        CredentialCreateUpdateReq createUpdateReq = convertToCreateUpdateReq(req);
        CredentialDTO createCredential = credentialService.createCredential(username, req.getAppId(),
            createUpdateReq);

        return EsbResp.buildSuccessResp(createCredential.toEsbCredentialSimpleInfoV3DTO());
    }

    private void checkCreateParam(EsbCreateOrUpdateCredentialV3Req req) {
        String name = req.getName();
        String type = req.getType();
        if (StringUtils.isBlank(name)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"name", "name cannot be null or blank"});
        }
        if (StringUtils.isBlank(type)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"type", "type cannot be null or blank"});
        }
    }

    @Override
    @AuditEntry(
        actionId = ActionId.MANAGE_TICKET
    )
    public EsbResp<EsbCredentialSimpleInfoV3DTO> updateCredential(
        String username,
        String appCode,
        @AuditRequestBody EsbCreateOrUpdateCredentialV3Req req) {
        checkUpdateParam(req);

        CredentialCreateUpdateReq createUpdateReq = convertToCreateUpdateReq(req);
        CredentialDTO updateCredential = credentialService.updateCredential(username, req.getAppId(),
            createUpdateReq);

        return EsbResp.buildSuccessResp(updateCredential.toEsbCredentialSimpleInfoV3DTO());
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "get_credential_detail"})
    public EsbResp<EsbCredentialV3DTO> getCredentialDetail(
        String username,
        String appCode,
        Long bizId,
        String scopeType,
        String scopeId,
        String id) {
        EsbGetCredentialDetailV3Req req = new EsbGetCredentialDetailV3Req();
        req.setBizId(bizId);
        req.setScopeType(scopeType);
        req.setScopeId(scopeId);
        req.setId(id);
        req.fillAppResourceScope(appScopeMappingService);
        return getCredentialDetailUsingPost(username, appCode, req);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "get_credential_detail"})
    public EsbResp<EsbCredentialV3DTO> getCredentialDetailUsingPost(
        String username,
        String appCode,
        @AuditRequestBody EsbGetCredentialDetailV3Req req) {
        CredentialDTO credentialDTO = credentialService.getCredentialById(req.getId());
        return EsbResp.buildSuccessResp(credentialDTO.toEsbCredentialV3DTO());
    }

    private void checkUpdateParam(EsbCreateOrUpdateCredentialV3Req req) {
        if (StringUtils.isBlank(req.getId())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"id", "id cannot be null or blank"});
        }
    }

    private CredentialCreateUpdateReq convertToCreateUpdateReq(
        EsbCreateOrUpdateCredentialV3Req req
    ) {
        String type = req.getType();
        CredentialCreateUpdateReq createUpdateReq = new CredentialCreateUpdateReq();
        createUpdateReq.setId(req.getId());
        createUpdateReq.setName(req.getName());
        createUpdateReq.setType(CredentialTypeEnum.valueOf(req.getType()));
        createUpdateReq.setDescription(req.getDescription());
        if (CredentialTypeEnum.SECRET_KEY.name().equals(type)) {
            createUpdateReq.setValue1(req.getCredentialSecretKey());
        } else if (CredentialTypeEnum.PASSWORD.name().equals(type)) {
            createUpdateReq.setValue1(req.getCredentialPassword());
        } else if (CredentialTypeEnum.APP_ID_SECRET_KEY.name().equals(type)) {
            createUpdateReq.setValue1(req.getCredentialAccessKey());
            createUpdateReq.setValue2(req.getCredentialSecretKey());
        } else if (CredentialTypeEnum.USERNAME_PASSWORD.name().equals(type)) {
            createUpdateReq.setValue1(req.getCredentialUsername());
            createUpdateReq.setValue2(req.getCredentialPassword());
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{
                    "type",
                    String.format(
                        "Unsupported type:%s, supported types:%s",
                        type,
                        CredentialTypeEnum.getAllNameStr()
                    )}

            );
        }
        return createUpdateReq;
    }
}
