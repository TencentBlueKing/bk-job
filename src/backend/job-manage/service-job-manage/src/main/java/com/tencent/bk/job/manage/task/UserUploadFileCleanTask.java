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

package com.tencent.bk.job.manage.task;

import com.google.common.collect.Sets;
import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.model.dto.PageData;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.config.ArtifactoryConfig;
import com.tencent.bk.job.manage.config.LocalFileConfigForManage;
import com.tencent.bk.job.manage.config.StorageSystemConfig;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @since 25/1/2021 21:19
 */
@Slf4j
@Component
public class UserUploadFileCleanTask {

    private static final String FILE_CLEAN_TASK_LOCK_KEY = "user:upload:file:clean";
    private final String uploadPath;
    private final File uploadDirectory;
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanService taskPlanService;
    private final ArtifactoryConfig artifactoryConfig;
    private final LocalFileConfigForManage localFileConfigForManage;
    private final ArtifactoryClient artifactoryClient;

    public UserUploadFileCleanTask(
        StorageSystemConfig storageSystemConfig,
        TaskTemplateService taskTemplateService,
        TaskPlanService taskPlanService,
        ArtifactoryConfig artifactoryConfig,
        LocalFileConfigForManage localFileConfigForManage,
        ArtifactoryClient artifactoryClient
    ) {
        this.uploadPath = storageSystemConfig.getJobStorageRootPath() + "/localupload/";
        this.taskTemplateService = taskTemplateService;
        this.taskPlanService = taskPlanService;
        this.artifactoryConfig = artifactoryConfig;
        this.localFileConfigForManage = localFileConfigForManage;
        this.artifactoryClient = artifactoryClient;
        this.uploadDirectory = new File(uploadPath);
    }

    public void execute() {
        String executeId = UUID.randomUUID().toString();

        try {
            if (LockUtils.tryGetDistributedLock(FILE_CLEAN_TASK_LOCK_KEY, executeId, 3600_000L)) {
                Set<String> skipFile = loadFileListFromDb();
                processFile(skipFile);
            } else {
                log.info("Some one else is running this task! Skip!");
            }
        } catch (Exception e) {
            log.error("Error while running user upload file clean task!", e);
        } finally {
            LockUtils.releaseDistributedLock(FILE_CLEAN_TASK_LOCK_KEY, executeId);
        }
    }

    private Set<String> loadFileListFromDb() {
        Set<String> templateFile = taskTemplateService.listLocalFiles();
        Set<String> planFile = taskPlanService.listLocalFiles();
        return Sets.union(templateFile, planFile);
    }

    private void processFile(Set<String> skipFile) {
        if (JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(localFileConfigForManage.getStorageBackend())) {
            processArtifactoryFile(skipFile);
        } else {
            processLocalFile(skipFile);
        }
    }

    private NodeDTO getEndNode(NodeDTO nodeDTO) {
        if (nodeDTO == null) return null;
        PageData<NodeDTO> nodePage = artifactoryClient.listNode(
            artifactoryConfig.getArtifactoryJobProject(),
            localFileConfigForManage.getLocalUploadRepo(),
            nodeDTO.getFullPath(),
            0,
            1
        );
        if (nodePage == null || nodePage.getRecords() == null || nodePage.getRecords().isEmpty()) {
            return nodeDTO;
        }
        return getEndNode(nodePage.getRecords().get(0));
    }

    /**
     * 从制品库中删除nodeList内指定的过期节点
     *
     * @param nodeList 需要检查的节点列表
     * @param skipPath 需要跳过的路径
     * @return 删除的节点数
     */
    private int deleteExpiredNodes(List<NodeDTO> nodeList, Set<String> skipPath) {
        int deletedNodeNum = 0;
        for (NodeDTO nodeDTO : nodeList) {
            try {
                NodeDTO endNode = getEndNode(nodeDTO);
                String endNodePath = StringUtil.removePrefix(endNode.getFullPath(), "/");
                if (skipPath.contains(endNodePath)) {
                    log.info("Skip artifactory file {}", endNodePath);
                    continue;
                }
                // 从制品库删除有效日期前创建的节点
                LocalDateTime endNodeLastDate = DateUtils.convertFromStringDate(
                    endNode.getLastModifiedDate(), "yyyy-MM-ddTHH:mm:ss.SSS"
                );
                if (endNodeLastDate
                    .plusDays(localFileConfigForManage.getExpireDays())
                    .compareTo(LocalDateTime.now()) < 0) {
                    artifactoryClient.deleteNode(
                        artifactoryConfig.getArtifactoryJobProject(),
                        localFileConfigForManage.getLocalUploadRepo(),
                        nodeDTO.getFullPath()
                    );
                    log.info("localFile {} in artifactory deleted", endNode.getFullPath());
                    deletedNodeNum += 1;
                }
            } catch (Throwable t) {
                FormattingTuple msg = MessageFormatter.format(
                    "Fail to check and process expire node {}",
                    nodeDTO.getFullPath()
                );
                log.warn(msg.getMessage(), t);
            }
        }
        return deletedNodeNum;
    }

    /**
     * 清理蓝鲸制品库中的临时文件
     *
     * @param skipFile 需要跳过的文件
     */
    private void processArtifactoryFile(Set<String> skipFile) {
        int start = 0;
        int pageSize = 100;
        int deletedNodeNum;
        List<NodeDTO> nodeList;
        do {
            PageData<NodeDTO> nodePage = artifactoryClient.listNode(
                artifactoryConfig.getArtifactoryJobProject(),
                localFileConfigForManage.getLocalUploadRepo(),
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
                deletedNodeNum = deleteExpiredNodes(nodeList, skipFile);
                start += pageSize - deletedNodeNum;
            } else {
                deletedNodeNum = 0;
            }
        } while (deletedNodeNum != 0 || (CollectionUtils.isNotEmpty(nodeList)));
    }

    /**
     * 清理存储于本地的临时文件
     *
     * @param skipFile 需要跳过的文件
     */
    private void processLocalFile(Set<String> skipFile) {
        Date thresholdDate = new Date(
            System.currentTimeMillis() - localFileConfigForManage.getExpireDays() * 3600 * 24 * 1000
        );
        Iterator<File> fileIterator = FileUtils.iterateFiles(uploadDirectory, new AgeFileFilter(thresholdDate),
            TrueFileFilter.TRUE);
        while (fileIterator.hasNext()) {
            File aFile = fileIterator.next();
            if (skipFile.contains(aFile.getPath().replaceAll(uploadPath, ""))) {
                if (log.isDebugEnabled()) {
                    log.debug("Skip file {}", aFile.getPath());
                }
                continue;
            } else {
                log.info("Delete file {}", aFile.getPath());
                FileUtils.deleteQuietly(aFile);
            }

            try {
                deleteEmptyDirectory(aFile.getParentFile());
                deleteEmptyDirectory(aFile.getParentFile().getParentFile());
            } catch (Exception e) {
                log.warn("Error while delete empty parent of file {}", aFile.getPath());
            }
        }
    }

    private void deleteEmptyDirectory(File directory) {
        if (directory == null) {
            log.warn("Directory is null!");
            return;
        }

        if (isEmpty(directory.toPath())) {
            boolean delete = directory.delete();
            if (delete) {
                log.info("Delete empty directory {}", directory.getPath());
            } else {
                log.warn("Delete directory {} failed!", directory.getPath());
            }
        } else {
            log.debug("Directory {} not empty!", directory.getPath());
        }
    }

    private boolean isEmpty(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return !entries.findFirst().isPresent();
            } catch (IOException e) {
                return false;
            }
        }

        return false;
    }
}
