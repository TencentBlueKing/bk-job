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

package com.tencent.bk.job.execute.task;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.model.dto.PageData;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.config.ArtifactoryConfig;
import com.tencent.bk.job.execute.config.LogExportConfig;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.constants.Consts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * 清理制品库/本地NFS中存储的日志导出临时文件
 */
@Slf4j
@Component
public class LogExportFileCleanTask {

    private final LogExportConfig logExportConfig;
    private final ArtifactoryConfig artifactoryConfig;
    private final StorageSystemConfig storageSystemConfig;
    private final ArtifactoryClient artifactoryClient;

    public LogExportFileCleanTask(LogExportConfig logExportConfig,
                                  ArtifactoryConfig artifactoryConfig,
                                  StorageSystemConfig storageSystemConfig,
                                  ArtifactoryClient artifactoryClient) {
        this.logExportConfig = logExportConfig;
        this.artifactoryConfig = artifactoryConfig;
        this.storageSystemConfig = storageSystemConfig;
        this.artifactoryClient = artifactoryClient;
    }

    public void execute() {
        if (JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY
            .equals(logExportConfig.getStorageBackend())) {
            // 清理制品库临时文件
            cleanArtifactoryLogExportFile();
        } else {
            // 清理本地NFS文件
            cleanLocalLogExportFile();
        }
    }

    /*
     * 从制品库中删除nodeList内指定的过期节点
     * @param nodeList 需要检查的节点列表
     * @return 删除的节点数
     */
    private int deleteExpiredNodes(List<NodeDTO> nodeList) {
        int deletedNodeNum = 0;
        for (NodeDTO nodeDTO : nodeList) {
            try {
                // 从制品库删除有效日期前创建的节点
                LocalDateTime endNodeLastDate = DateUtils.convertFromStringDate(
                    nodeDTO.getLastModifiedDate(), "yyyy-MM-ddTHH:mm:ss.SSS"
                );
                if (endNodeLastDate
                    .plusDays(logExportConfig.getArtifactoryFileExpireDays())
                    .compareTo(LocalDateTime.now()) < 0) {
                    artifactoryClient.deleteNode(
                        artifactoryConfig.getArtifactoryJobProject(),
                        logExportConfig.getLogExportRepo(),
                        nodeDTO.getFullPath()
                    );
                    log.info("logExportFile {} in artifactory deleted", nodeDTO.getFullPath());
                    deletedNodeNum += 1;
                }
            } catch (Throwable t) {
                FormattingTuple msg = MessageFormatter.format(
                    "Fail to check and process expire logExport node {}",
                    nodeDTO.getFullPath()
                );
                log.warn(msg.getMessage(), t);
            }
        }
        return deletedNodeNum;
    }

    /**
     * 清理制品库的执行日志导出临时文件
     */
    private void cleanArtifactoryLogExportFile() {
        log.info("begin to cleanArtifactoryLogExportFile");
        int start = 0;
        int pageSize = 100;
        int deletedNodeNum;
        List<NodeDTO> nodeList;
        do {
            PageData<NodeDTO> nodePage = artifactoryClient.listNode(
                artifactoryConfig.getArtifactoryJobProject(),
                logExportConfig.getLogExportRepo(),
                "/",
                start,
                pageSize
            );
            if (nodePage == null) {
                log.warn("nodePage is null");
                return;
            }
            nodeList = nodePage.getRecords();
            if (CollectionUtils.isNotEmpty(nodeList)) {
                deletedNodeNum = deleteExpiredNodes(nodeList);
                start += pageSize - deletedNodeNum;
            } else {
                deletedNodeNum = 0;
            }
        } while (deletedNodeNum != 0 || (CollectionUtils.isNotEmpty(nodeList)));
        log.info("cleanArtifactoryLogExportFile finished");
    }

    /**
     * 清理本地的执行日志导出临时文件
     */
    private void cleanLocalLogExportFile() {
        log.info("begin to cleanLocalLogExportFile");
        // 清理指定日期之前的执行日志导出临时文件
        Date thresholdDate = new Date(
            System.currentTimeMillis() - logExportConfig.getArtifactoryFileExpireDays() * 3600 * 24 * 1000
        );
        String logExportFileDirPath = PathUtil.joinFilePath(
            storageSystemConfig.getJobStorageRootPath(), Consts.LOG_EXPORT_DIR_NAME
        );
        Iterator<File> fileIterator = FileUtils.iterateFiles(
            new File(logExportFileDirPath),
            new AgeFileFilter(thresholdDate),
            TrueFileFilter.TRUE
        );
        while (fileIterator.hasNext()) {
            File file = fileIterator.next();
            log.info("Delete logExport file {}", file.getPath());
            FileUtils.deleteQuietly(file);
        }
        log.info("cleanLocalLogExportFile finished");
    }
}
