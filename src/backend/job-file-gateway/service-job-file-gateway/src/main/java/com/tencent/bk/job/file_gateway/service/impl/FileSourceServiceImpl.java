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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTypeDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceMetaData;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceStaticParam;
import com.tencent.bk.job.file_gateway.model.req.common.FileWorkerConfig;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FileSourceServiceImpl implements FileSourceService {

    private DSLContext dslContext;
    private FileSourceTypeDAO fileSourceTypeDAO;
    private FileSourceDAO fileSourceDAO;
    private FileWorkerDAO fileWorkerDAO;

    @Autowired
    public FileSourceServiceImpl(DSLContext dslContext, FileSourceTypeDAO fileSourceTypeDAO,
                                 FileSourceDAO fileSourceDAO, FileWorkerDAO fileWorkerDAO) {
        this.dslContext = dslContext;
        this.fileSourceTypeDAO = fileSourceTypeDAO;
        this.fileSourceDAO = fileSourceDAO;
        this.fileWorkerDAO = fileWorkerDAO;
    }

    @Override
    public List<FileSourceTypeDTO> listUniqueFileSourceType(String storageType) {
        List<FileSourceTypeDTO> fileSourceTypeDTOList = fileSourceTypeDAO.listOrderByVersion(dslContext
            , storageType);
        Set<String> codeSet = new HashSet<>();
        List<FileSourceTypeDTO> resultList = new ArrayList<>();
        // 多个Worker提供的同一种文件源能力只取版本最高的任意一个作为标准
        for (FileSourceTypeDTO fileSourceTypeDTO : fileSourceTypeDTOList) {
            if (!codeSet.contains(fileSourceTypeDTO.getCode())) {
                resultList.add(fileSourceTypeDTO);
                codeSet.add(fileSourceTypeDTO.getCode());
            }
        }
        return resultList;
    }

    @Override
    public Integer countAvailableFileSource(Long appId, String credentialId, String alias) {
        return fileSourceDAO.countAvailableLikeFileSource(dslContext, appId, credentialId, alias);
    }

    @Override
    public Integer countWorkTableFileSource(Long appId, String credentialId, String alias) {
        return fileSourceDAO.countWorkTableFileSource(dslContext, appId, credentialId, alias);
    }

    @Override
    public Integer countWorkTableFileSource(List<Long> appIdList, List<Integer> idList) {
        return fileSourceDAO.countWorkTableFileSource(dslContext, appIdList, idList);
    }

    @Override
    public List<FileSourceDTO> listAvailableFileSource(Long appId, String credentialId, String alias, Integer start,
                                                       Integer pageSize) {
        return fileSourceDAO.listAvailableFileSource(dslContext, appId, credentialId, alias, start, pageSize);
    }

    @Override
    public List<FileSourceDTO> listWorkTableFileSource(Long appId, String credentialId, String alias, Integer start,
                                                       Integer pageSize) {
        return fileSourceDAO.listWorkTableFileSource(dslContext, appId, credentialId, alias, start, pageSize);
    }

    @Override
    public List<FileSourceDTO> listWorkTableFileSource(List<Long> appIdList, List<Integer> idList, Integer start,
                                                       Integer pageSize) {
        return fileSourceDAO.listWorkTableFileSource(dslContext, appIdList, idList, start, pageSize);
    }

    @Override
    public Integer saveFileSource(Long appId, FileSourceDTO fileSourceDTO) {
        if (fileSourceDAO.checkFileSourceExists(dslContext, fileSourceDTO.getAppId(), fileSourceDTO.getAlias())) {
            throw new AlreadyExistsException(ErrorCode.FILE_SOURCE_ALIAS_ALREADY_EXISTS,
                new String[]{fileSourceDTO.getAlias()});
        }
        return fileSourceDAO.insertFileSource(dslContext, fileSourceDTO);
    }

    @Override
    public Integer updateFileSourceById(Long appId, FileSourceDTO fileSourceDTO) {
        return fileSourceDAO.updateFileSource(dslContext, fileSourceDTO);
    }

    @Override
    public int updateFileSourceStatus(Integer fileSourceId, Integer status) {
        return fileSourceDAO.updateFileSourceStatus(dslContext, fileSourceId, status);
    }

    @Override
    public FileSourceTypeDTO getFileSourceTypeById(Integer id) {
        return fileSourceTypeDAO.getById(id);
    }

    @Override
    public FileSourceTypeDTO getFileSourceTypeByCode(String code) {
        return fileSourceTypeDAO.getByCode(code);
    }

    @Override
    public Integer deleteFileSourceById(Long appId, Integer id) {
        return fileSourceDAO.deleteFileSourceById(dslContext, id);
    }

    @Override
    public Boolean enableFileSourceById(String username, Long appId, Integer id, Boolean enableFlag) {
        return fileSourceDAO.enableFileSourceById(dslContext, username, appId, id, enableFlag) == 1;
    }

    @Override
    public FileSourceDTO getFileSourceById(Long appId, Integer id) {
        return fileSourceDAO.getFileSourceById(dslContext, id);
    }

    @Override
    public FileSourceDTO getFileSourceById(Integer id) {
        return fileSourceDAO.getFileSourceById(dslContext, id);
    }

    @Override
    public FileSourceDTO getFileSourceByCode(String code) {
        return fileSourceDAO.getFileSourceByCode(dslContext, code);
    }

    private Long chooseAvailableWorker(String fileSourceTypeCode) {
        List<FileSourceTypeDTO> fileSourceTypeDTOList =
            fileSourceTypeDAO.listByCodeOrderByVersion(fileSourceTypeCode);
        // 多Worker同文件源标准选择策略：取版本最新的一个
        return fileSourceTypeDTOList.get(0).getWorkerId();
    }

    @Override
    public List<FileSourceStaticParam> getFileSourceParams(Long appId, String fileSourceTypeCode) {
        // 根据文件源类型选中一个Worker
        Long workerId = chooseAvailableWorker(fileSourceTypeCode);
        // 拿到Worker的配置，解析其定义的文件源Params
        FileWorkerDTO fileWorkerDTO = fileWorkerDAO.getFileWorkerById(dslContext, workerId);
        FileWorkerConfig fileWorkerConfig = JsonUtils.fromJson(fileWorkerDTO.getConfigStr(),
            new TypeReference<FileWorkerConfig>() {
            });
        List<FileSourceMetaData> fileSourceMetaDataList = fileWorkerConfig.getFileSourceMetaDataList();
        for (FileSourceMetaData fileSourceMetaData : fileSourceMetaDataList) {
            if (StringUtils.isNotBlank(fileSourceTypeCode)
                && fileSourceTypeCode.equals(fileSourceMetaData.getFileSourceTypeCode())) {
                return fileSourceMetaData.getStaticParamList();
            }
        }
        return null;
    }

    @Override
    public Boolean checkFileSourceAlias(Long appId, String alias, Integer fileSourceId) {
        int count = fileSourceDAO.countFileSource(dslContext, appId, null, alias);
        if (count == 0) {
            return true;
        } else {
            if (fileSourceId != null && fileSourceId > 0) {
                // 修改时Alias不变的情况
                FileSourceDTO fileSourceDTO = getFileSourceById(fileSourceId);
                return fileSourceDTO != null && fileSourceDTO.getAlias().equals(alias);
            }
        }
        return false;
    }

    @Override
    public boolean existsCode(String code) {
        return fileSourceDAO.existsCode(code);
    }

    @Override
    public boolean existsFileSource(Long appId, Integer id) {
        return fileSourceDAO.existsFileSource(appId, id);
    }

    @Override
    public Integer getFileSourceIdByCode(Long appId, String code) {
        return fileSourceDAO.getFileSourceIdByCode(appId, code);
    }
}
