package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.artifactory.config.ArtifactoryConfig;
import com.tencent.bk.job.common.artifactory.constants.ArtifactoryInterfaceConsts;
import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.config.LocalFileConfigForExecute;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ArtifactoryLocalFileService {

    private final ArtifactoryConfig artifactoryConfig;
    private final LocalFileConfigForExecute localFileConfigForExecute;
    private final ArtifactoryClient artifactoryClient;

    @Autowired
    public ArtifactoryLocalFileService(
        ArtifactoryConfig artifactoryConfig,
        LocalFileConfigForExecute localFileConfigForExecute,
        @Qualifier("jobArtifactoryClient") ArtifactoryClient artifactoryClient
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
        NodeDTO nodeDTO = null;
        try {
            nodeDTO = artifactoryClient.getFileNode(fullPath);
        } catch (InternalException e) {
            if (e.getErrorCode() == ErrorCode.CAN_NOT_FIND_NODE_IN_ARTIFACTORY) {
                log.error("[TransferLocalFile] transfer fail, local file {} not in artifactory", filePath);
                throw new InternalException(
                    "local file not found in artifactory",
                    ErrorCode.LOCAL_FILE_NOT_EXIST_IN_BACKEND,
                    new String[]{filePath}
                );
            } else {
                throw e;
            }
        }
        log.debug("nodeDTpwO={}", nodeDTO);
        if (nodeDTO == null) {
            throw new InternalException(
                "local file not found in artifactory",
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
