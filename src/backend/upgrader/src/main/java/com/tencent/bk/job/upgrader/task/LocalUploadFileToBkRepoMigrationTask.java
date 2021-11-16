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

package com.tencent.bk.job.upgrader.task;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.RequireTaskParam;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.anotation.UpgradeTaskInputParam;
import com.tencent.bk.job.upgrader.task.param.JobManageServerAddress;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 本地文件向BKREPO迁移任务
 */
@Slf4j
@UpgradeTask(
        dataStartVersion = "3.0.0.0",
        targetVersion = "3.4.2.0",
        targetExecuteTime = ExecuteTimeEnum.MAKE_UP)
public class LocalUploadFileToBkRepoMigrationTask extends BaseUpgradeTask {

    private ArtifactoryClient artifactoryAdminClient;
    private ArtifactoryClient artifactoryJobClient;
    private String baseUrl;
    private String adminUsername;
    private String adminPassword;
    private String jobUsername;
    private String jobPassword;
    private String jobProject;
    private String localUploadRepo;
    private String backupRepo;
    private String logExportRepo;
    private String storageRootPath;

    private final String localUploadDirName = "localupload";
    private final String backupDirName = "backup";
    private final String logExportDirName = "filedata";

    public LocalUploadFileToBkRepoMigrationTask(Properties properties) {
        super(properties);
    }

    private ArtifactoryClient getAdminClient() {
        Properties properties = getProperties();
        if (artifactoryAdminClient == null) {
            artifactoryAdminClient = new ArtifactoryClient(
                    (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_BASE_URL),
                    (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_ADMIN_USERNAME),
                    (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_ADMIN_PASSWORD),
                    null
            );
        }
        return artifactoryAdminClient;
    }

    private ArtifactoryClient getJobClient() {
        Properties properties = getProperties();
        if (artifactoryJobClient == null) {
            artifactoryJobClient = new ArtifactoryClient(
                    (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_BASE_URL),
                    (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_JOB_USERNAME),
                    (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_JOB_PASSWORD),
                    null
            );
        }
        return artifactoryJobClient;
    }

