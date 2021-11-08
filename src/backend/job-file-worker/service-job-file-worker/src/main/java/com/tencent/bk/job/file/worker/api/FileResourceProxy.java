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

package com.tencent.bk.job.file.worker.api;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.file.worker.consts.FileSourceTypeEnum;
import com.tencent.bk.job.file.worker.cos.service.RemoteClient;
import com.tencent.bk.job.file.worker.model.req.BaseReq;
import com.tencent.bk.job.file.worker.model.req.ExecuteActionReq;
import com.tencent.bk.job.file.worker.model.req.ListFileNodeReq;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
public class FileResourceProxy implements IFileResource {

    private final IFileResource cosFileResource;
    private final IFileResource artifactoryFileResource;

    @Autowired
    public FileResourceProxy(@Qualifier("COSFileResource") IFileResource cosFileResource, @Qualifier(
        "ArtifactoryFileResource") IFileResource artifactoryFileResource) {
        this.cosFileResource = cosFileResource;
        this.artifactoryFileResource = artifactoryFileResource;
    }

    public IFileResource chooseFileResource(BaseReq req) {
        if (FileSourceTypeEnum.TENCENT_CLOUD_COS.name().equals(req.getFileSourceTypeCode())) {
            return cosFileResource;
        } else if (FileSourceTypeEnum.BLUEKING_ARTIFACTORY.name().equals(req.getFileSourceTypeCode())) {
            return artifactoryFileResource;
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"fileSourceTypeCode"});
        }
    }

    @Override
    public RemoteClient getRemoteClient(BaseReq req) {
        return chooseFileResource(req).getRemoteClient(req);
    }

    @Override
    public InternalResponse<Boolean> isFileAvailable(BaseReq req) {
        return chooseFileResource(req).isFileAvailable(req);
    }

    @Override
    public InternalResponse<FileNodesDTO> listFileNode(ListFileNodeReq req) {
        return chooseFileResource(req).listFileNode(req);
    }

    @Override
    public InternalResponse<Boolean> executeAction(ExecuteActionReq req) {
        return chooseFileResource(req).executeAction(req);
    }
}
