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

package com.tencent.bk.job.manage.task;

import com.google.common.collect.Sets;
import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.model.dto.PageData;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLock;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLockConfig;
import com.tencent.bk.job.common.redis.util.LockResult;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.file.FileUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.config.LocalFileConfigForManage;
import com.tencent.bk.job.manage.config.StorageSystemConfig;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @since 25/1/2021 21:19
 */
@Slf4j
@Component
public class UserUploadFileCleanTask {

    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final String FILE_CLEAN_TASK_LOCK_KEY = "user:upload:file:clean";
    private final String uploadPath;
    private final File uploadDirectory;
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanService taskPlanService;
    private final ArtifactoryConfig artifactoryConfig;
    private final LocalFileConfigForManage localFileConfigForManage;
    private final ArtifactoryClient artifactoryClient;
    private final RedisTemplate<String, String> redisTemplate;

    public UserUploadFileCleanTask(
        StorageSystemConfig storageSystemConfig,
        TaskTemplateService taskTemplateService,
        TaskPlanService taskPlanService,
        ArtifactoryConfig artifactoryConfig,
        LocalFileConfigForManage localFileConfigForManage,
        @Qualifier("jobArtifactoryClient") ArtifactoryClient artifactoryClient,
        RedisTemplate<String, String> redisTemplate
    ) {
        this.uploadPath = storageSystemConfig.getJobStorageRootPath() + "/localupload/";
        this.taskTemplateService = taskTemplateService;
        this.taskPlanService = taskPlanService;
        this.artifactoryConfig = artifactoryConfig;
        this.localFileConfigForManage = localFileConfigForManage;
        this.artifactoryClient = artifactoryClient;
        this.uploadDirectory = new File(uploadPath);
        this.redisTemplate = redisTemplate;
    }

