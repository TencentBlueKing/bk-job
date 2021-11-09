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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.dao.CredentialDAO;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceCredentialDTO;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import com.tencent.bk.job.manage.service.CredentialService;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CredentialServiceImpl implements CredentialService {

    private final DSLContext dslContext;
    private final CredentialDAO credentialDAO;

    @Autowired
    public CredentialServiceImpl(DSLContext dslContext, CredentialDAO credentialDAO) {
        this.dslContext = dslContext;
        this.credentialDAO = credentialDAO;
    }

    @Override
    public PageData<CredentialDTO> listCredentials(
        CredentialDTO credentialQuery,
        BaseSearchCondition baseSearchCondition
    ) {
        return credentialDAO.listCredentials(credentialQuery, baseSearchCondition);
    }

    @Override
    public String saveCredential(String username, Long appId, CredentialCreateUpdateReq createUpdateReq) {
        String id = createUpdateReq.getId();
        CredentialDTO credentialDTO = buildCredentialDTO(username, appId, createUpdateReq);
        if (StringUtils.isNotBlank(id)) {
            CredentialDTO oldCredentialDTO = credentialDAO.getCredentialById(dslContext, id);
            if (oldCredentialDTO == null) {
                throw new NotFoundException(ErrorCode.CREDENTIAL_NOT_EXIST);
            }
            String value1 = createUpdateReq.getValue1();
            if ("******".equals(value1)) {
                credentialDTO.setFirstValue(oldCredentialDTO.getFirstValue());
            } else {
                credentialDTO.setFirstValue(value1);
            }
            String value2 = createUpdateReq.getValue2();
            if ("******".equals(value2)) {
                credentialDTO.setSecondValue(oldCredentialDTO.getSecondValue());
            } else {
                credentialDTO.setSecondValue(value2);
            }
            return credentialDAO.updateCredentialById(dslContext, credentialDTO);
        } else {
            credentialDTO.setCreator(username);
            credentialDTO.setCreateTime(credentialDTO.getLastModifyTime());
            return credentialDAO.insertCredential(dslContext, credentialDTO);
        }
    }

    @Override
    public Integer deleteCredentialById(String username, Long appId, String id) {
        return credentialDAO.deleteCredentialById(dslContext, id);
    }

    @Override
    public ServiceCredentialDTO getServiceCredentialById(Long appId, String id) {
        CredentialDTO credentialDTO = credentialDAO.getCredentialById(dslContext, id);
        if (credentialDTO == null || !credentialDTO.getAppId().equals(appId)) {
            return null;
        } else {
            return credentialDTO.toServiceCredentialDTO();
        }
    }

    @Override
    public ServiceCredentialDTO getServiceCredentialById(String id) {
        CredentialDTO credentialDTO = credentialDAO.getCredentialById(dslContext, id);
        if (credentialDTO == null) {
            return null;
        } else {
            return credentialDTO.toServiceCredentialDTO();
        }
    }

    private CredentialDTO buildCredentialDTO(String username, Long appId, CredentialCreateUpdateReq createUpdateReq) {
        CredentialDTO credentialDTO = new CredentialDTO();
        credentialDTO.setId(createUpdateReq.getId());
        credentialDTO.setAppId(appId);
        credentialDTO.setName(createUpdateReq.getName());
        credentialDTO.setType(createUpdateReq.getType().name());
        credentialDTO.setDescription(createUpdateReq.getDescription());
        credentialDTO.setCredential(createUpdateReq.toCommonCredential());
        credentialDTO.setLastModifyUser(username);
        credentialDTO.setLastModifyTime(System.currentTimeMillis());
        return credentialDTO;
    }
}
