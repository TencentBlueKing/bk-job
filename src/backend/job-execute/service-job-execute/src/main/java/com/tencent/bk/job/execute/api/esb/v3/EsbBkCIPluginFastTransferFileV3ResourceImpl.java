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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileSourceV3DTO;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.DataSizeConverter;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobExecuteV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbBkCIPluginFastTransferFileV3Request;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbBkCIPluginFastTransferFileV3ResourceImpl
    extends EsbFileTransferProcessor
    implements EsbBkCIPluginFastTransferFileV3Resource {

    private final MessageI18nService i18nService;
    private final ServiceFileSourceResource fileSourceResource;
    private final TaskExecuteService taskExecuteService;


    @Autowired
    public EsbBkCIPluginFastTransferFileV3ResourceImpl(MessageI18nService i18nService,
                                                       ServiceFileSourceResource fileSourceResource,
                                                       TaskExecuteService taskExecuteService) {
        this.i18nService = i18nService;
        this.fileSourceResource = fileSourceResource;
        this.taskExecuteService = taskExecuteService;
    }

    @Override
    public EsbResp<EsbJobExecuteV3DTO> bkciPluginFastTransferFile(
        String username,
        String appCode,
        EsbBkCIPluginFastTransferFileV3Request request
    ) {
        if (StringUtils.isEmpty(request.getName())) {
            request.setName(generateDefaultFastTaskName());
        }

        User user = JobContextUtil.getUser();
        ValidateResult validateResult = checkFastTransferFileRequest(request);
        if (!validateResult.isPass()) {
            log.warn("[EsbBkCIPluginFastTransferFileV3] request is invalid");
            throw new InvalidParamException(validateResult);
        }

        TaskInstanceDTO taskInstanceDTO = buildFastFileTaskInstance(username, appCode, request);
        StepInstanceDTO stepInstanceDTO = buildFastFileStepInstance(username, request);
        StepRollingConfigDTO rollingConfig = null;
        if (request.getRollingConfig() != null) {
            rollingConfig = StepRollingConfigDTO.fromEsbRollingConfig(request.getRollingConfig());
        }
        TaskInstanceDTO executeTaskInstanceDTO = taskExecuteService.executeFastTask(
            FastTaskDTO.builder()
                .taskInstance(taskInstanceDTO)
                .stepInstance(stepInstanceDTO)
                .rollingConfig(rollingConfig)
                .operator(user)
                .build()
        );

        EsbJobExecuteV3DTO jobExecuteInfo = buildEsbJobExecuteV3Result(
            executeTaskInstanceDTO,
            stepInstanceDTO
        );

        return EsbResp.buildSuccessResp(jobExecuteInfo);
    }

    private ValidateResult checkFastTransferFileRequest(EsbBkCIPluginFastTransferFileV3Request request) {
        ValidateResult validateResult;

        validateResult = validateFilePath(request.trimTargetPath().getTargetPath());
        if (!validateResult.isPass()) {
            return validateResult;
        }
        validateResult = validateAccount(request.getAccountId(), request.getAccountAlias());
        if (!validateResult.isPass()) {
            return validateResult;
        }
        validateResult = validateExecuteObjects(request.getExecuteObject());
        if (!validateResult.isPass()) {
            return validateResult;
        }
        // 校验文件源
        validateResult = validateBkCIPluginFileSources(request.getFileSources());
        if (!validateResult.isPass()) {
            return validateResult;
        }

        return ValidateResult.pass();
    }

    private ValidateResult validateBkCIPluginFileSources(List<EsbFileSourceV3DTO> fileSources) {
        // 插件不支持本地文件分发
        for (EsbFileSourceV3DTO fileSource : fileSources) {
            if (fileSource.getFileType() != null && fileSource.getFileType() == TaskFileTypeEnum.LOCAL.getType()) {
                String rejectReason = "bk-ci plugin does not support local file source";
                log.warn(rejectReason);
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new Object[]{"file_source_list.file_type", rejectReason});
            }
        }
        return validateFileSources(fileSources);
    }

    private TaskInstanceDTO buildFastFileTaskInstance(String username,
                                                      String appCode,
                                                      EsbBkCIPluginFastTransferFileV3Request request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName(request.getName());
        taskInstance.setType(TaskTypeEnum.FILE.getValue());
        taskInstance.setPlanId(-1L);
        taskInstance.setCronTaskId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setAppId(request.getAppId());
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance.setOperator(username);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setCurrentStepInstanceId(0L);
        taskInstance.setDebugTask(false);
        taskInstance.setCallbackUrl(request.getCallbackUrl());
        taskInstance.setAppCode(appCode);
        return taskInstance;
    }

    private StepInstanceDTO buildFastFileStepInstance(String username,
                                                      EsbBkCIPluginFastTransferFileV3Request request) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName(request.getName());
        stepInstance.setAccountId(request.getAccountId());
        stepInstance.setAccountAlias(request.getAccountAlias());
        stepInstance.setStepId(-1L);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setFileTargetPath(request.getTargetPath());
        stepInstance.setFileTargetName(request.getTargetName());
        stepInstance.setFileSourceList(convertFileSource(request.getAppId(), request.getFileSources()));
        stepInstance.setAppId(request.getAppId());
        stepInstance.setTargetExecuteObjects(ExecuteTargetDTO.buildFrom(request.getExecuteObject()));
        stepInstance.setOperator(username);
        stepInstance.setStatus(RunStatusEnum.BLANK);
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        stepInstance.setTimeout(request.getTimeout() == null
            ? JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS : request.getTimeout());
        if (request.getUploadSpeedLimit() != null && request.getUploadSpeedLimit() > 0) {
            stepInstance.setFileUploadSpeedLimit(DataSizeConverter.convertMBToKB(request.getUploadSpeedLimit()));
        }
        if (request.getDownloadSpeedLimit() != null && request.getDownloadSpeedLimit() > 0) {
            stepInstance.setFileDownloadSpeedLimit(DataSizeConverter.convertMBToKB(request.getDownloadSpeedLimit()));
        }
        FileTransferModeEnum transferMode = FileTransferModeEnum.getFileTransferModeEnum(request.getTransferMode());
        if (transferMode == null) {
            transferMode = FileTransferModeEnum.FORCE;
        }
        if (transferMode == FileTransferModeEnum.STRICT) {
            stepInstance.setNotExistPathHandler(NotExistPathHandlerEnum.STEP_FAIL.getValue());
        } else {
            stepInstance.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR.getValue());
        }
        return stepInstance;
    }

    private List<FileSourceDTO> convertFileSource(Long appId,
                                                  List<EsbFileSourceV3DTO> fileSources) throws ServiceException {
        if (fileSources == null) {
            return null;
        }
        List<FileSourceDTO> fileSourceDTOS = new ArrayList<>();
        fileSources.forEach(fileSource -> {
            Integer fileType = fileSource.getFileType();
            if (fileType == null) {
                // 默认服务器文件分发
                fileType = TaskFileTypeEnum.SERVER.getType();
            }
            FileSourceDTO fileSourceDTO = new FileSourceDTO();
            if (TaskFileTypeEnum.SERVER.getType() == fileType) {
                fileSourceDTO.setAccountId(fileSource.getAccount().getId());
                fileSourceDTO.setAccountAlias(fileSource.getAccount().getAlias());
            }
            // 插件调用不支持本地文件分发，所以直接置为false
            fileSourceDTO.setLocalUpload(false);
            fileSourceDTO.setFileType(fileType);
            List<FileDetailDTO> files = new ArrayList<>();
            if (fileSource.getTrimmedFiles() != null) {
                for (String file : fileSource.getTrimmedFiles()) {
                    FileDetailDTO fileDetailDTO = new FileDetailDTO(file);
                    files.add(fileDetailDTO);
                }
            }
            Integer fileSourceId = fileSource.getFileSourceId();
            String fileSourceCode = fileSource.getFileSourceCode();
            if (fileSourceId != null) {
                fileSourceDTO.setFileSourceId(fileSource.getFileSourceId());
            } else if (StringUtils.isNotBlank(fileSourceCode)) {
                try {
                    InternalResponse<Integer> resp = fileSourceResource.getFileSourceIdByCode(appId, fileSourceCode);
                    if (resp != null && resp.isSuccess()) {
                        if (resp.getData() != null) {
                            fileSourceDTO.setFileSourceId(resp.getData());
                        } else {
                            log.warn("fileSourceCode={},resp={}", fileSourceCode, resp);
                            throw new NotFoundException(ErrorCode.FAIL_TO_FIND_FILE_SOURCE_BY_CODE,
                                new String[]{fileSourceCode});
                        }
                    } else {
                        log.warn("fileSourceCode={},resp={}", fileSourceCode, resp);
                        throw new NotFoundException(ErrorCode.FILE_SOURCE_SERVICE_INVALID);
                    }
                } catch (Exception e) {
                    String msg = MessageFormatter.format(
                        "Fail to parse fileSourceCode to id:{}",
                        fileSourceCode
                    ).getMessage();
                    log.error(msg, e);
                    throw new InternalException(ErrorCode.INTERNAL_ERROR);
                }
            }
            fileSourceDTO.setFiles(files);
            fileSourceDTO.setServers(buildFileSourceServer(fileSource.getServer()));
            fileSourceDTOS.add(fileSourceDTO);
        });
        return fileSourceDTOS;
    }

    private EsbJobExecuteV3DTO buildEsbJobExecuteV3Result(TaskInstanceDTO executeTaskInstance,
                                                          StepInstanceDTO stepInstance) {
        EsbJobExecuteV3DTO jobExecuteInfo = new EsbJobExecuteV3DTO();
        jobExecuteInfo.setTaskInstanceId(executeTaskInstance.getId());
        jobExecuteInfo.setStepInstanceId(stepInstance.getId());
        jobExecuteInfo.setTaskName(stepInstance.getName());
        return jobExecuteInfo;
    }

    private String generateDefaultFastTaskName() {
        return i18nService.getI18n("task.type.name.fast_push_file") + "_"
            + DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }
}
