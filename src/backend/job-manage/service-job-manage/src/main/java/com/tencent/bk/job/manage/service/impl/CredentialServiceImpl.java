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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.manage.auth.TicketAuthService;
import com.tencent.bk.job.manage.dao.CredentialDAO;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceCredentialDisplayDTO;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import com.tencent.bk.job.manage.service.CredentialService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class CredentialServiceImpl implements CredentialService {

    private final CredentialDAO credentialDAO;
    private final TicketAuthService ticketAuthService;
    private final ServiceFileSourceResource fileSourceService;

    @Autowired
    public CredentialServiceImpl(CredentialDAO credentialDAO,
                                 TicketAuthService ticketAuthService,
                                 ServiceFileSourceResource fileSourceService) {
        this.credentialDAO = credentialDAO;
        this.ticketAuthService = ticketAuthService;
        this.fileSourceService = fileSourceService;
    }

    @Override
    public PageData<CredentialDTO> listCredentials(
        CredentialDTO credentialQuery,
        BaseSearchCondition baseSearchCondition
    ) {
        return credentialDAO.listCredentials(credentialQuery, baseSearchCondition);
    }

    @Override
    public PageData<CredentialDTO> listCredentialBasicInfo(Long appId, BaseSearchCondition baseSearchCondition) {
        return credentialDAO.listCredentialBasicInfo(appId, baseSearchCondition);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.CREATE_TICKET,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.TICKET,
            instanceIds = "#$?.id",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.CREATE_TICKET
    )
    @JobTransactional(
        transactionManager = "jobManageTransactionManager",
        rollbackFor = {Exception.class, Error.class}
    )
    public CredentialDTO createCredential(User user,
                                          Long appId,
                                          CredentialCreateUpdateReq createUpdateReq) {
        authCreateTicket(user, appId);

        CredentialDTO credentialDTO = buildCredentialDTO(user.getUsername(), appId, createUpdateReq);
        credentialDTO.setCreator(user.getUsername());
        credentialDTO.setCreateTime(credentialDTO.getLastModifyTime());
        String id = credentialDAO.insertCredential(credentialDTO);
        Boolean registerResult = ticketAuthService.registerTicket(user, id, createUpdateReq.getName());
        if (!registerResult) {
            log.warn("Fail to register ticket to iam:({},{})", id, createUpdateReq.getName());
        }
        return getCredentialById(id);
    }

    public void authCreateTicket(User user, long appId) {
        ticketAuthService.authCreateTicket(user, new AppResourceScope(appId)).denyIfNoPermission();
    }

    private void authManageTicket(User user, long appId, String credentialId) {
        ticketAuthService.authManageTicket(user, new AppResourceScope(appId), credentialId, null)
            .denyIfNoPermission();
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_TICKET,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.TICKET,
            instanceIds = "#createUpdateReq?.id",
            instanceNames = "#createUpdateReq?.name"
        ),
        content = EventContentConstants.EDIT_TICKET
    )
    public CredentialDTO updateCredential(User user,
                                          Long appId,
                                          CredentialCreateUpdateReq createUpdateReq) {
        String id = createUpdateReq.getId();
        authManageTicket(user, appId, id);

        CredentialDTO credentialDTO = buildCredentialDTO(user.getUsername(), appId, createUpdateReq);
        CredentialDTO originCredentialDTO = credentialDAO.getCredentialById(id);
        if (originCredentialDTO == null) {
            throw new NotFoundException(ErrorCode.CREDENTIAL_NOT_EXIST);
        }

        String value1 = createUpdateReq.getValue1();
        if ("******".equals(value1)) {
            credentialDTO.setFirstValue(originCredentialDTO.getFirstValue());
        } else {
            credentialDTO.setFirstValue(value1);
        }
        String value2 = createUpdateReq.getValue2();
        if ("******".equals(value2)) {
            credentialDTO.setSecondValue(originCredentialDTO.getSecondValue());
        } else {
            credentialDTO.setSecondValue(value2);
        }
        credentialDAO.updateCredentialById(credentialDTO);

        CredentialDTO updateCredential = getCredentialById(id);

        // 审计
        ActionAuditContext.current()
            .setOriginInstance(originCredentialDTO.toEsbCredentialSimpleInfoV3DTO())
            .setInstance(updateCredential.toEsbCredentialSimpleInfoV3DTO());

        return updateCredential;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_TICKET,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.TICKET,
            instanceIds = "#id"
        ),
        content = EventContentConstants.DELETE_TICKET
    )
    public Integer deleteCredentialById(User user, Long appId, String id) {
        authManageTicket(user, appId, id);

        CredentialDTO credential = getCredentialById(id);
        if (credential == null) {
            throw new NotFoundException(ErrorCode.CREDENTIAL_NOT_EXIST);
        }

        // 审计
        ActionAuditContext.current().setInstanceName(credential.getName());

        // 检查是否被引用
        Boolean isCredentialReferenced = fileSourceService.existsFileSourceUsingCredential(appId, id).getData();
        if (isCredentialReferenced) {
            String msg = MessageFormatter.format(
                "Credential ({},{}) is referenced, cannot delete",
                appId,
                id
            ).getMessage();
            throw new FailedPreconditionException(msg, ErrorCode.DELETE_REF_CREDENTIAL_FAIL);
        }

        return credentialDAO.deleteCredentialById(id);
    }

    @Override
    public CredentialDTO getCredentialById(Long appId, String id) {
        CredentialDTO credentialDTO = getCredentialById(id);
        if (credentialDTO == null || !credentialDTO.getAppId().equals(appId)) {
            return null;
        } else {
            return credentialDTO;
        }
    }

    @Override
    public CredentialDTO getCredentialById(String id) {
        return credentialDAO.getCredentialById(id);
    }

    @Override
    public List<ServiceCredentialDisplayDTO> listCredentialDisplayInfoByIds(Collection<String> ids) {
        return credentialDAO.listCredentialDisplayInfoByIds(ids);
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
