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

package com.tencent.bk.job.file_gateway.service.remote.impl;

import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.file.worker.model.req.BaseReq;
import com.tencent.bk.job.file.worker.model.req.ExecuteActionReq;
import com.tencent.bk.job.file.worker.model.req.ListFileNodeReq;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.service.CredentialService;
import com.tencent.bk.job.file_gateway.service.remote.FileSourceReqGenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
public class FileSourceReqGenServiceImpl extends BaseRemoteFileReqGenServiceImpl implements FileSourceReqGenService {

    @Autowired
    public FileSourceReqGenServiceImpl(CredentialService credentialService) {
        super(credentialService);
    }

    @Override
    public HttpReq genFileAvailableReq(Long appId, FileWorkerDTO fileWorkerDTO, FileSourceDTO fileSourceDTO) {
        BaseReq req = new BaseReq();
        String url = fillBaseReqGetUrl(req, fileWorkerDTO, fileSourceDTO, "/file/available");
        return genRemoteFileReq(url, req);
    }

    @Override
    public HttpReq genListFileNodeReq(Long appId, String path, String name, Integer start, Integer pageSize,
                                      FileWorkerDTO fileWorkerDTO, FileSourceDTO fileSourceDTO) {
        ListFileNodeReq req = new ListFileNodeReq();
        String url = fillBaseReqGetUrl(req, fileWorkerDTO, fileSourceDTO, "/file/listFileNode");
        req.setPath(path);
        req.setName(name);
        req.setStart(start);
        req.setPageSize(pageSize);
        return genRemoteFileReq(url, req);
    }

    @Override
    public HttpReq genExecuteActionReq(Long appId, String actionCode, Map<String, Object> actionParams,
                                       FileWorkerDTO fileWorkerDTO, FileSourceDTO fileSourceDTO) {
        ExecuteActionReq req = new ExecuteActionReq();
        String url = fillBaseReqGetUrl(req, fileWorkerDTO, fileSourceDTO, "/file/executeAction");
        req.setActionCode(actionCode);
        req.setParams(actionParams);
        return genRemoteFileReq(url, req);
    }

}
