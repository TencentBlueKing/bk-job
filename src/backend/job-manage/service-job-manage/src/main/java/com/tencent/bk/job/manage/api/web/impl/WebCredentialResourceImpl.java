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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.manage.api.web.WebCredentialResource;
import com.tencent.bk.job.manage.common.consts.CredentialTypeEnum;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import com.tencent.bk.job.manage.model.web.request.CredentialCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.CredentialVO;
import com.tencent.bk.job.manage.service.CredentialService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@Slf4j
public class WebCredentialResourceImpl implements WebCredentialResource {

    private final WebAuthService authService;
    private final CredentialService credentialService;

    @Autowired
    public WebCredentialResourceImpl(WebAuthService authService, CredentialService credentialService) {
        this.authService = authService;
        this.credentialService = credentialService;
    }

    private PageData<CredentialVO> mock() {
        CredentialVO credentialVO = new CredentialVO();
        credentialVO.setId("testId");
        credentialVO.setName("凭据1");
        credentialVO.setType(CredentialTypeEnum.APP_ID_SECRET_KEY.name());
        credentialVO.setDescription("mock的凭据");
        credentialVO.setValue1("value1");
        credentialVO.setValue2("value2");
        credentialVO.setValue3("value3");
        credentialVO.setLastModifyUser("admin");
        credentialVO.setLastModifyTime(System.currentTimeMillis());
        PageData<CredentialVO> pageData = new PageData<>();
        pageData.setStart(0);
        pageData.setPageSize(10);
        pageData.setTotal(1L);
        pageData.setData(Collections.singletonList(credentialVO));
        return pageData;
    }

    @Override
    public ServiceResponse<PageData<CredentialVO>> listCredentials(
        String username,
        Long appId,
        String id,
        String name,
        String description,
        String creator,
        String lastModifyUser,
        Integer start,
        Integer pageSize
    ) {
        log.debug("Input=({},{},{},{},{},{},{},{},{})", username, appId, id, name, description, creator,
            lastModifyUser, start, pageSize);
        CredentialDTO credentialQuery = new CredentialDTO();
        credentialQuery.setId(id);
        credentialQuery.setAppId(appId);
        credentialQuery.setName(name);
        credentialQuery.setDescription(description);
        credentialQuery.setCreator(creator);
        credentialQuery.setLastModifyUser(lastModifyUser);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);
        PageData<CredentialDTO> pageData = credentialService.listCredentials(credentialQuery, baseSearchCondition);
        List<CredentialVO> credentialVOList =
            pageData.getData().parallelStream().map(CredentialDTO::toVO).collect(Collectors.toList());
        PageData<CredentialVO> finalPageData = new PageData<>();
        finalPageData.setStart(pageData.getStart());
        finalPageData.setPageSize(pageData.getPageSize());
        finalPageData.setTotal(pageData.getTotal());
        finalPageData.setData(credentialVOList);
        addPermissionData(username, appId, finalPageData);
        return ServiceResponse.buildSuccessResp(finalPageData);
    }

    @Override
    public ServiceResponse<String> saveCredential(String username, Long appId,
                                                  CredentialCreateUpdateReq createUpdateReq) {
        AuthResultVO authResultVO = null;
        if (StringUtils.isBlank(createUpdateReq.getId())) {
            authResultVO = checkCreateTicketPermission(username, appId);
        } else {
            authResultVO = checkManageTicketPermission(username, appId, createUpdateReq.getId());
        }
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(credentialService.saveCredential(username, appId, createUpdateReq));
    }

    @Override
    public ServiceResponse<Integer> deleteCredentialById(String username, Long appId, String id) {
        AuthResultVO authResultVO = checkManageTicketPermission(username, appId, id);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(credentialService.deleteCredentialById(username, appId, id));
    }

    private void addPermissionData(String username, Long appId, PageData<CredentialVO> credentialVOPageData) {
        List<CredentialVO> credentialVOList = credentialVOPageData.getData();
        // 添加权限数据
        List<String> canManageIdList =
            authService.batchAuth(username, ActionId.MANAGE_TICKET, appId, ResourceTypeEnum.TICKET,
                credentialVOList.parallelStream()
                    .map(CredentialVO::getId)
                    .map(Objects::toString)
                    .collect(Collectors.toList()));
        List<String> canUseIdList =
            authService.batchAuth(username, ActionId.USE_TICKET, appId, ResourceTypeEnum.TICKET,
                credentialVOList.parallelStream()
                    .map(CredentialVO::getId)
                    .map(Objects::toString)
                    .collect(Collectors.toList()));
        credentialVOList.forEach(it -> {
            it.setCanManage(canManageIdList.contains(it.getId()));
            it.setCanUse(canUseIdList.contains(it.getId()));
        });
        credentialVOPageData.setCanCreate(checkCreateTicketPermission(username, appId).isPass());
    }

    public AuthResultVO checkUseTicketPermission(String username, Long appId, String credentialId) {
        // 需要拥有在业务下使用某个凭证的权限
        return authService.auth(true, username, ActionId.USE_TICKET, ResourceTypeEnum.TICKET, credentialId,
            PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build());
    }

    public AuthResultVO checkCreateTicketPermission(String username, Long appId) {
        // 需要拥有在业务下创建凭证的权限
        return authService.auth(true, username, ActionId.CREATE_TICKET, ResourceTypeEnum.BUSINESS,
            appId.toString(), null);
    }

    public AuthResultVO checkManageTicketPermission(String username, Long appId, String credentialId) {
        // 需要拥有在业务下管理某个具体凭证的权限
        return authService.auth(true, username, ActionId.MANAGE_TICKET, ResourceTypeEnum.TICKET,
            credentialId, PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build());
    }
}
