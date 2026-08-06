package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
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

    private final ArtifactoryHelper artifactoryHelper;
    private final LocalFileConfigForExecute localFileConfigForExecute;
    private final ArtifactoryClient artifactoryClient;
    private final LocalFileConfigForExecute localFileConfig;

    @Autowired
    public ArtifactoryLocalFileService(
        ArtifactoryHelper artifactoryHelper,
        LocalFileConfigForExecute localFileConfigForExecute,
        @Qualifier("jobArtifactoryClient") ArtifactoryClient artifactoryClient,
        LocalFileConfigForExecute localFileConfig
    ) {
        this.artifactoryHelper = artifactoryHelper;
        this.localFileConfigForExecute = localFileConfigForExecute;
        this.artifactoryClient = artifactoryClient;
        this.localFileConfig = localFileConfig;
    }

    public FileDetailDTO getFileDetailFromArtifactory(String filePath) {
        NodeDTO nodeDTO = getFileNodeAndHandleException(filePath);
        log.debug("nodeDTO={}", nodeDTO);
        if (nodeDTO == null) {
            throw new NotFoundException(
                "local file not found in artifactory",
                ErrorCode.LOCAL_FILE_NOT_EXIST_IN_BACKEND,
                new String[]{filePath, String.valueOf(localFileConfig.getExpireDays())}
            );
        }
        FileDetailDTO fileDetailDTO = new FileDetailDTO(filePath);
        fileDetailDTO.setFileName(nodeDTO.getName());
        fileDetailDTO.setFileHash(nodeDTO.getMd5());
        fileDetailDTO.setFileSize(nodeDTO.getSize());
        return fileDetailDTO;
    }

    private NodeDTO getFileNodeAndHandleException(String filePath) {
        String fullPath = PathUtil.joinFilePath(
            artifactoryHelper.getJobRealProject()
                + "/" + localFileConfigForExecute.getLocalUploadRepo(),
            filePath
        );
        NodeDTO nodeDTO;
        try {
            nodeDTO = artifactoryClient.getFileNode(fullPath);
        } catch (InternalException e) {
            if (e.getErrorCode() == ErrorCode.CAN_NOT_FIND_NODE_IN_ARTIFACTORY) {
                // 本地文件在制品库中不存在（通常是已过期被清理），属于可预期的业务场景，记录 INFO 即可
                log.info("[TransferLocalFile] transfer fail, local file {} not in artifactory", filePath);
                throw new NotFoundException(
                    "local file not found in artifactory",
                    ErrorCode.LOCAL_FILE_NOT_EXIST_IN_BACKEND,
                    new String[]{filePath, String.valueOf(localFileConfig.getExpireDays())}
                );
            } else {
                throw e;
            }
        }
        return nodeDTO;
    }

}
