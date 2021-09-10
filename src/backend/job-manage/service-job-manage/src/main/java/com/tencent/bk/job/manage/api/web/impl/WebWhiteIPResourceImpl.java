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
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.PageDataWithManagePermission;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.manage.api.web.WebWhiteIPResource;
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
    private final WebAuthService authService;

    @Autowired
    public WebWhiteIPResourceImpl(WhiteIPService whiteIPService, WebAuthService authService) {
        this.whiteIPService = whiteIPService;
        this.authService = authService;
    }

    @Override
    public ServiceResponse<PageDataWithManagePermission<WhiteIPRecordVO>> listWhiteIP(
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
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.MANAGE_WHITELIST);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        PageData<WhiteIPRecordVO> pageData = whiteIPService.listWhiteIPRecord(username, ipStr, appIdStr, appNameStr,
            actionScopeStr, creator, lastModifier, start, pageSize, orderField, order);
        PageDataWithManagePermission<WhiteIPRecordVO> pageDataWithManagePermission =
            new PageDataWithManagePermission<>(pageData);
        pageDataWithManagePermission.setCanCreate(
            authService.auth(false, username, ActionId.CREATE_WHITELIST).isPass());
        pageDataWithManagePermission.setCanManage(
            authService.auth(false, username, ActionId.MANAGE_WHITELIST).isPass());
        return ServiceResponse.buildSuccessResp(pageDataWithManagePermission);
    }

    @Override
    public ServiceResponse<Long> saveWhiteIP(String username, WhiteIPRecordCreateUpdateReq createUpdateReq) {
        Long id = createUpdateReq.getId();
        if (id != null && id > 0) {
            AuthResultVO authResultVO = authService.auth(true, username, ActionId.MANAGE_WHITELIST);
            if (!authResultVO.isPass()) {
                return ServiceResponse.buildAuthFailResp(authResultVO);
            }
        } else {
            AuthResultVO authResultVO = authService.auth(true, username, ActionId.CREATE_WHITELIST);
            if (!authResultVO.isPass()) {
                return ServiceResponse.buildAuthFailResp(authResultVO);
            }
        }
        return ServiceResponse.buildSuccessResp(whiteIPService.saveWhiteIP(username, createUpdateReq));
    }

    @Override
    public ServiceResponse<WhiteIPRecordVO> getWhiteIPDetailById(String username, Long id) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.MANAGE_WHITELIST);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(whiteIPService.getWhiteIPDetailById(username, id));
    }

    @Override
    public ServiceResponse<List<CloudAreaInfoVO>> listCloudAreas(String username) {
        return ServiceResponse.buildSuccessResp(whiteIPService.listCloudAreas(username));
    }

    @Override
    public ServiceResponse<List<ActionScopeVO>> listActionScope(String username) {
        return ServiceResponse.buildSuccessResp(whiteIPService.listActionScope(username));
    }

    @Override
    public ServiceResponse<Long> deleteWhiteIPById(String username, Long id) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.MANAGE_WHITELIST);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(whiteIPService.deleteWhiteIPById(username, id));
    }
}
