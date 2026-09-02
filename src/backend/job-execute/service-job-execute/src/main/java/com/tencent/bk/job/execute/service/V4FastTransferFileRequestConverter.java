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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.RollingTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.DataSizeConverter;
import com.tencent.bk.job.common.util.FilePathValidateUtil;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastTransferFileRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FileSourceDTO;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * v4 分发文件请求 -> 内部执行模型的转换。
 * <p>
 * 仓库里暂无 v4 直接分发文件接口，本转换器是从零新写的一份，参照 v3 的
 * EsbFastTransferFileV3ResourceImpl，并按 v4 协议做了字段调整（去 bk_biz_id、执行目标 v4 化、
 * 源文件账号平铺）。
 * <p>
 * 校验与转换放在一起，供审批预检（dryRun）与放行执行共用，保证两次调用不产生行为漂移。
 * <p>
 * TODO 后续补齐 v4 直接分发文件接口后，v3 的转换逻辑应改为委托本转换器，合并为一份实现，
 * 避免 v3/v4 两份转换长期并存导致行为分叉。
 */
@Slf4j
@Service
public class V4FastTransferFileRequestConverter {

    private final ServiceFileSourceResource fileSourceResource;

    private final ArtifactoryLocalFileService artifactoryLocalFileService;

    @Autowired
    public V4FastTransferFileRequestConverter(ServiceFileSourceResource fileSourceResource,
                                              ArtifactoryLocalFileService artifactoryLocalFileService) {
        this.fileSourceResource = fileSourceResource;
        this.artifactoryLocalFileService = artifactoryLocalFileService;
    }

    /**
     * 校验并把 v4 分发文件请求转换为快速任务
     *
     * @param request  v4 请求
     * @param operator 操作人
     * @param appCode  调用方 appCode
     * @param dryRun   是否只做预检，不产生任何副作用
     * @return 快速任务
     * @throws InvalidParamException 请求参数不合法
     */
    public FastTaskDTO convert(V4FastTransferFileRequest request,
                               User operator,
                               String appCode,
                               boolean dryRun) {
        ValidateResult checkResult = validate(request);
        if (!checkResult.isPass()) {
            throw new InvalidParamException(checkResult);
        }

        String username = operator == null ? null : operator.getUsername();
        StepRollingConfigDTO rollingConfig = null;
        if (request.getRollingConfig() != null) {
            rollingConfig = StepRollingConfigDTO.fromEsbRollingConfig(request.getRollingConfig());
        }
        String taskName = StringUtils.isNotBlank(request.getName())
            ? request.getName() : generateDefaultTaskName();
        return FastTaskDTO.builder()
            .taskInstance(buildTaskInstance(username, appCode, taskName, request))
            .stepInstance(buildStepInstance(username, taskName, request))
            .operator(operator)
            .rollingConfig(rollingConfig)
            .startTask(request.getStartTask())
            .dryRun(dryRun)
            .build();
    }

    public ValidateResult validate(V4FastTransferFileRequest request) {
        if (!FilePathValidateUtil.validateFileSystemAbsolutePath(request.getTrimmedTargetPath())) {
            log.warn("Fast transfer file, target path is invalid! path={}", request.getTrimmedTargetPath());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "file_target_path");
        }
        if ((request.getAccountId() == null || request.getAccountId() <= 0L)
            && StringUtils.isBlank(request.getAccountAlias())) {
            log.warn("Fast transfer file, account is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "account_id|account_alias");
        }
        if (request.getExecuteTarget() == null || request.getExecuteTarget().isTargetEmpty()) {
            log.warn("Fast transfer file, executeTarget is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "execute_target");
        }
        if (CollectionUtils.isEmpty(request.getFileSources())) {
            log.warn("Fast transfer file, file source list is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source_list");
        }
        // 分发模式不传按强制模式处理，但传了就必须是合法取值：静默降级会让调用方以为选中了某种模式
        if (request.getTransferMode() != null
            && FileTransferModeEnum.getFileTransferModeEnum(request.getTransferMode()) == null) {
            log.warn("Fast transfer file, transfer mode is invalid! transferMode={}", request.getTransferMode());
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "transfer_mode");
        }
        if (request.getRollingConfig() != null) {
            ValidateResult result = validateRollingFileSources(request);
            if (result != null) {
                return result;
            }
        }
        for (V4FileSourceDTO fileSource : request.getFileSources()) {
            ValidateResult result = validateFileSource(fileSource);
            if (result != null) {
                return result;
            }
        }
        return ValidateResult.pass();
    }

