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
import com.tencent.bk.job.execute.api.esb.v2.EsbFastExecuteSQLResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbJobExecuteDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbFastExecuteSQLRequest;
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
public class EsbFastExecuteSQLResourceImpl extends JobExecuteCommonProcessor implements EsbFastExecuteSQLResource {
    private final TaskExecuteService taskExecuteService;

    private final AccountService accountService;

    private final MessageI18nService i18nService;

    private final ScriptService scriptService;

    private final AuthService authService;

    @Autowired
    public EsbFastExecuteSQLResourceImpl(TaskExecuteService taskExecuteService, AccountService accountService,
                                         MessageI18nService i18nService, ScriptService scriptService,
                                         AuthService authService) {
        this.taskExecuteService = taskExecuteService;
        this.accountService = accountService;
        this.i18nService = i18nService;
        this.scriptService = scriptService;
        this.authService = authService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_fast_execute_sql"})
    public EsbResp<EsbJobExecuteDTO> fastExecuteSQL(EsbFastExecuteSQLRequest request) {
        ValidateResult validateResult = checkFastExecuteSQLRequest(request);
        if (!validateResult.isPass()) {
            log.warn("Fast execute SQL request is illegal!");
            throw new InvalidParamException(validateResult);
        }

        request.trimIps();

        ServiceScriptDTO script = getAndCheckScript(request.getAppId(), request.getUserName(), request.getScriptId(),
            scriptService);
        TaskInstanceDTO taskInstance = buildFastSQLTaskInstance(request);
        StepInstanceDTO stepInstance = buildFastSQLStepInstance(request, script);
        long taskInstanceId = taskExecuteService.createTaskInstanceFast(taskInstance, stepInstance);
        taskExecuteService.startTask(taskInstanceId);

        EsbJobExecuteDTO jobExecuteInfo = new EsbJobExecuteDTO();
        jobExecuteInfo.setTaskInstanceId(taskInstanceId);
        jobExecuteInfo.setTaskName(stepInstance.getName());
        return EsbResp.buildSuccessResp(jobExecuteInfo);
    }

    private String generateDefaultFastTaskName() {
        return i18nService.getI18n("task.type.name.fast_execute_sql") + "_"
            + DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }

    private ValidateResult checkFastExecuteSQLRequest(EsbFastExecuteSQLRequest request) {
        if (request.getScriptId() != null) {
            if (request.getScriptId() < 1) {
                log.warn("Fast execute SQL, scriptId:{} is invalid", request.getScriptId());
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_id");
            }
        } else {
            if (StringUtils.isBlank(request.getContent())) {
                log.warn("Fast execute SQL, script content is empty!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "script_id");
            }
        }

        if (CollectionUtils.isEmpty(request.getIpList()) &&
            CollectionUtils.isEmpty(request.getDynamicGroupIdList())
            && request.getTargetServer() == null) {
            log.warn("Fast execute SQL, target server is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME,
                "ip_list|custom_query_id|target_server");
        }
        if (request.getDbAccountId() == null) {
            log.warn("Fast execute SQL, account is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "db_account_id");
        }
        return ValidateResult.pass();
    }


    private TaskInstanceDTO buildFastSQLTaskInstance(EsbFastExecuteSQLRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        if (StringUtils.isNotBlank(request.getName())) {
            taskInstance.setName(request.getName());
        } else {
            taskInstance.setName(generateDefaultFastTaskName());
        }
        taskInstance.setCronTaskId(-1L);
        taskInstance.setAppId(request.getAppId());
        taskInstance.setOperator(request.getUserName());
        taskInstance.setTaskId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setDebugTask(false);
        taskInstance.setStatus(RunStatusEnum.BLANK.getValue());
        taskInstance.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance.setCurrentStepId(0L);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setType(TaskTypeEnum.SCRIPT.getValue());
        taskInstance.setAppCode(request.getAppCode());
        taskInstance.setCallbackUrl(request.getCallbackUrl());
        return taskInstance;
    }


    private StepInstanceDTO buildFastSQLStepInstance(EsbFastExecuteSQLRequest request, ServiceScriptDTO script)
        throws ServiceException {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setAppId(request.getAppId());
        if (StringUtils.isNotBlank(request.getName())) {
            stepInstance.setName(request.getName());
        } else {
            stepInstance.setName(generateDefaultFastTaskName());
        }
        stepInstance.setStepId(-1L);
        stepInstance.setScriptType(ScriptTypeEnum.SQL.getValue());
        if (script != null) {
            stepInstance.setScriptContent(script.getContent());
        } else {
            // 对传入参数进行base64解码
            stepInstance.setScriptContent(Base64Util.decodeContentToStr(request.getContent()));
        }

        stepInstance.setTimeout(calculateTimeout(request.getTimeout()));
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SQL.getValue());
        stepInstance.setStatus(RunStatusEnum.BLANK.getValue());
        stepInstance.setTargetServers(convertToStandardServers(request.getTargetServer(), request.getIpList(),
            request.getDynamicGroupIdList()));

        AccountDTO account = accountService.getAccountById(request.getDbAccountId());
        if (account == null) {
            log.info("Account:{} is not exist in app:{}", request.getDbAccountId(), request.getAppId());
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, "ID=" + request.getDbAccountId());
        }
        if (AccountCategoryEnum.DB != account.getCategory()) {
            log.info("Account:{} is not db account in app:{}", request.getDbAccountId(), request.getAppId());
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, "ID=" + request.getDbAccountId());
        }
        stepInstance.setAccountId(account.getDbSystemAccountId());
        stepInstance.setDbAccountId(account.getId());
        stepInstance.setOperator(request.getUserName());
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        return stepInstance;
    }
}
