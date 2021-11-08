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
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.api.esb.v2.EsbFastExecuteScriptResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbJobExecuteDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbFastExecuteScriptRequest;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Slf4j
public class EsbFastExecuteScriptResourceImpl
    extends JobExecuteCommonProcessor
    implements EsbFastExecuteScriptResource {
    private final TaskExecuteService taskExecuteService;

    private final AccountService accountService;

    private final MessageI18nService i18nService;

    private final ScriptService scriptService;

    private final AuthService authService;

    @Autowired
    public EsbFastExecuteScriptResourceImpl(TaskExecuteService taskExecuteService, AccountService accountService,
                                            MessageI18nService i18nService, ScriptService scriptService,
                                            AuthService authService) {
        this.taskExecuteService = taskExecuteService;
        this.accountService = accountService;
        this.i18nService = i18nService;
        this.scriptService = scriptService;
        this.authService = authService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_fast_execute_script"})
    public EsbResp<EsbJobExecuteDTO> fastExecuteScript(EsbFastExecuteScriptRequest request) {
        ValidateResult checkResult = checkFastExecuteScriptRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Fast execute script request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        request.trimIps();

        ServiceScriptDTO script = getAndCheckScript(request.getAppId(), request.getUserName(), request.getScriptId(),
            scriptService);
        TaskInstanceDTO taskInstance = buildFastScriptTaskInstance(request);
        StepInstanceDTO stepInstance = buildFastScriptStepInstance(request, script);
        long taskInstanceId = taskExecuteService.createTaskInstanceFast(taskInstance, stepInstance);
        taskExecuteService.startTask(taskInstanceId);

        EsbJobExecuteDTO jobExecuteInfo = new EsbJobExecuteDTO();
        jobExecuteInfo.setTaskInstanceId(taskInstanceId);
        jobExecuteInfo.setTaskName(stepInstance.getName());
        jobExecuteInfo.setStepInstanceId(stepInstance.getId());
        return EsbResp.buildSuccessResp(jobExecuteInfo);
    }

    private String generateDefaultFastTaskName() {
        return i18nService.getI18n("task.type.name.fast_execute_script") + "_"
            + DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }

    private ValidateResult checkFastExecuteScriptRequest(EsbFastExecuteScriptRequest request) {
        if (request.getScriptId() != null) {
            if (request.getScriptId() < 1) {
                log.warn("Fast execute script, scriptId:{} is invalid", request.getScriptId());
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_id");
            }
        } else {
            if (!ScriptTypeEnum.isValid(request.getScriptType())) {
                log.warn("Fast execute script, script type is invalid! scriptType={}", request.getScriptType());
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_type");
            }
            if (StringUtils.isBlank(request.getContent())) {
                log.warn("Fast execute script, script content is empty!");
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_content");
            }
        }

        if (CollectionUtils.isEmpty(request.getIpList()) &&
            CollectionUtils.isEmpty(request.getDynamicGroupIdList())
            && request.getTargetServer() == null) {
            log.warn("Fast execute script, target server is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME,
                "ip_list|custom_query_id|target_server");
        }
        if (StringUtils.isBlank(request.getAccount())) {
            log.warn("Fast execute script, account is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "account");
        }
        return ValidateResult.pass();
    }


    private TaskInstanceDTO buildFastScriptTaskInstance(EsbFastExecuteScriptRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        if (StringUtils.isNotBlank(request.getName())) {
            taskInstance.setName(request.getName());
        } else {
            taskInstance.setName(generateDefaultFastTaskName());
        }
        taskInstance.setTaskId(-1L);
        taskInstance.setCronTaskId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setDebugTask(false);
        taskInstance.setAppId(request.getAppId());
        taskInstance.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance.setStatus(RunStatusEnum.BLANK.getValue());
        taskInstance.setOperator(request.getUserName());
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setType(TaskTypeEnum.SCRIPT.getValue());
        taskInstance.setCurrentStepId(0L);
        taskInstance.setCallbackUrl(request.getCallbackUrl());
        taskInstance.setAppCode(request.getAppCode());
        return taskInstance;
    }


    private StepInstanceDTO buildFastScriptStepInstance(EsbFastExecuteScriptRequest request, ServiceScriptDTO script)
        throws ServiceException {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setAppId(request.getAppId());
        if (StringUtils.isNotBlank(request.getName())) {
            stepInstance.setName(request.getName());
        } else {
            stepInstance.setName(generateDefaultFastTaskName());
        }
        stepInstance.setStepId(-1L);
        if (script != null) {
            stepInstance.setScriptContent(script.getContent());
            stepInstance.setScriptType(script.getType());
        } else {
            // 对传入参数进行base64解码
            stepInstance.setScriptContent(Base64Util.decodeContentToStr(request.getContent()));
            stepInstance.setScriptType(request.getScriptType());
        }
        if (StringUtils.isNotEmpty(request.getScriptParam())) {
            String scriptParam = Base64Util.decodeContentToStr(request.getScriptParam());
            // 需要把换行转换成空格，否则脚本执行报错
            if (StringUtils.isNotBlank(scriptParam)) {
                stepInstance.setScriptParam(scriptParam.replace("\n", " "));
            }
        }
        stepInstance.setSecureParam(request.getIsParamSensitive() != null && request.getIsParamSensitive() == 1);
        stepInstance.setTimeout(calculateTimeout(request.getTimeout()));

        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue());
        stepInstance.setStatus(RunStatusEnum.BLANK.getValue());
        stepInstance.setTargetServers(convertToStandardServers(request.getTargetServer(), request.getIpList(),
            request.getDynamicGroupIdList()));
        AccountDTO account = accountService.getSystemAccountByAlias(request.getAccount(), request.getAppId());
        if (account == null) {
            log.info("Account:{} is not exist in app:{}", request.getAccount(), request.getAppId());
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, request.getAccount());
        }
        if (AccountCategoryEnum.SYSTEM != account.getCategory()) {
            log.info("Account:{} is not os account in app:{}", request.getAccount(), request.getAppId());
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, request.getAccount());
        }
        stepInstance.setAccountId(account.getId());
        stepInstance.setOperator(request.getUserName());
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        // ESB接口默认为手工录入的本地脚本
        stepInstance.setScriptSource(1);
        return stepInstance;
    }


}
