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

package com.tencent.bk.job.execute.engine.prepare.local;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FileDownloadException;
import com.tencent.bk.job.common.model.error.FileDownloadErrorDTO;
import com.tencent.bk.job.common.util.FilePathUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.file.FileUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.constants.Consts;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LocalFileDownloadTask implements Callable<Boolean> {

    private final StepInstanceDTO stepInstance;
    private final ArtifactoryClient artifactoryClient;
    private final String artifactoryProject;
    private final String artifactoryRepo;
    private final String jobStorageRootPath;
    private final FileDetailDTO file;
    private final ExecuteObject sourceExecuteObject;
    private final LogService logService;

    public LocalFileDownloadTask(StepInstanceDTO stepInstance,
                                 ArtifactoryClient artifactoryClient,
                                 String artifactoryProject,
                                 String artifactoryRepo,
                                 String jobStorageRootPath,
                                 FileDetailDTO file,
                                 ExecuteObject sourceExecuteObject,
                                 LogService logService) {
        this.stepInstance = stepInstance;
        this.artifactoryClient = artifactoryClient;
        this.artifactoryProject = artifactoryProject;
        this.artifactoryRepo = artifactoryRepo;
        this.jobStorageRootPath = jobStorageRootPath;
        this.file = file;
        this.sourceExecuteObject = sourceExecuteObject;
        this.logService = logService;
    }

    @Override
    public Boolean call() {
        try {
            return doCall();
        } catch (Throwable t) {
            FormattingTuple msg = MessageFormatter.format(
                "[{}]:Unexpected error when prepare localFile {}",
                stepInstance.getUniqueKey(),
                file.getFilePath()
            );
            log.error(msg.getMessage(), t);
            writeFailureLog(FileDownloadException.resolveError(t).toJson());
            return false;
        }
    }

    private Boolean doCall() {
        String filePath = file.getFilePath();
        // 制品库的完整路径
        NodeDTO nodeDTO = artifactoryClient.queryNodeDetail(artifactoryProject, artifactoryRepo, filePath);
        if (nodeDTO == null) {
            log.warn(
                "[{}]:File {} not exists in project {} repo {}",
                stepInstance.getUniqueKey(),
                filePath,
                artifactoryProject,
                artifactoryRepo
            );
            writeFailureLog(new FileDownloadErrorDTO(
                null,
                String.valueOf(ErrorCode.CAN_NOT_FIND_NODE_IN_ARTIFACTORY),
                "can not find node by filePath: " + filePath,
                null
            ).toJson());
            return false;
        }
        // 本地存储路径
        String localPath = PathUtil.joinFilePath(jobStorageRootPath, Consts.LOCAL_FILE_DIR_NAME);
        localPath = PathUtil.joinFilePath(localPath, filePath);
        // 如果本地文件还未下载就已存在并且Md5正确，直接完成准备阶段
        if (currentLocalFileValid(localPath, nodeDTO)) {
            // 将最后修改时间设置为当前时间，避免在分发过程中被清理
            File localFile = new File(localPath);
            boolean lastModifyTimeSet = localFile.setLastModified(System.currentTimeMillis());
            if (!lastModifyTimeSet) {
                log.warn("Fail to set lastModifyTime for {}", localPath);
            }
            return true;
        }
        Pair<InputStream, HttpRequestBase> pair = artifactoryClient.getFileInputStream(
            artifactoryProject,
            artifactoryRepo,
            filePath
        );
        InputStream ins = pair.getLeft();
        HttpRequestBase req = pair.getRight();
        Long fileSize = nodeDTO.getSize();
        // 保存到本地临时目录
        AtomicInteger speed = new AtomicInteger(0);
        AtomicInteger process = new AtomicInteger(0);
        try {
            log.debug(
                "[{}]:begin to download {} to {}",
                stepInstance.getUniqueKey(),
                filePath,
                localPath
            );
            FileUtil.writeInsToFile(ins, localPath, fileSize, speed, process);
            log.info(
                "[{}]:success: {} -> {}",
                stepInstance.getUniqueKey(),
                filePath,
                localPath
            );
            return true;
        } catch (InterruptedException e) {
            log.warn(
                "[{}]:Interrupted:Download {} to {}",
                stepInstance.getUniqueKey(),
                filePath,
                localPath
            );
        } catch (Exception e) {
            log.error(
                "[{}]:Fail to download {} to {}",
                stepInstance.getUniqueKey(),
                filePath,
                localPath,
                e
            );
            writeFailureLog(FileDownloadException.resolveError(e).toJson());
        } finally {
            if (req != null) {
                req.releaseConnection();
            }
        }
        return false;
    }

    private void writeFailureLog(String detail) {
        try {
            String filePath = file.getFilePath();
            String displayFileName = FilePathUtils.parseDirAndFileName(filePath).getRight();
            ServiceFileTaskLogDTO fileTaskLog = logService.buildUploadServiceFileTaskLogDTO(
                stepInstance,
                TaskFileTypeEnum.LOCAL,
                filePath,
                displayFileName,
                sourceExecuteObject,
                FileDistStatusEnum.FAILED,
                "--",
                "--",
                "0%",
                "FileName: " + filePath + " FileSize: -- Speed: -- Progress: 0% Detail: " + detail
            );
            Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executeObjectLogs = new HashMap<>();
            logService.addFileTaskLog(stepInstance, executeObjectLogs, sourceExecuteObject, fileTaskLog);
            logService.writeFileLogs(stepInstance.getCreateTime(), new ArrayList<>(executeObjectLogs.values()));
        } catch (Exception e) {
            log.error("[{}]:Fail to write local file download failure log", stepInstance.getUniqueKey(), e);
        }
    }

    /**
     * 判断当前的本地文件是否可以直接使用
     *
     * @param localPath 本地文件绝对路径
     * @param nodeDTO   制品库中的节点信息
     * @return 当前的本地文件是否可以直接使用
     */
    private boolean currentLocalFileValid(String localPath, NodeDTO nodeDTO) {
        File localFile = new File(localPath);
        if (!localFile.exists()) {
            return false;
        }
        String md5 = nodeDTO.getMd5();
        if (StringUtils.isBlank(md5)) {
            log.warn("Md5 from node is blank, node={}", nodeDTO);
            return false;
        }
        String lastModifyTimeFormat = "yyyy-MM-dd HH:mm:ss.SSS";
        try (InputStream fis = new FileInputStream(localFile)) {
            String localFileMd5 = DigestUtils.md5Hex(fis);
            if (!md5.equals(localFileMd5)) {
                log.info(
                    "[{}]:local file {} exists with wrong md5:{}, expect md5:{}," +
                        " lastModifyTime:{}, download again",
                    stepInstance.getUniqueKey(),
                    localPath,
                    localFileMd5,
                    md5,
                    TimeUtil.formatTime(localFile.lastModified(), lastModifyTimeFormat)
                );
                return false;
            }
            log.info(
                "[{}]:local file {} already exists with same md5:{}, lastModifyTime:{}",
                stepInstance.getUniqueKey(),
                localPath,
                localFileMd5,
                TimeUtil.formatTime(localFile.lastModified(), lastModifyTimeFormat)
            );
            return true;
        } catch (IOException e) {
            String msg = MessageFormatter.format(
                "Fail to check md5 of localFile: {}, download again",
                localPath
            ).getMessage();
            log.warn(msg, e);
        }
        return false;
    }
}