    public void execute() {
        HeartBeatRedisLockConfig config = HeartBeatRedisLockConfig.getDefault();
        config.setHeartBeatThreadName("UserUploadFileCleanRedisKeyHeartBeatThread");
        HeartBeatRedisLock lock = new HeartBeatRedisLock(
            redisTemplate,
            FILE_CLEAN_TASK_LOCK_KEY,
            machineIp,
            config
        );
        LockResult lockResult = lock.lock();
        if (!lockResult.isLockGotten()) {
            log.info(
                "lock {} gotten by another machine: {}, return",
                FILE_CLEAN_TASK_LOCK_KEY,
                lockResult.getLockValue()
            );
            return;
        }
        try {
            Set<String> skipFile = loadFileListFromDb();
            processFile(skipFile);
        } catch (Exception e) {
            log.error("Error while running user upload file clean task!", e);
        } finally {
            lockResult.tryToRelease();
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

    @Getter
    @AllArgsConstructor
    static class DeleteNodeResult {
        // 删除的节点总数，含子节点
        int deletedNodeNum;
        // 当前节点是否被删除
        boolean currentNodeDeleted;

        public DeleteNodeResult(int deletedNodeNum) {
            this.deletedNodeNum = deletedNodeNum;
            this.currentNodeDeleted = false;
        }
    }

    /**
     * 检查并删除过期的节点及其子节点
     *
     * @param node     目标节点
     * @param skipPath 需要跳过的路径
     * @return 节点删除结果
     */
    private DeleteNodeResult checkAndDeleteExpiredNodeAndChild(NodeDTO node, Set<String> skipPath) {
        if (node == null) {
            return new DeleteNodeResult(0);
        }
        String nodePath = StringUtil.removePrefix(node.getFullPath(), "/");
        if (skipPath.contains(nodePath)) {
            log.info("Skip artifactory node {}", nodePath);
            return new DeleteNodeResult(0);
        }
        boolean isFolder = node.getFolder() != null && node.getFolder();
        if (!isFolder) {
            // 文件节点，直接检查并删除
            return checkAndDeleteFileNode(node);
        } else {
            // 目录节点，需要递归遍历检查并删除
            return checkAndDeleteDirNode(node, skipPath);
        }
    }

    private DeleteNodeResult checkAndDeleteDirNode(NodeDTO node, Set<String> skipPath) {
        int deletedNum = 0;
        // 1.先处理子节点
        PageData<NodeDTO> nodePage;
        int pageNumber = 1;
        int pageSize = 100;
        do {
            nodePage = artifactoryClient.listNode(
                artifactoryConfig.getArtifactoryJobProject(),
                localFileConfigForManage.getLocalUploadRepo(),
                node.getFullPath(),
                pageNumber,
                pageSize
            );
            if (isEmpty(nodePage)) {
                break;
            }
            List<NodeDTO> subNodeList = nodePage.getRecords();
            int subNodeDeletedNum = 0;
            for (NodeDTO subNode : subNodeList) {
                // 单个节点处理失败后直接跳过，继续处理后续节点。
                try {
                    DeleteNodeResult result = checkAndDeleteExpiredNodeAndChild(subNode, skipPath);
                    deletedNum += result.getDeletedNodeNum();
                    if (result.currentNodeDeleted) {
                        subNodeDeletedNum += 1;
                    }
                } catch (Exception e) {
                    String msg = MessageFormatter.format(
                        "Fail to checkAndDelete node:{}",
                        subNode.getFullPath()
                    ).getMessage();
                    log.warn(msg, e);
                }
            }
            if (subNodeDeletedNum == 0) {
                // 所有子节点本身都没有被删除，页码数才增加，否则当前页需要重新检查处理
                pageNumber += 1;
            }
        } while (!isEmpty(nodePage));
        // 2.根目录不删除
        if ("/".equals(node.getFullPath().trim())) {
            return new DeleteNodeResult(deletedNum);
        }
        // 3.非根目录再判断是否为空目录，若为空且创建日期过期了也删除
        //   此处的目录过期需要用创建时间判断，因为子目录/文件删除会导致父目录的最后修改时间更新
        if (isNodeCreatedTimeExpired(node) && isDirNodeEmpty(node)) {
            if (deleteNode(node)) {
                deletedNum += 1;
                log.info("Delete empty dirNode: {}", node.getFullPath());
                return new DeleteNodeResult(deletedNum, true);
            } else {
                log.warn("Fail to delete empty dirNode:{}", node.getFullPath());
            }
        }
        return new DeleteNodeResult(deletedNum);
    }

    private boolean isDirNodeEmpty(NodeDTO dirNode) {
        PageData<NodeDTO> nodePage = artifactoryClient.listNode(
            artifactoryConfig.getArtifactoryJobProject(),
            localFileConfigForManage.getLocalUploadRepo(),
            dirNode.getFullPath(),
            1,
            1
        );
        return nodePage != null && (nodePage.getRecords() == null || nodePage.getRecords().isEmpty());
    }

    private DeleteNodeResult checkAndDeleteFileNode(NodeDTO node) {
        if (isNodeLastModifyTimeExpired(node)) {
            if (deleteNode(node)) {
                return new DeleteNodeResult(1, true);
            } else {
                return new DeleteNodeResult(0);
            }
        }
        return new DeleteNodeResult(0);
    }

    private boolean isEmpty(PageData<NodeDTO> nodePage) {
        return nodePage == null || nodePage.getRecords() == null || nodePage.getRecords().isEmpty();
    }

    private boolean isNodeLastModifyTimeExpired(NodeDTO nodeDTO) {
        LocalDateTime lastModifyDateTime = parseLastModifyDateTime(nodeDTO);
        boolean result = isExpired(lastModifyDateTime);
        log.debug(
            "check node {}, lastModify={}, expired={}",
            nodeDTO.getFullPath(),
            nodeDTO.getLastModifiedDate(),
            result
        );
        return result;
    }

    private boolean isNodeCreatedTimeExpired(NodeDTO nodeDTO) {
        LocalDateTime createdDateTime = parseCreatedDateTime(nodeDTO);
        boolean result = isExpired(createdDateTime);
        log.debug(
            "check node {}, createdDateTime={}, expired={}",
            nodeDTO.getFullPath(),
            nodeDTO.getCreatedDate(),
            result
        );
        return result;
    }

    private LocalDateTime parseLastModifyDateTime(NodeDTO nodeDTO) {
        return parseDateTimeFromStr(nodeDTO.getLastModifiedDate());
    }

    private LocalDateTime parseCreatedDateTime(NodeDTO nodeDTO) {
        return parseDateTimeFromStr(nodeDTO.getCreatedDate());
    }

    private LocalDateTime parseDateTimeFromStr(String dateTimeStr) {
        return DateUtils.convertFromStringDateByPatterns(
            dateTimeStr,
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SS",
            "yyyy-MM-dd'T'HH:mm:ss.S",
            "yyyy-MM-dd'T'HH:mm:ss.",
            "yyyy-MM-dd'T'HH:mm:ss"
        );
    }

    private boolean isExpired(LocalDateTime dateTime) {
        return dateTime.plusDays(localFileConfigForManage.getExpireDays()).isBefore(LocalDateTime.now());
    }

    private boolean deleteNode(NodeDTO nodeDTO) {
        if (localFileConfigForManage.isExpireDelete()) {
            boolean deleted = artifactoryClient.deleteNode(
                artifactoryConfig.getArtifactoryJobProject(),
                localFileConfigForManage.getLocalUploadRepo(),
                nodeDTO.getFullPath()
            );
            log.info("Delete localUpload node {} in artifactory, result={}", nodeDTO.getFullPath(), deleted);
            return deleted;
        } else {
            log.info("Fake Delete localUpload node {} in artifactory, result={}", nodeDTO.getFullPath(), false);
            return false;
        }
    }

    /**
     * 清理蓝鲸制品库中的临时文件
     *
     * @param skipFile 需要跳过的文件
     */
    private void processArtifactoryFile(Set<String> skipFile) {
        NodeDTO localUploadNode = artifactoryClient.queryNodeDetail(
            artifactoryConfig.getArtifactoryJobProject(),
            localFileConfigForManage.getLocalUploadRepo(),
            "/"
        );
        DeleteNodeResult result = checkAndDeleteExpiredNodeAndChild(localUploadNode, skipFile);
        if (result.deletedNodeNum > 0) {
            log.info("processArtifactoryFile finished, deletedNodeNum={}", result.deletedNodeNum);
        }
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
            } else {
                if (localFileConfigForManage.isExpireDelete()) {
                    log.info("Delete file {}", aFile.getPath());
                    FileUtils.deleteQuietly(aFile);
                    try {
                        FileUtil.deleteEmptyDirectory(aFile.getParentFile());
                        FileUtil.deleteEmptyDirectory(aFile.getParentFile().getParentFile());
                    } catch (Exception e) {
                        log.warn("Error while delete empty parent of file {}", aFile.getPath());
                    }
                } else {
                    log.info("Fake Delete file {}", aFile.getPath());
                }
            }
        }
    }
}
