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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.req.common.ExecuteActionReq;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesVO;
import com.tencent.bk.job.file_gateway.service.DispatchService;
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
    private final DispatchService dispatchService;

    @Autowired
    public WebFileResourceImpl(WebFileSourceResourceImpl webFileSourceResource, FileService fileService,
                               DispatchService dispatchService) {
        this.webFileSourceResource = webFileSourceResource;
        this.fileService = fileService;
        this.dispatchService = dispatchService;
    }

    private FileWorkerDTO getFileWorker(Long appId, FileSourceDTO fileSourceDTO) {
        if (fileSourceDTO == null) {
            throw new ServiceException(ErrorCode.FILE_SOURCE_NOT_EXIST);
        }
        return dispatchService.findBestFileWorker(fileSourceDTO);
    }

    @Override
    public ServiceResponse<FileNodesVO> listFileNode(String username, Long appId, Integer fileSourceId, String path,
                                                     String name, Integer start, Integer pageSize) {
        try {
            AuthResultVO viewFileSourceAuthResultVO = webFileSourceResource.checkViewFileSourcePermission(username,
                appId, fileSourceId);
            if (!viewFileSourceAuthResultVO.isPass()) {
                return ServiceResponse.buildAuthFailResp(viewFileSourceAuthResultVO);
            }
            AuthResultVO manageFileSourceAuthResultVO =
                webFileSourceResource.checkManageFileSourcePermission(username, appId, fileSourceId);
            FileNodesVO fileNodesVO = fileService.listFileNode(username, appId, fileSourceId, path, name, start,
                pageSize);
            for (Map<String, Object> map : fileNodesVO.getPageData().getData()) {
                map.put("canManage", manageFileSourceAuthResultVO.isPass());
            }
            return ServiceResponse.buildSuccessResp(fileNodesVO);
        } catch (ServiceException e) {
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public ServiceResponse<Boolean> executeAction(String username, Long appId, Integer fileSourceId,
                                                  ExecuteActionReq req) {
        try {
            AuthResultVO authResultVO = webFileSourceResource.checkManageFileSourcePermission(username, appId,
                fileSourceId);
            if (!authResultVO.isPass()) {
                return ServiceResponse.buildAuthFailResp(authResultVO);
            }
            return ServiceResponse.buildSuccessResp(fileService.executeAction(username, appId, fileSourceId, req));
        } catch (ServiceException e) {
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }
}
