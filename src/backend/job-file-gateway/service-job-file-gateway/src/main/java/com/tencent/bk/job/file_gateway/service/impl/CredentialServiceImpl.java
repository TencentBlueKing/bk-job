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

package com.tencent.bk.job.file_gateway.service.impl;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.file_gateway.client.ServiceCredentialResourceClient;
import com.tencent.bk.job.file_gateway.service.CredentialService;
import com.tencent.bk.job.manage.model.credential.CommonCredential;
import com.tencent.bk.job.manage.model.inner.resp.ServiceCredentialDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CredentialServiceImpl implements CredentialService {

    ServiceCredentialResourceClient credentialService;

    @Autowired
    public CredentialServiceImpl(ServiceCredentialResourceClient credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    public CommonCredential getCredentialById(Long appId, String id) {
        InternalResponse<ServiceCredentialDTO> credentialWebResponse = credentialService.getCredentialById(appId,
            id);
        ServiceCredentialDTO credentialDTO = credentialWebResponse.getData();
        if (credentialDTO == null) {
            return null;
        }
        try {
            CommonCredential commonCredential = credentialDTO.getCredential();
            // Type补全
            commonCredential.setType(credentialDTO.getType());
            return commonCredential;
        } catch (Exception e) {
            log.error("credential not valid:{}", credentialDTO, e);
            return null;
        }
    }
}
