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

import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.PageDataWithManagePermission;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.manage.api.web.WebWhiteIPResource;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import com.tencent.bk.job.manage.service.WhiteIPService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class WebWhiteIPResourceImpl implements WebWhiteIPResource {

    private final WhiteIPService whiteIPService;
    private final NoResourceScopeAuthService noResourceScopeAuthService;

    @Autowired
    public WebWhiteIPResourceImpl(WhiteIPService whiteIPService,
                                  NoResourceScopeAuthService noResourceScopeAuthService) {
        this.whiteIPService = whiteIPService;
        this.noResourceScopeAuthService = noResourceScopeAuthService;
    }

    @Override
    public Response<PageDataWithManagePermission<WhiteIPRecordVO>> listWhiteIP(
        String username,
        String ipStr,
        String appIdStr,
        String appNameStr,
        String actionScopeStr,
        String creator,
        String lastModifier,
        Integer start,
        Integer pageSize,
        String orderField,
        Integer order
    ) {
        AuthResult authResult = noResourceScopeAuthService.authManageWhiteList(username);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        PageData<WhiteIPRecordVO> pageData = whiteIPService.listWhiteIPRecord(username, ipStr, appIdStr, appNameStr,
            actionScopeStr, creator, lastModifier, start, pageSize, orderField, order);
        PageDataWithManagePermission<WhiteIPRecordVO> pageDataWithManagePermission =
            new PageDataWithManagePermission<>(pageData);
        pageDataWithManagePermission.setCanCreate(
            noResourceScopeAuthService.authCreateWhiteList(username).isPass());
        pageDataWithManagePermission.setCanManage(
            noResourceScopeAuthService.authManageWhiteList(username).isPass());
        return Response.buildSuccessResp(pageDataWithManagePermission);
    }

    @Override
    public Response<Long> saveWhiteIP(String username, WhiteIPRecordCreateUpdateReq createUpdateReq) {
        Long id = createUpdateReq.getId();
        if (id != null && id > 0) {
            AuthResult authResult = noResourceScopeAuthService.authManageWhiteList(username);
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
        } else {
            AuthResult authResult = noResourceScopeAuthService.authCreateWhiteList(username);
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
        }
        return Response.buildSuccessResp(whiteIPService.saveWhiteIP(username, createUpdateReq));
    }

    @Override
    public Response<WhiteIPRecordVO> getWhiteIPDetailById(String username, Long id) {
        AuthResult authResult = noResourceScopeAuthService.authManageWhiteList(username);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(whiteIPService.getWhiteIPDetailById(username, id));
    }

    @Override
    public Response<List<CloudAreaInfoVO>> listCloudAreas(String username) {
        return Response.buildSuccessResp(whiteIPService.listCloudAreas(username));
    }

    @Override
    public Response<List<ActionScopeVO>> listActionScope(String username) {
        return Response.buildSuccessResp(whiteIPService.listActionScope(username));
    }

    @Override
    public Response<Long> deleteWhiteIPById(String username, Long id) {
        AuthResult authResult = noResourceScopeAuthService.authManageWhiteList(username);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(whiteIPService.deleteWhiteIPById(username, id));
    }
}
