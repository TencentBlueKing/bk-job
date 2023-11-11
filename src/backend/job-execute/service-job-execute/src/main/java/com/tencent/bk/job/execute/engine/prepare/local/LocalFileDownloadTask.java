package com.tencent.bk.job.execute.engine.prepare.local;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.util.FileUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.constants.Consts;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.InputStream;
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

    public LocalFileDownloadTask(StepInstanceDTO stepInstance,
                                 ArtifactoryClient artifactoryClient,
                                 String artifactoryProject,
                                 String artifactoryRepo,
                                 String jobStorageRootPath,
                                 FileDetailDTO file) {
        this.stepInstance = stepInstance;
        this.artifactoryClient = artifactoryClient;
        this.artifactoryProject = artifactoryProject;
        this.artifactoryRepo = artifactoryRepo;
        this.jobStorageRootPath = jobStorageRootPath;
        this.file = file;
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
            return false;
        }
    }

    private Boolean doCall() {
        String filePath = file.getFilePath();
        // 本地存储路径
        String localPath = PathUtil.joinFilePath(jobStorageRootPath, Consts.LOCAL_FILE_DIR_NAME);
        localPath = PathUtil.joinFilePath(localPath, filePath);
        File localFile = new File(localPath);
        // 如果本地文件还未下载就已存在，说明是分发配置文件，直接完成准备阶段
        if (localFile.exists()) {
            log.debug(
                "[{}]:local file {} already exists, lastModifyTime:{}",
                stepInstance.getUniqueKey(),
                localPath,
                TimeUtil.formatTime(localFile.lastModified(), "yyyy-MM-dd HH:mm:ss.SSS")
            );
            return true;
        }
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
            return false;
        }
        Pair<InputStream, HttpRequestBase> pair = artifactoryClient.getFileInputStream(
            artifactoryProject,
            artifactoryRepo,
            filePath
        );
        InputStream ins = pair.getLeft();
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
                localPath
            );
        }
        return false;
    }
}
