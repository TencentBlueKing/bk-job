package com.tencent.bk.job.execute.engine.prepare;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.util.FileUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.config.ArtifactoryConfigForExecute;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class LocalFilePrepareService {

    private final StorageSystemConfig storageSystemConfig;
    private final ArtifactoryConfigForExecute artifactoryConfig;
    private final ArtifactoryClient artifactoryClient;

    @Autowired
    public LocalFilePrepareService(
        StorageSystemConfig storageSystemConfig,
        ArtifactoryConfigForExecute artifactoryConfig,
        ArtifactoryClient artifactoryClient
    ) {
        this.storageSystemConfig = storageSystemConfig;
        this.artifactoryConfig = artifactoryConfig;
        this.artifactoryClient = artifactoryClient;
    }

    public void prepareLocalFiles(List<FileSourceDTO> fileSourceList) {
        if (!artifactoryConfig.isEnable()) {
            log.info("artifactory is not enable, not need to prepare local file");
            return;
        }
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            if (fileSourceDTO == null) {
                log.warn("fileSourceDTO is null");
                continue;
            }
            if (fileSourceDTO.isLocalUpload() || fileSourceDTO.getFileType() == TaskFileTypeEnum.LOCAL.getType()) {
                List<FileDetailDTO> files = fileSourceDTO.getFiles();
                for (FileDetailDTO file : files) {
                    String filePath = file.getFilePath();
                    // 制品库的完整路径
                    String fullFilePath = PathUtil.joinFilePath(artifactoryConfig.getJobLocalUploadRootPath(), filePath);
                    NodeDTO nodeDTO = artifactoryClient.getFileNode(fullFilePath);
                    InputStream ins = artifactoryClient.getFileInputStream(fullFilePath);
                    // 本地存储路径
                    String localPath = PathUtil.joinFilePath(storageSystemConfig.getJobStorageRootPath(), "localupload");
                    localPath = PathUtil.joinFilePath(localPath, filePath);
                    // 保存到本地临时目录
                    AtomicInteger speed = new AtomicInteger(0);
                    AtomicInteger process = new AtomicInteger(0);
                    try {
                        log.debug("Download {} to {}", fullFilePath, localPath);
                        FileUtil.writeInsToFile(ins, localPath, nodeDTO.getSize(), speed, process);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
