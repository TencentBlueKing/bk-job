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
import com.tencent.bk.job.file_gateway.consts.WorkerSelectScopeEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTypeDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceMetaData;
import com.tencent.bk.job.file_gateway.model.req.common.FileWorkerConfig;
import com.tencent.bk.job.file_gateway.service.FileWorkerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileWorkerServiceImpl implements FileWorkerService {

    private DSLContext dslContext;
    private FileWorkerDAO fileWorkerDAO;
    private FileSourceTypeDAO fileSourceTypeDAO;

    @Autowired
    public FileWorkerServiceImpl(DSLContext dslContext, FileWorkerDAO fileWorkerDAO,
                                 FileSourceTypeDAO fileSourceTypeDAO) {
        this.dslContext = dslContext;
        this.fileWorkerDAO = fileWorkerDAO;
        this.fileSourceTypeDAO = fileSourceTypeDAO;
    }

    @Override
    public Long heartBeat(FileWorkerDTO fileWorkerDTO) {
        Long id = fileWorkerDTO.getId();
        String configStr = fileWorkerDTO.getConfigStr();
        FileWorkerConfig fileWorkerConfig = null;
        if (StringUtils.isNotBlank(configStr)) {
            fileWorkerConfig = JsonUtils.fromJson(configStr, new TypeReference<FileWorkerConfig>() {
            });
            if (fileWorkerConfig == null) {
                log.warn("cannot parse fileWorkerConfig, configStr={}", configStr);
            }
        }
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
            dslContext.transaction(configuration -> {
                fileWorkerDAO.updateFileWorker(dslContext, fileWorkerDTO);
                for (FileSourceTypeDTO fileSourceTypeDTO : fileSourceTypeDTOList) {
                    fileSourceTypeDAO.upsertByWorker(dslContext, fileSourceTypeDTO);
                }
            });
        } else {
            fileWorkerDAO.updateFileWorker(dslContext, fileWorkerDTO);
        }
        return id;
    }

    @Override
    public int offLine(Long workerId) {
        FileWorkerDTO fileWorkerDTO = fileWorkerDAO.getFileWorkerById(dslContext, workerId);
        fileWorkerDTO.setOnlineStatus((byte) 0);
        return fileWorkerDAO.updateFileWorker(dslContext, fileWorkerDTO);
    }

    private Integer getLatency(FileWorkerDTO fileWorkerDTO) {
        //TODO:计算延迟
        return 200;
    }

    @Override
    public List<FileWorkerDTO> listFileWorker(String username, Long appId, WorkerSelectScopeEnum workerSelectScope) {
        List<FileWorkerDTO> fileWorkerDTOList = new ArrayList<>();
        if (workerSelectScope == WorkerSelectScopeEnum.PUBLIC) {
            fileWorkerDTOList = fileWorkerDAO.listPublicFileWorkers(dslContext);
        } else if (workerSelectScope == WorkerSelectScopeEnum.APP) {
            fileWorkerDTOList = fileWorkerDAO.listFileWorkers(dslContext, appId);
        } else {
            List<FileWorkerDTO> publicFileWorkerDTOList = fileWorkerDAO.listPublicFileWorkers(dslContext);
            List<FileWorkerDTO> appFileWorkerDTOList = fileWorkerDAO.listFileWorkers(dslContext, appId);
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
        fileWorkerDTOList.forEach(fileWorkerDTO -> {
            fileWorkerDTO.setLatency(getLatency(fileWorkerDTO));
        });
        return fileWorkerDTOList;
    }
}
