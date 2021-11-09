package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.config.ArtifactoryConfig;
import com.tencent.bk.job.execute.config.LocalFileConfigForExecute;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ArtifactoryLocalFileService {

    private final ArtifactoryConfig artifactoryConfig;
    private final LocalFileConfigForExecute localFileConfigForExecute;
    private final ArtifactoryClient artifactoryClient;

    @Autowired
    public ArtifactoryLocalFileService(
        ArtifactoryConfig artifactoryConfig, LocalFileConfigForExecute localFileConfigForExecute,
        ArtifactoryClient artifactoryClient
    ) {
        this.artifactoryConfig = artifactoryConfig;
        this.localFileConfigForExecute = localFileConfigForExecute;
        this.artifactoryClient = artifactoryClient;
    }

    public FileDetailDTO getFileDetailFromArtifactory(String filePath) {
        String fullPath = PathUtil.joinFilePath(
            artifactoryConfig.getArtifactoryJobProject()
                + "/" + localFileConfigForExecute.getLocalUploadRepo(),
            filePath
        );
        NodeDTO nodeDTO = artifactoryClient.getFileNode(fullPath);
        log.debug("nodeDTO={}", nodeDTO);
        if (nodeDTO == null) {
            throw new ServiceException(
                ErrorCode.LOCAL_FILE_NOT_EXIST_IN_BACKEND,
                new String[]{filePath}
            );
        }
        FileDetailDTO fileDetailDTO = new FileDetailDTO(filePath);
        fileDetailDTO.setFileName(nodeDTO.getName());
        fileDetailDTO.setFileHash(nodeDTO.getMd5());
        fileDetailDTO.setFileSize(nodeDTO.getSize());
        return fileDetailDTO;
    }

}
