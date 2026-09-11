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

package com.tencent.bk.job.manage.service.template;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.manage.config.LocalFileConfigForManage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 作业模板中本地文件（file_type=2）的制品库信息补齐。
 * 调用方只提供上传时拿到的相对路径，hash 与大小由服务端查询制品库补齐。
 */
@Slf4j
@Service
public class TemplateLocalFileService {

    private final ArtifactoryHelper artifactoryHelper;
    private final LocalFileConfigForManage localFileConfigForManage;
    private final ArtifactoryClient artifactoryClient;

    public TemplateLocalFileService(ArtifactoryHelper artifactoryHelper,
                                    LocalFileConfigForManage localFileConfigForManage,
                                    @Qualifier("jobArtifactoryClient") ArtifactoryClient artifactoryClient) {
        this.artifactoryHelper = artifactoryHelper;
        this.localFileConfigForManage = localFileConfigForManage;
        this.artifactoryClient = artifactoryClient;
    }

    /**
     * 查询本地文件的 hash 与大小。
     * 路径必须位于当前业务的上传目录下，否则视为越权访问他人上传的文件。
     *
     * @param appId    业务 ID
     * @param filePath generate_local_file_upload_url 返回的相对路径
     */
    public LocalFileDetail getFileDetail(Long appId, String filePath) {
        checkPathBelongsToApp(appId, filePath);
        NodeDTO node = queryFileNode(filePath);
        if (node == null || Boolean.TRUE.equals(node.getFolder())) {
            throw buildFileNotFoundException(filePath);
        }
        return new LocalFileDetail(node.getMd5(), node.getSize());
    }

    /**
     * 上传路径的约定格式为 {appId}/{uuid}/{username}/{fileName}，首段即归属业务。
     */
    private void checkPathBelongsToApp(Long appId, String filePath) {
        if (StringUtils.isBlank(filePath)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"file_list", "local file path cannot be blank"});
        }
        String normalizedPath = StringUtils.strip(filePath.replace("\\", "/"), "/");
        if (normalizedPath.contains("..")) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"file_list", "local file path cannot contain '..': " + filePath});
        }
        String firstSegment = StringUtils.substringBefore(normalizedPath, "/");
        if (!String.valueOf(appId).equals(firstSegment)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"file_list",
                    "local file path does not belong to current scope: " + filePath});
        }
    }

    private NodeDTO queryFileNode(String filePath) {
        String fullPath = PathUtil.joinFilePath(
            artifactoryHelper.getJobRealProject() + "/" + localFileConfigForManage.getLocalUploadRepo(),
            filePath
        );
        try {
            return artifactoryClient.getFileNode(fullPath);
        } catch (InternalException e) {
            if (ErrorCode.CAN_NOT_FIND_NODE_IN_ARTIFACTORY == e.getErrorCode()) {
                throw buildFileNotFoundException(filePath);
            }
            throw e;
        }
    }

    private NotFoundException buildFileNotFoundException(String filePath) {
        return new NotFoundException(
            "local file not found in artifactory",
            ErrorCode.LOCAL_FILE_NOT_EXIST_IN_BACKEND,
            new String[]{filePath, String.valueOf(localFileConfigForManage.getExpireDays())}
        );
    }

    @Getter
    public static class LocalFileDetail {
        private final String fileHash;
        private final Long fileSize;

        public LocalFileDetail(String fileHash, Long fileSize) {
            this.fileHash = fileHash;
            this.fileSize = fileSize;
        }
    }
}
