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
import com.tencent.bk.job.common.util.http.HttpReqGenUtil;
import com.tencent.bk.job.file.worker.model.req.BaseReq;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.service.CredentialService;
import com.tencent.bk.job.manage.model.credential.CommonCredential;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BaseRemoteFileReqGenServiceImpl {

    protected final CredentialService credentialService;

    public BaseRemoteFileReqGenServiceImpl(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    protected String getCompleteUrl(FileWorkerDTO fileWorkerDTO, String url) {
        String host = fileWorkerDTO.getAccessHost();
        Integer port = fileWorkerDTO.getAccessPort();
        return "http://" + host + ":" + port.toString() + "/worker/api" + url;
    }

    protected String fillBaseReqGetUrl(BaseReq req, FileWorkerDTO fileWorkerDTO,
                                       FileSourceDTO fileSourceDTO, String url) {
        String completeUrl = getCompleteUrl(fileWorkerDTO, url);
        String credentialId = fileSourceDTO.getCredentialId();
        CommonCredential commonCredential = null;
        if (StringUtils.isNotBlank(credentialId)) {
            commonCredential = credentialService.getCredentialById(fileSourceDTO.getAppId(),
                fileSourceDTO.getCredentialId());
        }
        if (commonCredential != null) {
            req.setCredential(commonCredential);
            log.debug("Credential of id {} is {}", credentialId, commonCredential);
        } else if (StringUtils.isNotBlank(credentialId)) {
            log.warn("Cannot find credential by id {}, fileSource={}", credentialId, fileSourceDTO);
        }
        req.setFileSourceTypeCode(fileSourceDTO.getFileSourceType().getCode());
        req.setFileSourceInfoMap(fileSourceDTO.getFileSourceInfoMap());
        return completeUrl;
    }

    protected HttpReq genRemoteFileReq(String url, Object body) {
        return HttpReqGenUtil.genSimpleJsonReq(url, body);
    }
}
