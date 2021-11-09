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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.esb.v3.EsbCredentialV3Resource;
import com.tencent.bk.job.manage.api.inner.ServiceCredentialResource;
import com.tencent.bk.job.manage.common.consts.CredentialTypeEnum;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateOrUpdateCredentialV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbCredentialSimpleInfoV3DTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceBasicCredentialDTO;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
public class EsbCredentialResourceV3Impl implements EsbCredentialV3Resource {
    private final ServiceCredentialResource credentialService;
    private final AuthService authService;
    private final MessageI18nService i18nService;

    @Autowired
    public EsbCredentialResourceV3Impl(ServiceCredentialResource credentialService, AuthService authService,
                                       MessageI18nService i18nService) {
        this.credentialService = credentialService;
        this.authService = authService;
        this.i18nService = i18nService;
    }

    @Override
    public EsbResp<EsbCredentialSimpleInfoV3DTO> createCredential(EsbCreateOrUpdateCredentialV3Req req) {
        checkCreateParam(req);
        return saveCredential(req);
    }

    @Override
    public EsbResp<EsbCredentialSimpleInfoV3DTO> updateCredential(EsbCreateOrUpdateCredentialV3Req req) {
        checkUpdateParam(req);
        return saveCredential(req);
    }

    private void checkAppId(Long appId) {
        if (appId == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"bk_biz_id", "bk_biz_id cannot be null"});
        }
    }

    private void checkCreateParam(EsbCreateOrUpdateCredentialV3Req req) {
        checkAppId(req.getAppId());
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

    private void checkUpdateParam(EsbCreateOrUpdateCredentialV3Req req) {
        checkAppId(req.getAppId());
        if (StringUtils.isBlank(req.getId())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"id", "id cannot be null or blank"});
        }
    }

    private EsbResp<EsbCredentialSimpleInfoV3DTO> saveCredential(EsbCreateOrUpdateCredentialV3Req req) {
        CredentialCreateUpdateReq createUpdateReq = convertToCreateUpdateReq(req);
        InternalResponse<ServiceBasicCredentialDTO> resp;
        if (req.getId() == null) {
            resp = credentialService.createCredential(
                req.getUserName(),
                req.getAppId(),
                createUpdateReq
            );
        } else {
            resp = credentialService.updateCredential(
                req.getUserName(),
                req.getAppId(),
                createUpdateReq
            );
        }
        if (resp.getAuthResult() != null) {
            return authService.buildEsbAuthFailResp(
                AuthResult.fromAuthResultDTO(resp.getAuthResult()).getRequiredActionResources()
            );
        } else if (!resp.isSuccess()) {
            throw new InternalException(resp.getCode());
        }
        ServiceBasicCredentialDTO data = resp.getData();
        return EsbResp.buildSuccessResp(new EsbCredentialSimpleInfoV3DTO(data.getId()));
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
