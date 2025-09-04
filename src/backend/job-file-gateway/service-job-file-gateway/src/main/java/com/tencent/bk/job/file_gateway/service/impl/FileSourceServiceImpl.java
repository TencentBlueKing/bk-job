/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.audit.JobAuditAttributeNames;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.auth.FileSourceAuthService;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBasicInfoDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTypeDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceMetaData;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceStaticParam;
import com.tencent.bk.job.file_gateway.model.req.common.FileWorkerConfig;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class FileSourceServiceImpl implements FileSourceService {

    private final FileSourceTypeDAO fileSourceTypeDAO;
    private final FileSourceDAO fileSourceDAO;
    private final FileWorkerDAO fileWorkerDAO;
    private final FileSourceAuthService fileSourceAuthService;

    @Autowired
    public FileSourceServiceImpl(FileSourceTypeDAO fileSourceTypeDAO,
                                 FileSourceDAO fileSourceDAO,
                                 FileWorkerDAO fileWorkerDAO,
                                 FileSourceAuthService fileSourceAuthService) {
        this.fileSourceTypeDAO = fileSourceTypeDAO;
        this.fileSourceDAO = fileSourceDAO;
        this.fileWorkerDAO = fileWorkerDAO;
        this.fileSourceAuthService = fileSourceAuthService;
    }

    @Override
    public List<FileSourceTypeDTO> listUniqueFileSourceType(String storageType) {
        List<FileSourceTypeDTO> fileSourceTypeDTOList = fileSourceTypeDAO.listEnabledTypeOrderByVersion(storageType);
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
        return fileSourceDAO.countAvailableLikeFileSource(appId, credentialId, alias);
    }

    @Override
    public Integer countWorkTableFileSource(Long appId, String credentialId, String alias) {
        return fileSourceDAO.countWorkTableFileSource(appId, credentialId, alias);
    }

    @Override
    public Integer countWorkTableFileSource(List<Long> appIdList, List<Integer> idList) {
        return fileSourceDAO.countWorkTableFileSource(appIdList, idList);
    }

    @Override
    public List<FileSourceDTO> listAvailableFileSource(Long appId, String credentialId, String alias, Integer start,
                                                       Integer pageSize) {
        return fileSourceDAO.listAvailableFileSource(appId, credentialId, alias, start, pageSize);
    }

    @Override
    public List<FileSourceDTO> listWorkTableFileSource(Long appId, String credentialId, String alias, Integer start,
                                                       Integer pageSize) {
        return fileSourceDAO.listWorkTableFileSource(appId, credentialId, alias, start, pageSize);
    }

    @Override
    public List<FileSourceDTO> listWorkTableFileSource(List<Long> appIdList, List<Integer> idList, Integer start,
                                                       Integer pageSize) {
        return fileSourceDAO.listWorkTableFileSource(appIdList, idList, start, pageSize);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.CREATE_FILE_SOURCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.FILE_SOURCE,
            instanceIds = "#$?.id",
            instanceNames = "#fileSource?.alias"
        ),
        content = EventContentConstants.CREATE_FILE_SOURCE
    )
    @JobTransactional(
        transactionManager = "jobFileGatewayTransactionManager",
        rollbackFor = {Exception.class, Error.class}
    )
    public FileSourceDTO saveFileSource(String username, Long appId, FileSourceDTO fileSource) {
        authCreate(username, appId);

        if (existsCode(appId, fileSource.getCode())) {
            throw new FailedPreconditionException(
                ErrorCode.FILE_SOURCE_CODE_ALREADY_EXISTS,
                new String[]{fileSource.getCode()}
            );
        }

        if (fileSourceDAO.checkFileSourceExists(fileSource.getAppId(), fileSource.getAlias())) {
            throw new AlreadyExistsException(ErrorCode.FILE_SOURCE_ALIAS_ALREADY_EXISTS,
                new String[]{fileSource.getAlias()});
        }

        Integer id = fileSourceDAO.insertFileSource(fileSource);
        fileSource.setId(id);

        boolean registerResult = fileSourceAuthService.registerFileSource(
            username, fileSource.getId(), fileSource.getAlias());
        if (!registerResult) {
            log.warn("Fail to register file_source to iam:({},{})", fileSource.getId(), fileSource.getAlias());
        }
        return getFileSourceById(id);
    }

    private void authView(String username, long appId, int fileSourceId) {
        fileSourceAuthService.authViewFileSource(username, new AppResourceScope(appId), fileSourceId, null)
            .denyIfNoPermission();
    }

    private void authCreate(String username, long appId) {
        fileSourceAuthService.authCreateFileSource(username, new AppResourceScope(appId)).denyIfNoPermission();
    }

    private void authManage(String username, long appId, int fileSourceId) {
        fileSourceAuthService.authManageFileSource(username, new AppResourceScope(appId),
            fileSourceId, null).denyIfNoPermission();
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_FILE_SOURCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.FILE_SOURCE,
            instanceIds = "#fileSource?.id",
            instanceNames = "$?.alias"
        ),
        content = EventContentConstants.EDIT_FILE_SOURCE
    )
    public FileSourceDTO updateFileSourceById(String username, Long appId, FileSourceDTO fileSource) {
        authManage(username, appId, fileSource.getId());

        if (existsCodeExceptId(appId, fileSource.getCode(), fileSource.getId())) {
            throw new FailedPreconditionException(
                ErrorCode.FILE_SOURCE_CODE_ALREADY_EXISTS,
                new String[]{fileSource.getCode()}
            );
        }

        FileSourceDTO originFileSource = getFileSourceById(fileSource.getId());
        if (originFileSource == null) {
            throw new NotFoundException(ErrorCode.FILE_SOURCE_NOT_EXIST);
        }

        fileSourceDAO.updateFileSource(fileSource);

        FileSourceDTO updateFileSource = getFileSourceById(fileSource.getId());

        // 审计
        ActionAuditContext.current()
            .setOriginInstance(FileSourceDTO.toEsbFileSourceV3DTO(fileSource))
            .setInstance(FileSourceDTO.toEsbFileSourceV3DTO(updateFileSource));

        return updateFileSource;
    }

    @Override
    public int updateFileSourceStatus(Integer fileSourceId, Integer status) {
        return fileSourceDAO.updateFileSourceStatus(fileSourceId, status);
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
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_FILE_SOURCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.FILE_SOURCE,
            instanceIds = "#id"
        ),
        content = EventContentConstants.DELETE_FILE_SOURCE
    )
    public Integer deleteFileSourceById(String username, Long appId, Integer id) {
        authManage(username, appId, id);

        FileSourceDTO fileSource = getFileSourceById(id);
        if (fileSource == null) {
            throw new NotFoundException(ErrorCode.FILE_SOURCE_NOT_EXIST);
        }
        ActionAuditContext.current().setInstanceName(fileSource.getAlias());

        return fileSourceDAO.deleteFileSourceById(id);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_FILE_SOURCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.FILE_SOURCE,
            instanceIds = "#id"
        ),
        content = EventContentConstants.SWITCH_FILE_SOURCE_STATUS
    )
    public Boolean enableFileSourceById(String username, Long appId, Integer id, Boolean enableFlag) {
        authManage(username, appId, id);
        FileSourceDTO fileSource = getFileSourceById(id);
        if (fileSource == null) {
            throw new NotFoundException(ErrorCode.FILE_SOURCE_NOT_EXIST);
        }

        // 审计
        ActionAuditContext.current()
            .setInstanceName(fileSource.getAlias())
            .addAttribute(JobAuditAttributeNames.OPERATION, enableFlag ? "Switch on" : "Switch off");

        return fileSourceDAO.enableFileSourceById(username, appId, id, enableFlag) == 1;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.VIEW_FILE_SOURCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.FILE_SOURCE,
            instanceIds = "#id",
            instanceNames = "#$?.alias"
        ),
        content = EventContentConstants.VIEW_FILE_SOURCE
    )
    public FileSourceDTO getFileSourceById(String username, Long appId, Integer id) {
        authView(username, appId, id);
        return getFileSourceById(appId, id);
    }

    @Override
    public FileSourceDTO getFileSourceById(Long appId, Integer id) {
        return fileSourceDAO.getFileSourceById(id);
    }

    @Override
    public FileSourceDTO getFileSourceById(Integer id) {
        return fileSourceDAO.getFileSourceById(id);
    }

    @Override
    public List<FileSourceBasicInfoDTO> listFileSourceByIds(Collection<Integer> ids) {
        return fileSourceDAO.listFileSourceByIds(ids);
    }

    @Override
    public FileSourceDTO getFileSourceByCode(String code) {
        return fileSourceDAO.getFileSourceByCode(code);
    }

    @Override
    public FileSourceDTO getFileSourceByCode(Long appId, String code) {
        return fileSourceDAO.getFileSourceByCode(appId, code);
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
        FileWorkerDTO fileWorkerDTO = fileWorkerDAO.getFileWorkerById(workerId);
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
        int count = fileSourceDAO.countFileSource(appId, null, alias);
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
    public boolean existsCode(Long appId, String code) {
        return fileSourceDAO.existsCode(appId, code);
    }

    @Override
    public boolean existsCodeExceptId(Long appId, String code, Integer exceptId) {
        return fileSourceDAO.existsCodeExceptId(appId, code, exceptId);
    }

    @Override
    public boolean existsFileSource(Long appId, Integer id) {
        return fileSourceDAO.existsFileSource(appId, id);
    }

    @Override
    public boolean existsFileSourceUsingCredential(Long appId, String credentialId) {
        return fileSourceDAO.existsFileSourceUsingCredential(appId, credentialId);
    }

    @Override
    public Integer getFileSourceIdByCode(Long appId, String code) {
        return fileSourceDAO.getFileSourceIdByCode(appId, code);
    }
}
