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

package com.tencent.bk.job.execute.api.esb.v2.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.EsbFileSourceDTO;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.api.esb.v2.EsbFastPushFileResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbJobExecuteDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbFastPushFileRequest;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
public class EsbFastPushFileResourceImpl extends JobExecuteCommonProcessor implements EsbFastPushFileResource {
    private final TaskExecuteService taskExecuteService;

    private final MessageI18nService i18nService;

    private final AccountService accountService;

    private final AuthService authService;


    @Autowired
    public EsbFastPushFileResourceImpl(TaskExecuteService taskExecuteService, MessageI18nService i18nService,
                                       AccountService accountService, AuthService authService) {
        this.taskExecuteService = taskExecuteService;
        this.i18nService = i18nService;
        this.accountService = accountService;
        this.authService = authService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_fast_push_file"})
    public EsbResp<EsbJobExecuteDTO> fastPushFile(EsbFastPushFileRequest request) {
        ValidateResult checkResult = checkFastPushFileRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Fast transfer file request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        if (StringUtils.isEmpty(request.getName())) {
            request.setName(generateDefaultFastTaskName());
        }

        TaskInstanceDTO taskInstance = buildFastFileTaskInstance(request);
        StepInstanceDTO stepInstance = buildFastFileStepInstance(request);
        long taskInstanceId = taskExecuteService.createTaskInstanceFast(taskInstance, stepInstance);
        taskExecuteService.startTask(taskInstanceId);

        EsbJobExecuteDTO jobExecuteInfo = new EsbJobExecuteDTO();
        jobExecuteInfo.setTaskInstanceId(taskInstanceId);
        jobExecuteInfo.setTaskName(stepInstance.getName());
        return EsbResp.buildSuccessResp(jobExecuteInfo);
    }

    private ValidateResult checkFastPushFileRequest(EsbFastPushFileRequest request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("Fast transfer file, appId invalid!appId is empty or invalid!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (!validateFileSystemPath(request.getTargetPath())) {
            log.warn("Fast transfer file, target path is invalid!path={}", request.getTargetPath());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "file_target_path");
        }
        if (StringUtils.isBlank(request.getAccount())) {
            log.warn("Fast transfer file, account is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "account");
        }

        ValidateResult fileSourceValidateResult = validateFileSource(request);
        if (!fileSourceValidateResult.isPass()) {
            return fileSourceValidateResult;
        }

        if (CollectionUtils.isEmpty(request.getIpList()) &&
            CollectionUtils.isEmpty(request.getDynamicGroupIdList())
            && request.getTargetServer() == null) {
            log.warn("Fast transfer file, target server is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                "ip_list|custom_query_id|target_servers");
        }
        if (request.getTimeout() != null && (request.getTimeout() < 0 || request.getTimeout() > 86400)) {
            log.warn("Fast transfer file, timeout is invalid!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "timeout");
        }

        return ValidateResult.pass();
    }

    private ValidateResult validateFileSource(EsbFastPushFileRequest request) {
        if (request.getFileSources() == null || request.getFileSources().isEmpty()) {
            log.warn("Fast transfer file, file source list is null or empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source");
        }
        List<EsbFileSourceDTO> fileSources = request.getFileSources();
        for (EsbFileSourceDTO fileSource : fileSources) {
            List<String> files = fileSource.getFiles();
            if (files == null || files.isEmpty()) {
                log.warn("File source contains empty file list");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.files");
            }
            for (String file : files) {
                if (!validateFileSystemPath(file)) {
                    log.warn("Invalid path:{}", file);
                    return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "file_source.files");
                }
            }
            String account = fileSource.getAccount();
            if (StringUtils.isEmpty(account)) {
                log.warn("File source account is null!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.account");
            }
            if (CollectionUtils.isEmpty(fileSource.getIpList()) &&
                CollectionUtils.isEmpty(fileSource.getDynamicGroupIdList())
                && fileSource.getTargetServer() == null) {
                log.warn("File source, target server is empty!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                    "file_source.ip_list|file_source.custom_query_id|file_source.target_servers");
            }
        }
        return ValidateResult.pass();
    }

    private boolean validateFileSystemPath(String path) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        if (path.indexOf(' ') != -1) {
            return false;
        }
        Pattern p1 = Pattern.compile("(//|\\\\)+");
        Matcher m1 = p1.matcher(path);
        if (m1.matches()) {
            return false;
        }

        Pattern p2 = Pattern.compile("^[a-zA-Z]:(/|\\\\).*");//windows
        Matcher m2 = p2.matcher(path);

        if (!m2.matches()) { //非windows
            if (path.charAt(0) == '/') {
                return !path.contains("\\\\");
            } else {
                return false;
            }
        }
        return true;
    }

    private TaskInstanceDTO buildFastFileTaskInstance(EsbFastPushFileRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setType(TaskTypeEnum.FILE.getValue());
        taskInstance.setName(request.getName());
        taskInstance.setTaskId(-1L);
        taskInstance.setCronTaskId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setAppId(request.getAppId());
        taskInstance.setStatus(RunStatusEnum.BLANK.getValue());
        taskInstance.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance.setOperator(request.getUserName());
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setCurrentStepId(0L);
        taskInstance.setDebugTask(false);
        taskInstance.setCallbackUrl(request.getCallbackUrl());
        taskInstance.setAppCode(request.getAppCode());
        return taskInstance;
    }

    private String generateDefaultFastTaskName() {
        return i18nService.getI18n("task.type.name.fast_push_file") + "_"
            + DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }

    private StepInstanceDTO buildFastFileStepInstance(EsbFastPushFileRequest request) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName(request.getName());
        AccountDTO account = checkAndGetOsAccount(request.getAppId(), request.getAccount());
        stepInstance.setAccountId(account.getId());
        stepInstance.setAccount(account.getAccount());
        stepInstance.setStepId(-1L);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE.getValue());
        stepInstance.setFileTargetPath(request.getTargetPath());
        stepInstance.setFileSourceList(convertFileSource(request.getAppId(), request.getFileSources()));
        stepInstance.setAppId(request.getAppId());
        stepInstance.setTargetServers(convertToStandardServers(request.getTargetServer(), request.getIpList(),
            request.getDynamicGroupIdList()));
        stepInstance.setOperator(request.getUserName());
        stepInstance.setStatus(RunStatusEnum.BLANK.getValue());
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        if (request.getTimeout() == null) {
            stepInstance.setTimeout(7200);
        } else {
            stepInstance.setTimeout(request.getTimeout());
        }
        if (request.getUploadSpeedLimit() != null && request.getUploadSpeedLimit() > 0) {
            stepInstance.setFileUploadSpeedLimit(request.getUploadSpeedLimit() << 10);
        }
        if (request.getDownloadSpeedLimit() != null && request.getDownloadSpeedLimit() > 0) {
            stepInstance.setFileDownloadSpeedLimit(request.getDownloadSpeedLimit() << 10);
        }
        // 新增参数兼容历史接口调用：默认直接创建
        stepInstance.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR.getValue());
        return stepInstance;
    }

    private AccountDTO checkAndGetOsAccount(long appId, String accountAlias) throws ServiceException {
        AccountDTO account = accountService.getSystemAccountByAlias(accountAlias, appId);
        if (account == null) {
            log.info("Account:{} is not exist in app:{}", accountAlias, appId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, accountAlias);
        }
        if (AccountCategoryEnum.SYSTEM != account.getCategory()) {
            log.info("Account:{} is not os account in app:{}", accountAlias, appId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, accountAlias);
        }
        return account;
    }

    private List<FileSourceDTO> convertFileSource(long appId, List<EsbFileSourceDTO> fileSources)
        throws ServiceException {
        if (fileSources == null) {
            return null;
        }
        List<FileSourceDTO> fileSourceDTOS = new ArrayList<>();
        fileSources.forEach(fileSource -> {
            FileSourceDTO fileSourceDTO = new FileSourceDTO();
            fileSourceDTO.setAccount(fileSource.getAccount());
            AccountDTO account = checkAndGetOsAccount(appId, fileSource.getAccount());
            fileSourceDTO.setAccountId(account.getId());
            fileSourceDTO.setAccount(account.getAccount());
            fileSourceDTO.setLocalUpload(false);
            // ESB接口目前仅支持服务器文件分发
            fileSourceDTO.setFileType(TaskFileTypeEnum.SERVER.getType());
            List<FileDetailDTO> files = new ArrayList<>();
            if (fileSource.getFiles() != null) {
                fileSource.getFiles().forEach(file -> {
                    files.add(new FileDetailDTO(file));
                });
            }
            fileSourceDTO.setFiles(files);
            fileSourceDTO.setServers(convertToStandardServers(fileSource.getTargetServer(), fileSource.getIpList(),
                fileSource.getDynamicGroupIdList()));
            fileSourceDTOS.add(fileSourceDTO);
        });
        return fileSourceDTOS;
    }
}
