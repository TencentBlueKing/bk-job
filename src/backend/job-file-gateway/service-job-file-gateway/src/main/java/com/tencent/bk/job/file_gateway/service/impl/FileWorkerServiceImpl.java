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

package com.tencent.bk.job.file_gateway.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.config.FileGatewayConfig;
import com.tencent.bk.job.file_gateway.consts.WorkerSelectScopeEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerTagDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTypeDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceMetaData;
import com.tencent.bk.job.file_gateway.model.req.common.FileWorkerConfig;
import com.tencent.bk.job.file_gateway.service.FileWorkerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileWorkerServiceImpl implements FileWorkerService {

    private final DSLContext dslContext;
    private final FileWorkerDAO fileWorkerDAO;
    private final FileSourceTypeDAO fileSourceTypeDAO;
    private final FileWorkerTagDAO fileWorkerTagDAO;
    private final FileGatewayConfig fileGatewayConfig;

    @Autowired
    public FileWorkerServiceImpl(DSLContext dslContext,
                                 FileWorkerDAO fileWorkerDAO,
                                 FileSourceTypeDAO fileSourceTypeDAO,
                                 FileWorkerTagDAO fileWorkerTagDAO,
                                 FileGatewayConfig fileGatewayConfig) {
        this.dslContext = dslContext;
        this.fileWorkerDAO = fileWorkerDAO;
        this.fileSourceTypeDAO = fileSourceTypeDAO;
        this.fileWorkerTagDAO = fileWorkerTagDAO;
        this.fileGatewayConfig = fileGatewayConfig;
    }

    @Override
    public Long heartBeat(FileWorkerDTO fileWorkerDTO) {
        Long id;
        String configStr = fileWorkerDTO.getConfigStr();
        FileWorkerConfig fileWorkerConfig = null;
        if (StringUtils.isNotBlank(configStr)) {
            fileWorkerConfig = JsonUtils.fromJson(configStr, new TypeReference<FileWorkerConfig>() {
            });
            if (fileWorkerConfig == null) {
                log.warn("cannot parse fileWorkerConfig, configStr={}", configStr);
            }
        }
        if (!fileWorkerDAO.existsFileWorker(
            fileWorkerDTO.getAccessHost(),
            fileWorkerDTO.getAccessPort())
        ) {
            id = saveFileWorker(fileWorkerDTO);
        } else {
            id = updateFileWorker(fileWorkerDTO);
        }
        log.debug("file worker id={}", id);
        if (fileWorkerConfig != null) {
            List<FileSourceMetaData> fileSourceMetaDataList = fileWorkerConfig.getFileSourceMetaDataList();
            List<FileSourceTypeDTO> fileSourceTypeDTOList = new ArrayList<>();
            for (FileSourceMetaData fileSourceMetaData : fileSourceMetaDataList) {
                FileSourceTypeDTO fileSourceTypeDTO = new FileSourceTypeDTO();
                fileSourceTypeDTO.setWorkerId(id);
                fileSourceTypeDTO.setCode(fileSourceMetaData.getFileSourceTypeCode());
                fileSourceTypeDTO.setName(fileSourceMetaData.getName());
                fileSourceTypeDTO.setIcon(fileSourceMetaData.getIconBase64());
                fileSourceTypeDTO.setStorageType(fileSourceMetaData.getStorageTypeCode());
                fileSourceTypeDTO.setLastModifier("Worker_" + id);
                fileSourceTypeDTOList.add(fileSourceTypeDTO);
            }
            for (FileSourceTypeDTO fileSourceTypeDTO : fileSourceTypeDTOList) {
                fileSourceTypeDAO.upsertByWorker(dslContext, fileSourceTypeDTO);
            }
        }
        return id;
    }

    private Long saveFileWorker(FileWorkerDTO fileWorkerDTO) {
        return fileWorkerDAO.insertFileWorker(fileWorkerDTO);
    }

    private Long updateFileWorker(FileWorkerDTO fileWorkerDTO) {
        FileWorkerDTO oldFileWorkerDTO = fileWorkerDAO.getFileWorker(
            fileWorkerDTO.getAccessHost(), fileWorkerDTO.getAccessPort()
        );
        Long workerId = oldFileWorkerDTO.getId();
        fileWorkerDTO.setId(workerId);
        fileWorkerDAO.updateFileWorker(fileWorkerDTO);
        return workerId;
    }

    @Override
    public int offLine(Long workerId) {
        FileWorkerDTO fileWorkerDTO = fileWorkerDAO.getFileWorkerById(workerId);
        fileWorkerDTO.setOnlineStatus((byte) 0);
        return fileWorkerDAO.updateFileWorker(fileWorkerDTO);
    }

    private Integer getLatency(FileWorkerDTO fileWorkerDTO) {
        //TODO:计算延迟
        return 200;
    }

    @Override
    public List<FileWorkerDTO> listFileWorker(String username, Long appId, WorkerSelectScopeEnum workerSelectScope) {
        WorkerIdsCondition workerIdsCondition = getIncludedAndExcludedWorkerIds();
        List<FileWorkerDTO> fileWorkerDTOList = new ArrayList<>();
        if (workerSelectScope == WorkerSelectScopeEnum.PUBLIC) {
            fileWorkerDTOList = fileWorkerDAO.listPublicFileWorkers(
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
        } else if (workerSelectScope == WorkerSelectScopeEnum.APP) {
            fileWorkerDTOList = fileWorkerDAO.listFileWorkers(
                appId,
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
        } else {
            List<FileWorkerDTO> publicFileWorkerDTOList = fileWorkerDAO.listPublicFileWorkers(
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
            List<FileWorkerDTO> appFileWorkerDTOList = fileWorkerDAO.listFileWorkers(
                appId,
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
            for (FileWorkerDTO fileWorkerDTO : publicFileWorkerDTOList) {
                if (!fileWorkerDTOList.contains(fileWorkerDTO)) {
                    fileWorkerDTOList.add(fileWorkerDTO);
                }
            }
            for (FileWorkerDTO fileWorkerDTO : appFileWorkerDTOList) {
                if (!fileWorkerDTOList.contains(fileWorkerDTO)) {
                    fileWorkerDTOList.add(fileWorkerDTO);
                }
            }
        }
        fileWorkerDTOList.forEach(fileWorkerDTO -> fileWorkerDTO.setLatency(getLatency(fileWorkerDTO)));
        return fileWorkerDTOList;
    }

    @Override
    public FileWorkerDTO getFileWorker(String accessHost, Integer accessPort) {
        return fileWorkerDAO.getFileWorker(accessHost, accessPort);
    }

    @Override
    public WorkerIdsCondition getIncludedAndExcludedWorkerIds() {
        List<Long> includedWorkerIds = null;
        List<Long> excludedWorkerIds = null;
        List<String> whiteTags = fileGatewayConfig.getWorkerTagWhiteList();
        List<String> blackTags = fileGatewayConfig.getWorkerTagBlackList();
        if (CollectionUtils.isNotEmpty(whiteTags)) {
            includedWorkerIds = fileWorkerTagDAO.listWorkerIdByTag(whiteTags);
        }
        if (CollectionUtils.isNotEmpty(blackTags)) {
            excludedWorkerIds = fileWorkerTagDAO.listWorkerIdByTag(blackTags);
        }
        return new WorkerIdsCondition(includedWorkerIds, excludedWorkerIds);
    }
}