    private void initConfig() {
        Properties properties = getProperties();
        log.debug("properties={}", properties);
        baseUrl = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_BASE_URL);
        adminUsername = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_ADMIN_USERNAME);
        adminPassword = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_ADMIN_PASSWORD);
        jobUsername = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_JOB_USERNAME);
        jobPassword = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_JOB_PASSWORD);
        jobProject = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ARTIFACTORY_JOB_PROJECT);
        localUploadRepo = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_LOCAL_FILE_ARTIFACTORY_REPO);
        backupRepo = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_BACKUP_ARTIFACTORY_REPO);
        logExportRepo = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_LOG_EXPORT_ARTIFACTORY_REPO);
        storageRootPath = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_JOB_STORAGE_ROOT_PATH);
    }

    private void initUserAndProject() {

        boolean projectUserCreated = ArtifactoryHelper.createJobUserAndProjectIfNotExists(
                baseUrl,
                adminUsername,
                adminPassword,
                jobUsername,
                jobPassword,
                jobProject
        );
        if (!projectUserCreated) {
            log.error(
                    "Fail to create project {} or user {}",
                    jobProject,
                    jobUsername
            );
        }
    }

    private void initLocaluploadRepo() {
        String REPO_LOCALUPLOAD_DESCRIPTION = "BlueKing bk-job official project localupload repo," +
                " which is used to save job data produced by users. " +
                "Do not delete me unless you know what you are doing";
        boolean repoCreated = ArtifactoryHelper.createRepoIfNotExist(
                baseUrl,
                adminUsername,
                adminPassword,
                jobProject,
                localUploadRepo,
                REPO_LOCALUPLOAD_DESCRIPTION
        );
        if (repoCreated) {
            log.info(
                    "repo {} created",
                    localUploadRepo
            );
        }
    }

    private void initBackupRepo() {
        String REPO_BACKUP_DESCRIPTION = "BlueKing bk-job official project backup repo," +
                " which is used to save job export data produced by program. " +
                "Do not delete me unless you know what you are doing";
        boolean repoCreated = ArtifactoryHelper.createRepoIfNotExist(
                baseUrl,
                adminUsername,
                adminPassword,
                jobProject,
                backupRepo,
                REPO_BACKUP_DESCRIPTION
        );
        if (repoCreated) {
            log.info(
                    "repo {} created",
                    backupRepo
            );
        }
    }

    private void initLogExportRepo() {
        String REPO_LOG_EXPORT_DESCRIPTION = "BlueKing bk-job official project logExport repo," +
                " which is used to save job execute log export data produced by program. " +
                "Do not delete me unless you know what you are doing";
        boolean repoCreated = ArtifactoryHelper.createRepoIfNotExist(
                baseUrl,
                adminUsername,
                adminPassword,
                jobProject,
                logExportRepo,
                REPO_LOG_EXPORT_DESCRIPTION
        );
        if (repoCreated) {
            log.info(
                    "repo {} created",
                    logExportRepo
            );
        }
    }

    @Override
    public void init() {
        // 0.获取配置信息
        initConfig();
        // 1.创建项目与用户
        initUserAndProject();
        // 2.创建本地文件仓库
        initLocaluploadRepo();
        // 3.创建导入导出仓库
        initBackupRepo();
        // 4.创建执行日志导出仓库
        initLogExportRepo();
    }

    private List<File> scanAllFilesOnPath(String path) {
        File rootFile = new File(path);
        if (!rootFile.exists()) {
            return Collections.emptyList();
        }
        if (!rootFile.isDirectory()) {
            return Collections.singletonList(rootFile);
        }
        File[] files = rootFile.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<File> fileList = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                fileList.addAll(scanAllFilesOnPath(file.getAbsolutePath()));
            } else {
                fileList.add(file);
            }
        }
        fileList.sort((f1, f2) -> (int) (f2.lastModified() - f1.lastModified()));
        return fileList;
    }

    private boolean existsFileInRepo(String repo, String filePath, String fileMd5) {
        ArtifactoryClient jobClient = getJobClient();
        try {
            NodeDTO nodeDTO = jobClient.queryNodeDetail(jobProject, repo, filePath);
            return nodeDTO != null
                    && fileMd5.equals(nodeDTO.getMd5());
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                    "Fail to compare targetFile {} with node in repo {}",
                    filePath,
                    repo
            );
            log.warn(msg.getMessage());
        }
        return false;
    }

    private void uploadFilesToRepo(String path, String repo) {
        ArtifactoryClient jobClient = getJobClient();
        List<File> fileList = scanAllFilesOnPath(path);
        log.info("{} files in path {}", fileList.size(), path);
        List<String> failedFilePathList = new ArrayList<>();
        for (int i = 0; i < fileList.size(); i++) {
            File file = fileList.get(i);
            String filePath = file.getAbsolutePath();
            String relativePath = StringUtil.removePrefix(filePath, path);
            relativePath = StringUtil.removePrefix(relativePath, "/");
            String md5;
            log.info(
                    "{} {}/{}: process {} ",
                    repo,
                    i + 1,
                    fileList.size(),
                    filePath
            );
            try {
                md5 = DigestUtils.md5Hex(new FileInputStream(file));
            } catch (IOException e) {
                FormattingTuple msg = MessageFormatter.format(
                        "Fail to calc md5 of file {}",
                        filePath
                );
                failedFilePathList.add(filePath);
                log.warn(msg.getMessage(), e);
                throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
            }
            if (!existsFileInRepo(repo, relativePath, md5)) {
                NodeDTO nodeDTO;
                int retryTimes = 0;
                boolean uploaded = false;
                try {
                    while (!uploaded && retryTimes < 3) {
                        nodeDTO = jobClient.uploadGenericFile(
                                jobProject,
                                repo,
                                relativePath,
                                file
                        );
                        if (nodeDTO != null && md5.equals(nodeDTO.getMd5())) {
                            log.info(
                                    "Success uploaded {} to repo {}, md5={}",
                                    relativePath, repo, nodeDTO.getMd5()
                            );
                            uploaded = true;
                        } else {
                            log.warn(
                                    "Fail to upload {} to repo {}, md5 not correct, retry after 5s",
                                    relativePath, repo
                            );
                            Thread.sleep(5000);
                            retryTimes += 1;
                        }
                    }
                    if (!uploaded) {
                        failedFilePathList.add(filePath);
                    }
                } catch (Exception e) {
                    FormattingTuple msg = MessageFormatter.format(
                            "Fail to upload {} to repo {}",
                            relativePath,
                            repo
                    );
                    log.warn(msg.getMessage(), e);
                }
            } else {
                log.info("File {} already in repo {}, skip", relativePath, repo);
            }
        }
        log.warn("Fail to upload {} files to repo {}", failedFilePathList.size(), repo);
        failedFilePathList.forEach(log::info);
    }

    private void uploadLocalFiles() {
        String localFileDirPath = PathUtil.joinFilePath(storageRootPath, localUploadDirName);
        uploadFilesToRepo(localFileDirPath, localUploadRepo);
    }

    private void uploadBackupFiles() {
        String localFileDirPath = PathUtil.joinFilePath(storageRootPath, backupDirName);
        uploadFilesToRepo(localFileDirPath, backupRepo);
    }

    private void uploadLogExportFiles() {
        String localFileDirPath = PathUtil.joinFilePath(storageRootPath, logExportDirName);
        uploadFilesToRepo(localFileDirPath, logExportRepo);
    }

    @Override
    public int execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        // 1.初始化用户与各仓库
        init();
        // 2.上传本地文件至制品库
        uploadLocalFiles();
        // 3.上传导入导出文件至制品库
        uploadBackupFiles();
        // 4.上传执行日志导出文件至制品库
        uploadLogExportFiles();
        return 0;
    }
}
