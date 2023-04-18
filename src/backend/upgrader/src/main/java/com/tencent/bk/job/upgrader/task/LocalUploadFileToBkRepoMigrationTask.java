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
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 本地文件向BKREPO迁移任务
 */
@Slf4j
@UpgradeTask(
    dataStartVersion = "3.0.0.0",
    targetVersion = "3.4.2.1",
    targetExecuteTime = ExecuteTimeEnum.MAKE_UP)
@SuppressWarnings("unused")
public class LocalUploadFileToBkRepoMigrationTask extends BaseUpgradeTask {

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
    private boolean enableMigrateLocalUploadFile;
    private boolean enableMigrateBackupFile;
    private boolean enableMigrateLogExportFile;
    private ThreadPoolExecutor uploadExecutor;
    private final ConcurrentLinkedQueue<String> allFailedFilePathList = new ConcurrentLinkedQueue<>();

    public LocalUploadFileToBkRepoMigrationTask(Properties properties) {
        super(properties);
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
        enableMigrateLocalUploadFile = Boolean.parseBoolean(
            (String) properties.getOrDefault(
                ParamNameConsts.CONFIG_PROPERTY_ENABLE_MIGRATE_LOCAL_UPLOAD_FILE, "true"
            ));
        enableMigrateBackupFile = Boolean.parseBoolean(
            (String) properties.getOrDefault(
                ParamNameConsts.CONFIG_PROPERTY_ENABLE_MIGRATE_BACKUP_FILE, "false"
            ));
        enableMigrateLogExportFile = Boolean.parseBoolean(
            (String) properties.getOrDefault(
                ParamNameConsts.CONFIG_PROPERTY_ENABLE_MIGRATE_LOG_EXPORT_FILE, "false"
            ));
        int uploadConcurrency = Integer.parseInt(
            (String) properties.getOrDefault(
                ParamNameConsts.CONFIG_PROPERTY_MIGRATE_UPLOAD_CONCURRENCY, 20
            ));
        uploadExecutor = new ThreadPoolExecutor(
            uploadConcurrency, uploadConcurrency,
            1, TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(200),
            (r, executor) -> {
                // 使用当前线程上传
                log.info("task queue full, use current thread to upload");
                r.run();
            });
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
        return fileList;
    }

    private boolean existsFileInRepo(String repo, String filePath, String fileMd5) {
        ArtifactoryClient jobClient = getJobClient();
        int retryCount = 0;
        int maxRetryCount = 3;
        do {
            try {
                NodeDTO nodeDTO = jobClient.queryNodeDetail(jobProject, repo, filePath);
                return nodeDTO != null
                    && fileMd5.equals(nodeDTO.getMd5());
            } catch (Exception e) {
                FormattingTuple msg = MessageFormatter.arrayFormat(
                    "Fail to compare targetFile {} with node in repo {}, retry={}", new Object[]{
                        filePath,
                        repo,
                        retryCount
                    }
                );
                log.warn(msg.getMessage());
                retryCount += 1;
                ThreadUtils.sleep(5000);
            }
        } while (retryCount < maxRetryCount);
        return false;
    }

    class UploadTask extends Thread {

        private final ArtifactoryClient jobClient;
        private final File file;
        private final String repo;
        private final String path;
        private final int taskIndex;
        private final int taskSize;
        private final ConcurrentLinkedQueue<String> failedFilePathList;

        UploadTask(
            ArtifactoryClient jobClient,
            File file, String repo,
            String path, int taskIndex, int taskSize,
            ConcurrentLinkedQueue<String> failedFilePathList) {
            this.jobClient = jobClient;
            this.file = file;
            this.repo = repo;
            this.path = path;
            this.taskIndex = taskIndex;
            this.taskSize = taskSize;
            this.failedFilePathList = failedFilePathList;
        }

