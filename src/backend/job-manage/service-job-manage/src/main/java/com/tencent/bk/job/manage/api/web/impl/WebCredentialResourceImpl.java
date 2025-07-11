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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.api.web.WebCredentialResource;
import com.tencent.bk.job.manage.auth.TicketAuthService;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.CredentialBasicVO;
import com.tencent.bk.job.manage.model.web.vo.CredentialVO;
import com.tencent.bk.job.manage.service.CredentialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@Slf4j
public class WebCredentialResourceImpl implements WebCredentialResource {

    private final CredentialService credentialService;
    private final TicketAuthService ticketAuthService;

    @Autowired
    public WebCredentialResourceImpl(CredentialService credentialService,
                                     TicketAuthService ticketAuthService) {
        this.credentialService = credentialService;
        this.ticketAuthService = ticketAuthService;
    }

    @Override
    public Response<PageData<CredentialVO>> listCredentials(String username,
                                                            AppResourceScope appResourceScope,
                                                            String scopeType,
                                                            String scopeId,
                                                            String id,
                                                            String name,
                                                            String description,
                                                            String creator,
                                                            String lastModifyUser,
                                                            Integer start,
                                                            Integer pageSize) {
        CredentialDTO credentialQuery = new CredentialDTO();
        credentialQuery.setId(id);
        credentialQuery.setAppId(appResourceScope.getAppId());
        credentialQuery.setName(name);
        credentialQuery.setDescription(description);
        credentialQuery.setCreator(creator);
        credentialQuery.setLastModifyUser(lastModifyUser);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);
        PageData<CredentialDTO> pageData = credentialService.listCredentials(credentialQuery, baseSearchCondition);
        List<CredentialVO> credentialVOList =
            pageData.getData().stream().map(CredentialDTO::toVO).collect(Collectors.toList());
        PageData<CredentialVO> finalPageData = new PageData<>();
        finalPageData.setStart(pageData.getStart());
        finalPageData.setPageSize(pageData.getPageSize());
        finalPageData.setTotal(pageData.getTotal());
        finalPageData.setData(credentialVOList);
        addPermissionData(username, appResourceScope, finalPageData);
        return Response.buildSuccessResp(finalPageData);
    }

    @Override
    public Response<PageData<CredentialBasicVO>> listCredentialBasicInfo(String username,
                                                                         AppResourceScope appResourceScope,
                                                                         String scopeType,
                                                                         String scopeId,
                                                                         Integer start,
                                                                         Integer pageSize) {
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);
        PageData<CredentialDTO> pageData = credentialService.listCredentialBasicInfo(
            appResourceScope.getAppId(),
            baseSearchCondition
        );
        List<CredentialBasicVO> credentialBasicVOList =
            pageData.getData().stream().map(CredentialDTO::toBasicVO).collect(Collectors.toList());
        PageData<CredentialBasicVO> finalPageData = new PageData<>();
        finalPageData.setStart(pageData.getStart());
        finalPageData.setPageSize(pageData.getPageSize());
        finalPageData.setTotal(pageData.getTotal());
        finalPageData.setData(credentialBasicVOList);
        addPermissionDataForBasicVO(username, appResourceScope, finalPageData);
        return Response.buildSuccessResp(finalPageData);
    }

    @Override
    @AuditEntry(actionId = ActionId.CREATE_TICKET)
    public Response<CredentialVO> createCredential(String username,
                                                   AppResourceScope appResourceScope,
                                                   String scopeType,
                                                   String scopeId,
                                                   @AuditRequestBody CredentialCreateUpdateReq createUpdateReq) {
        CredentialDTO credential =
            credentialService.createCredential(username, appResourceScope.getAppId(), createUpdateReq);
        return Response.buildSuccessResp(credential.toVO());
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_TICKET)
    public Response<CredentialVO> updateCredential(String username,
                                                   AppResourceScope appResourceScope,
                                                   String scopeType,
                                                   String scopeId,
                                                   String credentialId,
                                                   @AuditRequestBody CredentialCreateUpdateReq createUpdateReq) {
        createUpdateReq.setId(credentialId);
        CredentialDTO credential = credentialService.updateCredential(username, appResourceScope.getAppId(),
            createUpdateReq);
        return Response.buildSuccessResp(credential.toVO());
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_TICKET)
    public Response<Integer> deleteCredentialById(String username,
                                                  AppResourceScope appResourceScope,
                                                  String scopeType,
                                                  String scopeId,
                                                  String id) {
        return Response.buildSuccessResp(
            credentialService.deleteCredentialById(username, appResourceScope.getAppId(), id));
    }

    private void addPermissionData(String username, AppResourceScope appResourceScope,
                                   PageData<CredentialVO> credentialVOPageData) {
        List<CredentialVO> credentialVOList = credentialVOPageData.getData();
        // 添加权限数据
        List<String> credentialIdList = credentialVOList.stream()
            .map(CredentialVO::getId)
            .collect(Collectors.toList());
        List<String> canManageIdList =
            ticketAuthService.batchAuthManageTicket(username, appResourceScope, credentialIdList);
        List<String> canUseIdList =
            ticketAuthService.batchAuthUseTicket(username, appResourceScope, credentialIdList);
        credentialVOList.forEach(it -> {
            it.setCanManage(canManageIdList.contains(it.getId()));
            it.setCanUse(canUseIdList.contains(it.getId()));
        });
        credentialVOPageData.setCanCreate(checkCreateTicketPermission(username, appResourceScope).isPass());
    }

    private void addPermissionDataForBasicVO(String username, AppResourceScope appResourceScope,
                                             PageData<CredentialBasicVO> credentialBasicVOPageData) {
        List<CredentialBasicVO> credentialBasicVOList = credentialBasicVOPageData.getData();
        // 添加权限数据
        List<String> credentialIdList = credentialBasicVOList.stream()
            .map(CredentialBasicVO::getId)
            .collect(Collectors.toList());
        List<String> canUseIdList =
            ticketAuthService.batchAuthUseTicket(username, appResourceScope, credentialIdList);
        credentialBasicVOList.forEach(it -> {
            it.setCanUse(canUseIdList.contains(it.getId()));
        });
        credentialBasicVOPageData.setCanCreate(checkCreateTicketPermission(username, appResourceScope).isPass());
    }

    public AuthResult checkCreateTicketPermission(String username, AppResourceScope appResourceScope) {
        // 需要拥有在业务下创建凭证的权限
        return ticketAuthService.authCreateTicket(username, appResourceScope);
    }
}
