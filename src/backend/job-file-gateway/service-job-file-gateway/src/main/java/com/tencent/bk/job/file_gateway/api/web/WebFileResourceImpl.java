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

package com.tencent.bk.job.file_gateway.api.web;

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.file_gateway.model.req.common.ExecuteActionReq;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesVO;
import com.tencent.bk.job.file_gateway.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@Slf4j
public class WebFileResourceImpl implements WebFileResource {

    private final WebFileSourceResourceImpl webFileSourceResource;
    private final FileService fileService;

    @Autowired
    public WebFileResourceImpl(WebFileSourceResourceImpl webFileSourceResource, FileService fileService) {
        this.webFileSourceResource = webFileSourceResource;
        this.fileService = fileService;
    }


    @Override
    public Response<FileNodesVO> listFileNode(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              Integer fileSourceId,
                                              String path,
                                              String name,
                                              Integer start,
                                              Integer pageSize) {
        try {
            Long appId = appResourceScope.getAppId();
            AuthResult viewFileSourceAuthResult = webFileSourceResource.checkViewFileSourcePermission(username,
                appId, fileSourceId);
            if (!viewFileSourceAuthResult.isPass()) {
                throw new PermissionDeniedException(viewFileSourceAuthResult);
            }
            AuthResult manageFileSourceAuthResult =
                webFileSourceResource.checkManageFileSourcePermission(username, appId, fileSourceId);
            FileNodesVO fileNodesVO = fileService.listFileNode(username, appId, fileSourceId, path, name, start,
                pageSize);
            for (Map<String, Object> map : fileNodesVO.getPageData().getData()) {
                map.put("canManage", manageFileSourceAuthResult.isPass());
            }
            return Response.buildSuccessResp(fileNodesVO);
        } catch (ServiceException e) {
            return Response.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public Response<Boolean> executeAction(String username,
                                           AppResourceScope appResourceScope,
                                           String scopeType,
                                           String scopeId,
                                           Integer fileSourceId,
                                           ExecuteActionReq req) {
        try {
            Long appId = appResourceScope.getAppId();
            AuthResult authResult = webFileSourceResource.checkManageFileSourcePermission(username, appId,
                fileSourceId);
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
            return Response.buildSuccessResp(fileService.executeAction(username, appId, fileSourceId, req));
        } catch (ServiceException e) {
            return Response.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }
}