        @Override
        public void run() {
            String filePath = file.getAbsolutePath();
            String relativePath = StringUtil.removePrefix(filePath, path);
            relativePath = StringUtil.removePrefix(relativePath, "/");
            String md5;
            log.info(
                "{} {}/{}: process {} ",
                repo,
                taskIndex + 1,
                taskSize,
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
            if (existsFileInRepo(repo, relativePath, md5)) {
                log.info("File {} already in repo {}, skip", relativePath, repo);
                return;
            }
            uploadFileToRepoWithRetry(filePath, relativePath, md5);
        }

        private void uploadFileToRepoWithRetry(String filePath, String relativePath, String md5) {
            NodeDTO nodeDTO;
            int retryTimes = 0;
            boolean uploaded = false;
            while (!uploaded && retryTimes < 3) {
                try {
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
                            relativePath,
                            repo
                        );
                        retryTimes += 1;
                        ThreadUtils.sleep(5000);
                    }
                } catch (Exception e) {
                    FormattingTuple msg = MessageFormatter.arrayFormat(
                        "Fail to upload {} to repo {}, retry={}", new Object[]{
                            relativePath,
                            repo,
                            retryTimes
                        }
                    );
                    log.warn(msg.getMessage(), e);
                    retryTimes += 1;
                    ThreadUtils.sleep(5000);
                }
            }
            if (!uploaded) {
                failedFilePathList.add(filePath);
            }
        }
    }

    private void uploadFilesToRepo(String path, String repo) {
        ArtifactoryClient jobClient = getJobClient();
        List<File> fileList = scanAllFilesOnPath(path);
        log.info("{} files in path {}", fileList.size(), path);
        List<Future<?>> uploadTaskFutureList = new ArrayList<>();
        ConcurrentLinkedQueue<String> currentFailedFilePathList = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < fileList.size(); i++) {
            File file = fileList.get(i);
            UploadTask task = new UploadTask(
                jobClient,
                file, repo, path,
                i, fileList.size(),
                currentFailedFilePathList
            );
            Future<?> future = uploadExecutor.submit(task);
            uploadTaskFutureList.add(future);
        }
        uploadTaskFutureList.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException e) {
                log.warn("UploadTask interrupted", e);
            } catch (ExecutionException e) {
                log.warn("UploadTask execute error", e);
            }
        });
        log.info("all {} uploadTasks to repo {} done!", fileList.size(), repo);
        if (!currentFailedFilePathList.isEmpty()) {
            log.warn("Fail to upload {} files to repo {}", currentFailedFilePathList.size(), repo);
            currentFailedFilePathList.forEach(log::info);
            allFailedFilePathList.addAll(currentFailedFilePathList);
        }
    }

    private void uploadLocalFiles() {
        String localUploadDirName = "localupload";
        String localFileDirPath = PathUtil.joinFilePath(storageRootPath, localUploadDirName);
        uploadFilesToRepo(localFileDirPath, localUploadRepo);
    }

    private void uploadBackupFiles() {
        String backupDirName = "backup";
        String localFileDirPath = PathUtil.joinFilePath(storageRootPath, backupDirName);
        uploadFilesToRepo(localFileDirPath, backupRepo);
    }

    private void uploadLogExportFiles() {
        String logExportDirName = "filedata";
        String localFileDirPath = PathUtil.joinFilePath(storageRootPath, logExportDirName);
        uploadFilesToRepo(localFileDirPath, logExportRepo);
    }

    @Override
    public boolean execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        // 1.初始化用户与各仓库
        init();
        // 2.上传本地文件至制品库
        if (enableMigrateLocalUploadFile) {
            uploadLocalFiles();
        }
        // 3.上传导入导出文件至制品库
        if (enableMigrateBackupFile) {
            uploadBackupFiles();
        }
        // 4.上传执行日志导出文件至制品库
        if (enableMigrateLogExportFile) {
            uploadLogExportFiles();
        }
        uploadExecutor.shutdown();
        if (allFailedFilePathList.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
}
