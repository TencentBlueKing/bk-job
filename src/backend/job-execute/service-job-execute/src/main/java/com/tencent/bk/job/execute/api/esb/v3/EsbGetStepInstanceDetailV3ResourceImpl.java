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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbAccountV3BasicDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileDestinationV3DTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileSourceV3DTO;
import com.tencent.bk.job.common.esb.model.job.v3.resp.EsbApprovalStepV3DTO;
import com.tencent.bk.job.common.esb.model.job.v3.resp.EsbFileStepV3DTO;
import com.tencent.bk.job.common.esb.model.job.v3.resp.EsbScriptStepV3DTO;
import com.tencent.bk.job.common.esb.model.job.v3.resp.EsbStepV3DTO;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.util.FileTransferModeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbGetStepInstanceDetailV3ResourceImpl implements EsbGetStepInstanceDetailV3Resource {

    private final TaskInstanceService taskInstanceService;
    private final AppScopeMappingService appScopeMappingService;
    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private final StepInstanceService stepInstanceService;

    public EsbGetStepInstanceDetailV3ResourceImpl(TaskInstanceService taskInstanceService,
                                                  AppScopeMappingService appScopeMappingService,
                                                  TaskInstanceAccessProcessor taskInstanceAccessProcessor,
                                                  StepInstanceService stepInstanceService) {
        this.taskInstanceService = taskInstanceService;
        this.appScopeMappingService = appScopeMappingService;
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbStepV3DTO> getStepInstanceDetail(String username,
                                                       String appCode,
                                                       String scopeType,
                                                       String scopeId,
                                                       Long taskInstanceId,
                                                       Long stepInstanceId) {
        long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);

        StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(taskInstanceId, stepInstanceId);

        taskInstanceAccessProcessor.processBeforeAccess(username, appId, stepInstance.getTaskInstanceId());

        EsbStepV3DTO esbStepV3DTO = convertToEsbStepV3DTO(stepInstance);
        return EsbResp.buildSuccessResp(esbStepV3DTO);
    }

    private EsbStepV3DTO convertToEsbStepV3DTO(StepInstanceDTO stepInstance) {
        if (stepInstance == null) {
            return null;
        }
        EsbStepV3DTO esbStepV3DTO = new EsbStepV3DTO();
        esbStepV3DTO.setId(stepInstance.getId());
        esbStepV3DTO.setName(stepInstance.getName());
        esbStepV3DTO.setType(stepInstance.getStepType().getValue());
        switch (stepInstance.getStepType()) {
            case SCRIPT:
                EsbScriptStepV3DTO scriptStepInfo = new EsbScriptStepV3DTO();
                scriptStepInfo.setType(stepInstance.getScriptSource());
                scriptStepInfo.setScriptId(stepInstance.getScriptId());
                scriptStepInfo.setScriptVersionId(stepInstance.getScriptVersionId());
                scriptStepInfo.setContent(stepInstance.getScriptContent());
                scriptStepInfo.setLanguage(stepInstance.getScriptType().getValue());
                if (stepInstance.isSecureParam()) {
                    scriptStepInfo.setSecureParam(1);
                    scriptStepInfo.setScriptParam("******");
                } else {
                    scriptStepInfo.setSecureParam(0);
                    if (StringUtils.isNotEmpty(stepInstance.getResolvedScriptParam())) {
                        scriptStepInfo.setScriptParam(stepInstance.getResolvedScriptParam());
                    } else {
                        scriptStepInfo.setScriptParam(stepInstance.getScriptParam());
                    }
                }
                scriptStepInfo.setScriptTimeout(getTimeout(stepInstance));
                scriptStepInfo.setAccount(getAccount(stepInstance));
                scriptStepInfo.setServer(stepInstance.getTargetExecuteObjects().toEsbServerV3DTO());
                scriptStepInfo.setIgnoreError(stepInstance.isIgnoreError() ? 1 : 0);
                esbStepV3DTO.setScriptInfo(scriptStepInfo);
                break;
            case FILE:
                EsbFileStepV3DTO fileStepInfo = new EsbFileStepV3DTO();
                fileStepInfo.setFileSourceList(convertToFileSourceList(stepInstance.getFileSourceList()));
                fileStepInfo.setFileDestination(getFileDestination(stepInstance));
                fileStepInfo.setTimeout(getTimeout(stepInstance));
                Long uploadSpeedLimit = getFileUploadSpeedLimit(stepInstance);
                if (uploadSpeedLimit != null) {
                    fileStepInfo.setSourceSpeedLimit(uploadSpeedLimit >> 10);
                }
                Long downloadSpeedLimit = getFileDownloadSpeedLimit(stepInstance);
                if (downloadSpeedLimit != null) {
                    fileStepInfo.setDestinationSpeedLimit(downloadSpeedLimit >> 10);
                }
                Integer transferMode = FileTransferModeUtil.getTransferMode(
                    stepInstance.getFileDuplicateHandle(),
                    stepInstance.getNotExistPathHandler()
                ).getValue();
                fileStepInfo.setTransferMode(transferMode);
                fileStepInfo.setIgnoreError(stepInstance.isIgnoreError() ? 1 : 0);
                esbStepV3DTO.setFileInfo(fileStepInfo);
                break;
            case APPROVAL:
                EsbApprovalStepV3DTO esbApprovalStepV3DTO = new EsbApprovalStepV3DTO();
                esbApprovalStepV3DTO.setApprovalMessage(stepInstance.getConfirmMessage());
                esbStepV3DTO.setApprovalInfo(esbApprovalStepV3DTO);
                break;
        }
        return esbStepV3DTO;
    }

    private Long getTimeout(StepInstanceDTO stepInstance) {
        Integer timeout = stepInstance.getTimeout();
        return timeout == null ? null : timeout.longValue();
    }

    private EsbAccountV3BasicDTO getAccount(StepInstanceDTO stepInstance) {
        EsbAccountV3BasicDTO account = new EsbAccountV3BasicDTO();
        account.setId(stepInstance.getAccountId());
        account.setName(stepInstance.getAccount());
        return account;
    }

    private Long getFileUploadSpeedLimit(StepInstanceDTO stepInstance) {
        Integer fileUploadSpeedLimit = stepInstance.getFileUploadSpeedLimit();
        return fileUploadSpeedLimit == null ? null : fileUploadSpeedLimit.longValue();
    }

    private Long getFileDownloadSpeedLimit(StepInstanceDTO stepInstance) {
        Integer fileDownloadSpeedLimit = stepInstance.getFileDownloadSpeedLimit();
        return fileDownloadSpeedLimit == null ? null : fileDownloadSpeedLimit.longValue();
    }

    private List<EsbFileSourceV3DTO> convertToFileSourceList(List<FileSourceDTO> fileSourceList) {
        if (fileSourceList == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(fileSourceList)) {
            return Collections.emptyList();
        }
        return fileSourceList.stream().map(this::convertToEsbFileSourceV3DTO).collect(Collectors.toList());
    }

    private EsbFileSourceV3DTO convertToEsbFileSourceV3DTO(FileSourceDTO fileSourceDTO) {
        if (fileSourceDTO == null) {
            return null;
        }
        EsbFileSourceV3DTO esbFileSourceV3DTO = new EsbFileSourceV3DTO();
        esbFileSourceV3DTO.setFiles(fileSourceDTO.getFiles()
            .stream().map(FileDetailDTO::getFilePath)
            .collect(Collectors.toList())
        );
        EsbAccountV3BasicDTO account = new EsbAccountV3BasicDTO();
        account.setId(fileSourceDTO.getAccountId());
        account.setName(fileSourceDTO.getAccount());
        esbFileSourceV3DTO.setAccount(account);
        esbFileSourceV3DTO.setServer(fileSourceDTO.getServers().toEsbServerV3DTO());
        esbFileSourceV3DTO.setFileType(fileSourceDTO.getFileType());
        esbFileSourceV3DTO.setFileSourceId(fileSourceDTO.getFileSourceId());
        return esbFileSourceV3DTO;
    }

    private EsbFileDestinationV3DTO getFileDestination(StepInstanceDTO stepInstance) {
        if (stepInstance == null) {
            return null;
        }
        EsbFileDestinationV3DTO esbFileDestinationV3DTO = new EsbFileDestinationV3DTO();
        esbFileDestinationV3DTO.setPath(stepInstance.getFileTargetPath());
        esbFileDestinationV3DTO.setAccount(getAccount(stepInstance));
        esbFileDestinationV3DTO.setServer(stepInstance.getTargetExecuteObjects().toEsbServerV3DTO());
        return esbFileDestinationV3DTO;
    }
}