    /**
     * 按文件源滚动只支持服务器文件
     */
    private ValidateResult validateRollingFileSources(V4FastTransferFileRequest request) {
        if (!RollingTypeEnum.FILE_SOURCE.getValue().equals(request.getRollingConfig().getType())) {
            return null;
        }
        for (V4FileSourceDTO fileSource : request.getFileSources()) {
            Integer fileType = fileSource.getFileType();
            if (fileType != null && fileType != TaskFileTypeEnum.SERVER.getType()) {
                return ValidateResult.fail(ErrorCode.FILE_SOURCE_ROLLING_ONLY_SUPPORT_SERVER_FILE);
            }
        }
        return null;
    }

    private ValidateResult validateFileSource(V4FileSourceDTO fileSource) {
        Integer fileType = fileSource.getFileType();
        // fileType 不传默认为服务器文件
        if (fileType != null && !TaskFileTypeEnum.isValid(fileType)) {
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "file_source.file_type");
        }
        List<String> files = fileSource.getTrimmedFiles();
        if (CollectionUtils.isEmpty(files)) {
            log.warn("File source contains empty file list");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.file_list");
        }
        boolean serverFile = fileType == null || TaskFileTypeEnum.SERVER.getType() == fileType;
        for (String file : files) {
            if (serverFile && !FilePathValidateUtil.validateFileSystemAbsolutePath(file)) {
                log.warn("Invalid path: {}", file);
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "file_source.file_list");
            }
        }
        if (serverFile) {
            if ((fileSource.getAccountId() == null || fileSource.getAccountId() < 1L)
                && StringUtils.isBlank(fileSource.getAccountAlias())) {
                log.warn("File source account is empty!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                    "file_source.account_id|file_source.account_alias");
            }
            if (fileSource.getExecuteTarget() == null || fileSource.getExecuteTarget().isTargetEmpty()) {
                log.warn("File source executeTarget is empty!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.execute_target");
            }
        } else if (TaskFileTypeEnum.FILE_SOURCE.getType() == fileType) {
            Integer fileSourceId = fileSource.getFileSourceId();
            if ((fileSourceId == null || fileSourceId <= 0) && StringUtils.isBlank(fileSource.getFileSourceCode())) {
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                    "file_source.file_source_id|file_source.file_source_code");
            }
        }
        return null;
    }

    private String generateDefaultTaskName() {
        return I18nUtil.getI18nMessage("task.type.name.fast_push_file") + "_"
            + DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }

    private TaskInstanceDTO buildTaskInstance(String username,
                                              String appCode,
                                              String taskName,
                                              V4FastTransferFileRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setType(TaskTypeEnum.FILE.getValue());
        taskInstance.setName(taskName);
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

    private StepInstanceDTO buildStepInstance(String username,
                                              String taskName,
                                              V4FastTransferFileRequest request) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName(taskName);
        stepInstance.setAccountId(request.getAccountId());
        stepInstance.setAccountAlias(request.getAccountAlias());
        stepInstance.setStepId(-1L);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setFileTargetPath(request.getTrimmedTargetPath());
        stepInstance.setFileTargetName(request.getTargetName());
        stepInstance.setFileSourceList(convertFileSources(request.getAppId(), request.getFileSources()));
        stepInstance.setAppId(request.getAppId());
        stepInstance.setTargetExecuteObjects(
            V4ExecuteTargetConverter.v4ToExecuteTargetDTO(request.getExecuteTarget()));
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
        FileTransferModeEnum transferMode = resolveTransferMode(request.getTransferMode());
        stepInstance.setFileDuplicateHandle(transferMode.getDuplicateHandler().getId());
        stepInstance.setNotExistPathHandler(transferMode.getNotExistPathHandler().getValue());
        return stepInstance;
    }

    /**
     * 不传分发模式时按强制模式处理；取值合法性已在 {@link #validate} 中校验过
     */
    private FileTransferModeEnum resolveTransferMode(Integer transferMode) {
        if (transferMode == null) {
            return FileTransferModeEnum.FORCE;
        }
        return FileTransferModeEnum.getFileTransferModeEnum(transferMode);
    }

    private List<FileSourceDTO> convertFileSources(Long appId, List<V4FileSourceDTO> fileSources) {
        if (fileSources == null) {
            return null;
        }
        List<FileSourceDTO> fileSourceDTOList = new ArrayList<>();
        for (V4FileSourceDTO fileSource : fileSources) {
            Integer fileType = fileSource.getFileType();
            if (fileType == null) {
                fileType = TaskFileTypeEnum.SERVER.getType();
            }
            FileSourceDTO fileSourceDTO = new FileSourceDTO();
            fileSourceDTO.setFileType(fileType);
            if (TaskFileTypeEnum.SERVER.getType() == fileType) {
                fileSourceDTO.setAccountId(fileSource.getAccountId());
                fileSourceDTO.setAccountAlias(fileSource.getAccountAlias());
            }
            fileSourceDTO.setLocalUpload(TaskFileTypeEnum.LOCAL.getType() == fileType);
            fileSourceDTO.setFiles(convertFiles(fileType, fileSource.getTrimmedFiles()));
            fileSourceDTO.setFileSourceId(resolveFileSourceId(appId, fileSource));
            fileSourceDTO.setServers(V4ExecuteTargetConverter.v4ToExecuteTargetDTO(fileSource.getExecuteTarget()));
            fileSourceDTOList.add(fileSourceDTO);
        }
        return fileSourceDTOList;
    }

    private List<FileDetailDTO> convertFiles(Integer fileType, List<String> files) {
        List<FileDetailDTO> fileDetailList = new ArrayList<>();
        if (files == null) {
            return fileDetailList;
        }
        for (String file : files) {
            if (TaskFileTypeEnum.LOCAL.getType() == fileType) {
                // 从制品库获取本地文件信息
                fileDetailList.add(artifactoryLocalFileService.getFileDetailFromArtifactory(file));
            } else {
                fileDetailList.add(new FileDetailDTO(file));
            }
        }
        return fileDetailList;
    }

    private Integer resolveFileSourceId(Long appId, V4FileSourceDTO fileSource) {
        if (fileSource.getFileSourceId() != null) {
            return fileSource.getFileSourceId();
        }
        String fileSourceCode = fileSource.getFileSourceCode();
        if (StringUtils.isBlank(fileSourceCode)) {
            return null;
        }
        InternalResponse<Integer> resp;
        try {
            resp = fileSourceResource.getFileSourceIdByCode(appId, fileSourceCode);
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "Fail to parse fileSourceCode to id: {}", fileSourceCode).getMessage();
            log.error(msg, e);
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        if (resp == null || !resp.isSuccess()) {
            log.warn("fileSourceCode={}, resp={}", fileSourceCode, resp);
            throw new NotFoundException(ErrorCode.FILE_SOURCE_SERVICE_INVALID);
        }
        if (resp.getData() == null) {
            log.warn("fileSourceCode={}, resp={}", fileSourceCode, resp);
            throw new NotFoundException(ErrorCode.FAIL_TO_FIND_FILE_SOURCE_BY_CODE, new String[]{fileSourceCode});
        }
        return resp.getData();
    }
}
