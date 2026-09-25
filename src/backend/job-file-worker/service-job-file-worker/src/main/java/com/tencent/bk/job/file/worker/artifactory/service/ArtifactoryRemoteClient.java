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

package com.tencent.bk.job.file.worker.artifactory.service;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.http.HttpUrlSafetyUtils;
import com.tencent.bk.job.file.worker.model.FileMetaData;
import com.tencent.bk.job.file.worker.service.RemoteClient;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.InputStream;
import java.util.List;

@Slf4j
public class ArtifactoryRemoteClient extends ArtifactoryClient implements RemoteClient {

    public ArtifactoryRemoteClient(String baseUrl, String username, String password, MeterRegistry meterRegistry) {
        super(requireSafeRemoteBaseUrl(baseUrl), username, password, meterRegistry);
    }

    private static String requireSafeRemoteBaseUrl(String baseUrl) {
        String host = HttpUrlSafetyUtils.parseHttpUrlHost(baseUrl);
        if (host == null || HttpUrlSafetyUtils.isResolvedToDangerousAddress(host,
            HttpUrlSafetyUtils.DEFAULT_HOST_RESOLVER)) {
            throw new InvalidParamException(ErrorCode.BK_ARTIFACTORY_BASE_URL_INVALID);
        }
        return baseUrl;
    }

    @Override
    public FileMetaData getFileMetaData(String filePath) {
        List<String> path = parsePath(filePath);
        NodeDTO nodeDTO = super.queryNodeDetailForFileDownload(path.get(0), path.get(1), path.get(2));
        return new FileMetaData(nodeDTO.getSize(), nodeDTO.getMd5());
    }

    @Override
    public Pair<InputStream, HttpRequestBase> getFileInputStream(String filePath) throws ServiceException {
        return super.getFileInputStream(filePath);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
