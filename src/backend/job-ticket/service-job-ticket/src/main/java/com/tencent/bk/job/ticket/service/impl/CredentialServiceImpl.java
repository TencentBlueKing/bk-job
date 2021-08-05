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

package com.tencent.bk.job.ticket.service.impl;

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.ticket.dao.CredentialDAO;
import com.tencent.bk.job.ticket.model.dto.CredentialDTO;
import com.tencent.bk.job.ticket.model.inner.resp.ServiceCredentialDTO;
import com.tencent.bk.job.ticket.model.web.req.CredentialCreateUpdateReq;
import com.tencent.bk.job.ticket.model.web.resp.CredentialVO;
import com.tencent.bk.job.ticket.service.CredentialService;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    public PageData<CredentialDTO> listCredentials(List<Long> appIdList, List<String> idList, Integer start,
                                                   Integer pageSize) {
        List<CredentialDTO> credentialDTOList = credentialDAO.listCredentials(dslContext, appIdList, idList, start,
            pageSize);
        if (credentialDTOList != null) {
            Integer totalCount = credentialDAO.countCredentials(dslContext, appIdList, idList);
            PageData<CredentialDTO> pageData = new PageData<>();
            pageData.setStart(start);
            pageData.setPageSize(pageSize);
            pageData.setTotal(totalCount.longValue());
            pageData.setData(credentialDTOList);
            return pageData;
        } else {
            throw new RuntimeException("get null result when listCredentials");
        }
    }

    @Override
    public PageData<CredentialVO> listCredentials(String username, Long appId, String id, String name,
                                                  String description, String creator, String lastModifyUser,
                                                  Integer start, Integer pageSize) {
        List<CredentialDTO> credentialDTOList = credentialDAO.listCredentials(dslContext, appId, id, name,
            description, creator, lastModifyUser, start, pageSize);
        if (credentialDTOList != null) {
            Integer totalCount = credentialDAO.countCredentials(dslContext, appId, id, name, description, creator,
                lastModifyUser);
            List<CredentialVO> credentialVOList =
                credentialDTOList.parallelStream().map(CredentialDTO::toVO).collect(Collectors.toList());
            PageData<CredentialVO> pageData = new PageData<>();
            pageData.setStart(start);
            pageData.setPageSize(pageSize);
            pageData.setTotal(totalCount.longValue());
            pageData.setData(credentialVOList);
            return pageData;
        } else {
            throw new RuntimeException("get null result when listCredentials");
        }
    }

    @Override
    public String saveCredential(String username, Long appId, CredentialCreateUpdateReq createUpdateReq) {
        String id = createUpdateReq.getId();
        CredentialDTO credentialDTO = buildCredentialDTO(username, appId, createUpdateReq);
        if (StringUtils.isNotBlank(id)) {
            CredentialDTO oldCredentialDTO = credentialDAO.getCredentialById(dslContext, id);
            if (oldCredentialDTO == null) {
                throw new ServiceException(String.format("cannot find credential by id=%d", id));
            }
            String value1 = createUpdateReq.getValue1();
            if (value1.equals("******")) {
                credentialDTO.setFirstValue(oldCredentialDTO.getFirstValue());
            } else {
                credentialDTO.setFirstValue(value1);
            }
            String value2 = createUpdateReq.getValue2();
            if (value2.equals("******")) {
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
