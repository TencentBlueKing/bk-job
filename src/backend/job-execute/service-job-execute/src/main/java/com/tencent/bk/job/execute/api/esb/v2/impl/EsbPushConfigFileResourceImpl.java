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
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.api.esb.common.ConfigFileUtil;
import com.tencent.bk.job.execute.api.esb.v2.EsbPushConfigFileResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.config.StorageSystemConfig;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbJobExecuteDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbPushConfigFileRequest;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbPushConfigFileResourceImpl extends JobExecuteCommonProcessor implements EsbPushConfigFileResource {
    private final TaskExecuteService taskExecuteService;

    private final AccountService accountService;
    private final StorageSystemConfig storageSystemConfig;
    private final AgentService agentService;
    private final ApplicationService applicationService;

    @Autowired
    public EsbPushConfigFileResourceImpl(TaskExecuteService taskExecuteService,
                                         AccountService accountService,
                                         StorageSystemConfig storageSystemConfig,
                                         AgentService agentService,
                                         ApplicationService applicationService) {
        this.taskExecuteService = taskExecuteService;
        this.accountService = accountService;
        this.storageSystemConfig = storageSystemConfig;
        this.agentService = agentService;
        this.applicationService = applicationService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_push_config_file"})
    public EsbResp<EsbJobExecuteDTO> pushConfigFile(EsbPushConfigFileRequest request) {
        ValidateResult checkResult = checkPushConfigFileRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Fast transfer file request is illegal!");
            throw new InvalidParamException(checkResult);
        }
        //appId存在性校验
        ApplicationInfoDTO applicationInfo = applicationService.getAppById(request.getAppId());
        if (applicationInfo == null) {
            throw new NotFoundException(ErrorCode.APP_ID_NOT_EXIST);
        }

        request.trimIps();

        TaskInstanceDTO taskInstance = buildFastFileTaskInstance(request);
        StepInstanceDTO stepInstance = buildFastFileStepInstance(request, request.getFileList());
        long taskInstanceId = taskExecuteService.createTaskInstanceFast(taskInstance, stepInstance);
        taskExecuteService.startTask(taskInstanceId);

        EsbJobExecuteDTO jobExecuteInfo = new EsbJobExecuteDTO();
        jobExecuteInfo.setTaskInstanceId(taskInstanceId);
        jobExecuteInfo.setTaskName(stepInstance.getName());
        return EsbResp.buildSuccessResp(jobExecuteInfo);
    }

    private TaskInstanceDTO buildFastFileTaskInstance(EsbPushConfigFileRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setType(TaskTypeEnum.FILE.getValue());
        if (StringUtils.isNotEmpty(request.getName())) {
            taskInstance.setName(request.getName());
        } else {
            taskInstance.setName("API_PUSH_CONFIG_FILE_" + DateUtils.currentTimeMillis());
        }
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
        taskInstance.setAppCode(request.getAppCode());
        return taskInstance;
    }

    private StepInstanceDTO buildFastFileStepInstance(EsbPushConfigFileRequest request,
                                                      List<EsbPushConfigFileRequest.EsbConfigFileDTO> configFileList) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        if (StringUtils.isNotEmpty(request.getName())) {
            stepInstance.setName(request.getName());
        } else {
            stepInstance.setName("API_PUSH_CONFIG_FILE_" + DateUtils.currentTimeMillis());
        }
        AccountDTO account = checkAndGetOsAccount(request.getAppId(), request.getAccount());
        stepInstance.setAccountId(account.getId());
        stepInstance.setAccount(account.getAccount());
        stepInstance.setStepId(-1L);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE.getValue());
        stepInstance.setFileTargetPath(request.getTargetPath());
        stepInstance.setFileSourceList(convertConfigFileSource(request.getUserName(), configFileList));
        stepInstance.setAppId(request.getAppId());
        stepInstance.setTargetServers(convertToStandardServers(null, request.getIpList(), null));
        stepInstance.setOperator(request.getUserName());
        stepInstance.setStatus(RunStatusEnum.BLANK.getValue());
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        stepInstance.setTimeout(JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS);
        return stepInstance;
    }

    private AccountDTO checkAndGetOsAccount(long appId, String accountAlias) {
        AccountDTO account = accountService.getSystemAccountByAlias(accountAlias, appId);
        if (account == null) {
            log.info("Account:{} is not exist in app:{}", accountAlias, appId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(accountAlias));
        }
        if (AccountCategoryEnum.SYSTEM != account.getCategory()) {
            log.info("Account:{} is not os account in app:{}", accountAlias, appId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(accountAlias));
        }
        return account;
    }

    private List<FileSourceDTO> convertConfigFileSource(String userName,
                                                        List<EsbPushConfigFileRequest.EsbConfigFileDTO> configFileList)
        throws ServiceException {
        if (configFileList == null) {
            return null;
        }
        List<FileSourceDTO> fileSourceDTOS = new ArrayList<>();
        configFileList.forEach(configFile -> {
            FileSourceDTO fileSourceDTO = new FileSourceDTO();
            fileSourceDTO.setAccount("root");
            fileSourceDTO.setLocalUpload(false);
            fileSourceDTO.setFileType(TaskFileTypeEnum.BASE64_FILE.getType());
            // 保存配置文件至机器
            String configFileLocalPath = ConfigFileUtil.saveConfigFileToLocal(
                storageSystemConfig.getJobStorageRootPath(),
                userName,
                configFile.getFileName(),
                configFile.getContent()
            );
            List<FileDetailDTO> files = new ArrayList<>();
            files.add(new FileDetailDTO(configFileLocalPath));
            fileSourceDTO.setFiles(files);
            // 设置配置文件所在机器IP信息
            fileSourceDTO.setServers(agentService.getLocalServersDTO());
            fileSourceDTOS.add(fileSourceDTO);
        });
        return fileSourceDTOS;
    }

    private ValidateResult checkPushConfigFileRequest(EsbPushConfigFileRequest request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("Push config file, appId is empty or invalid!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (StringUtils.isBlank(request.getTargetPath())) {
            log.warn("Push config file, targetPath is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "file_target_path");
        }
        if (StringUtils.isBlank(request.getAccount())) {
            log.warn("Push config file, account is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "account");
        }
        if (request.getIpList() == null || request.getIpList().isEmpty()) {
            log.warn("Push config file, ipList is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "ip_list");
        }
        if (request.getFileList() == null || request.getFileList().isEmpty()) {
            log.warn("Push config file, fileList is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "file_list");
        }

        List<EsbPushConfigFileRequest.EsbConfigFileDTO> fileList = request.getFileList();
        for (EsbPushConfigFileRequest.EsbConfigFileDTO fileInfo : fileList) {
            if (StringUtils.isBlank(fileInfo.getFileName())) {
                log.warn("Push config file, empty file name!");
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "file_list.file_name");
            }
            if (StringUtils.isBlank(fileInfo.getContent())) {
                log.warn("Push config file, config file content is empty!");
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "file_list.content");
            }
        }
        return ValidateResult.pass();
    }

}
