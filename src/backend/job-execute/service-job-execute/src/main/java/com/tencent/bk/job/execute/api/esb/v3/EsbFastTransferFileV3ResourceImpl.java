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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbAccountV3BasicDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileSourceV3DTO;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.FilePathValidateUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.execute.client.FileSourceResourceClient;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobExecuteV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbFastTransferFileV3Request;
import com.tencent.bk.job.execute.service.ArtifactoryLocalFileService;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
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
public class EsbFastTransferFileV3ResourceImpl
    extends JobExecuteCommonV3Processor
    implements EsbFastTransferFileV3Resource {
    private final TaskExecuteService taskExecuteService;

    private final FileSourceResourceClient fileSourceService;

    private final MessageI18nService i18nService;

    private final ArtifactoryLocalFileService artifactoryLocalFileService;

    private final AppScopeMappingService appScopeMappingService;


    @Autowired
    public EsbFastTransferFileV3ResourceImpl(TaskExecuteService taskExecuteService,
                                             FileSourceResourceClient fileSourceService,
                                             MessageI18nService i18nService,
                                             ArtifactoryLocalFileService artifactoryLocalFileService,
                                             AppScopeMappingService appScopeMappingService) {
        this.taskExecuteService = taskExecuteService;
        this.fileSourceService = fileSourceService;
        this.i18nService = i18nService;
        this.artifactoryLocalFileService = artifactoryLocalFileService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_fast_transfer_file"})
    @CustomTimed(metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_API,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_FAST_FILE
        })
    public EsbResp<EsbJobExecuteV3DTO> fastTransferFile(EsbFastTransferFileV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);

        ValidateResult checkResult = checkFastTransferFileRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Fast transfer file request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        if (StringUtils.isEmpty(request.getName())) {
            request.setName(generateDefaultFastTaskName());
        }

        TaskInstanceDTO taskInstance = buildFastFileTaskInstance(request);
        StepInstanceDTO stepInstance = buildFastFileStepInstance(request);
        StepRollingConfigDTO rollingConfig = null;
        if (request.getRollingConfig() != null) {
            rollingConfig = StepRollingConfigDTO.fromEsbRollingConfig(request.getRollingConfig());
        }
        long taskInstanceId = taskExecuteService.executeFastTask(
            FastTaskDTO.builder()
                .taskInstance(taskInstance)
                .stepInstance(stepInstance)
                .rollingConfig(rollingConfig)
                .build()
        );

        EsbJobExecuteV3DTO jobExecuteInfo = new EsbJobExecuteV3DTO();
        jobExecuteInfo.setTaskInstanceId(taskInstanceId);
        jobExecuteInfo.setStepInstanceId(stepInstance.getId());
        jobExecuteInfo.setTaskName(stepInstance.getName());
        return EsbResp.buildSuccessResp(jobExecuteInfo);
    }

    private ValidateResult checkFileSource(EsbFileSourceV3DTO fileSource) {
        Integer fileType = fileSource.getFileType();
        // fileType是后加的字段，为null则默认为服务器文件不校验
        if (fileType != null && !TaskFileTypeEnum.isValid(fileType)) {
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "file_source.file_type");
        }
        List<String> files = fileSource.getFiles();
        if (files == null || files.isEmpty()) {
            log.warn("File source contains empty file list");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.file_list");
        }
        for (String file : files) {
            if ((fileType == null
                || TaskFileTypeEnum.SERVER.getType() == fileType)
                && !FilePathValidateUtil.validateFileSystemAbsolutePath(file)) {
                log.warn("Invalid path:{}", file);
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "file_source.file_list");
            }
        }
        if (null == fileType || TaskFileTypeEnum.SERVER.getType() == fileType) {
            //对文件源类型为服务器文件的文件源校验账号和服务器信息
            EsbAccountV3BasicDTO account = fileSource.getAccount();
            if (account == null) {
                log.warn("File source account is null!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.account");
            }
            if ((account.getId() == null || account.getId() < 1L) && StringUtils.isBlank(account.getAlias())) {
                log.warn("File source account is empty!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                    "file_source.account.account_id|file_source.account.account_alias");
            }
            if (!checkServer(fileSource.getServer()).isPass()) {
                log.warn("File source server is empty!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.server");
            }
        } else if (TaskFileTypeEnum.FILE_SOURCE.getType() == fileType) {
            //对文件源类型为第三方文件源的文件源校验Id与Code
            Integer fileSourceId = fileSource.getFileSourceId();
            String fileSourceCode = fileSource.getFileSourceCode();
            if ((fileSourceId == null || fileSourceId <= 0) && StringUtils.isBlank(fileSourceCode)) {
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                    "file_source.file_source_id/file_source.file_source_code");
            }
        }
        return null;
    }

    private ValidateResult checkFastTransferFileRequest(EsbFastTransferFileV3Request request) {
        if (!FilePathValidateUtil.validateFileSystemAbsolutePath(request.getTargetPath())) {
            log.warn("Fast transfer file, target path is invalid!path={}", request.getTargetPath());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "file_target_path");
        }
        if ((request.getAccountId() == null || request.getAccountId() <= 0L)
            && StringUtils.isBlank(request.getAccountAlias())) {
            log.warn("Fast transfer file, account is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "account_id|account_alias");
        }
        if (!checkServer(request.getTargetServer()).isPass()) {
            log.warn("Fast transfer file, targetServer is illegal!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "target_server");
        }

        if (request.getFileSources() == null || request.getFileSources().isEmpty()) {
            log.warn("Fast transfer file, file source list is null or empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source_list");
        }
        List<EsbFileSourceV3DTO> fileSources = request.getFileSources();
        for (EsbFileSourceV3DTO fileSource : fileSources) {
            ValidateResult result = checkFileSource(fileSource);
            if (result != null) return result;
        }

        return ValidateResult.pass();
    }

    private TaskInstanceDTO buildFastFileTaskInstance(EsbFastTransferFileV3Request request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setType(TaskTypeEnum.FILE.getValue());
        taskInstance.setName(request.getName());
        taskInstance.setTaskId(-1L);
        taskInstance.setCronTaskId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setAppId(request.getAppId());
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance.setOperator(request.getUserName());
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setCurrentStepInstanceId(0L);
        taskInstance.setDebugTask(false);
        taskInstance.setCallbackUrl(request.getCallbackUrl());
        taskInstance.setAppCode(request.getAppCode());
        return taskInstance;
    }

    private String generateDefaultFastTaskName() {
        return i18nService.getI18n("task.type.name.fast_push_file") + "_"
            + DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }

    private StepInstanceDTO buildFastFileStepInstance(EsbFastTransferFileV3Request request) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName(request.getName());
        stepInstance.setAccountId(request.getAccountId());
        stepInstance.setAccountAlias(request.getAccountAlias());
        stepInstance.setStepId(-1L);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE.getValue());
        stepInstance.setFileTargetPath(request.getTargetPath());
        stepInstance.setFileTargetName(request.getTargetName());
        stepInstance.setFileSourceList(convertFileSource(request.getFileSources()));
        stepInstance.setAppId(request.getAppId());
        stepInstance.setTargetServers(convertToServersDTO(request.getTargetServer()));
        stepInstance.setOperator(request.getUserName());
        stepInstance.setStatus(RunStatusEnum.BLANK);
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        stepInstance.setTimeout(request.getTimeout() == null ?
            JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS : request.getTimeout());
        if (request.getUploadSpeedLimit() != null && request.getUploadSpeedLimit() > 0) {
            stepInstance.setFileUploadSpeedLimit(request.getUploadSpeedLimit() << 10);
        }
        if (request.getDownloadSpeedLimit() != null && request.getDownloadSpeedLimit() > 0) {
            stepInstance.setFileDownloadSpeedLimit(request.getDownloadSpeedLimit() << 10);
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

    private List<FileSourceDTO> convertFileSource(List<EsbFileSourceV3DTO> fileSources) throws ServiceException {
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
            if (TaskFileTypeEnum.LOCAL.getType() == fileType) {
                fileSourceDTO.setLocalUpload(true);
            } else {
                fileSourceDTO.setLocalUpload(false);
            }
            fileSourceDTO.setFileType(fileType);
            List<FileDetailDTO> files = new ArrayList<>();
            if (fileSource.getFiles() != null) {
                for (String file : fileSource.getFiles()) {
                    FileDetailDTO fileDetailDTO;
                    if (fileType == TaskFileTypeEnum.LOCAL.getType()) {
                        // 从制品库获取本地文件信息
                        fileDetailDTO = artifactoryLocalFileService.getFileDetailFromArtifactory(file);
                    } else {
                        fileDetailDTO = new FileDetailDTO(file);
                    }
                    files.add(fileDetailDTO);
                }
            }
            Integer fileSourceId = fileSource.getFileSourceId();
            String fileSourceCode = fileSource.getFileSourceCode();
            if (fileSourceId != null) {
                fileSourceDTO.setFileSourceId(fileSource.getFileSourceId());
            } else if (StringUtils.isNotBlank(fileSourceCode)) {
                try {
                    InternalResponse<Integer> resp = fileSourceService.getFileSourceIdByCode(fileSourceCode);
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
            fileSourceDTO.setServers(convertToServersDTO(fileSource.getServer()));
            fileSourceDTOS.add(fileSourceDTO);
        });
        return fileSourceDTOS;
    }
}
