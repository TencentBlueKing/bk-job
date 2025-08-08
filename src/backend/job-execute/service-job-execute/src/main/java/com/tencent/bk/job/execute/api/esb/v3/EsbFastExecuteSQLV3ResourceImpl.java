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

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobExecuteV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbFastExecuteSQLV3Request;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Slf4j
public class EsbFastExecuteSQLV3ResourceImpl
    extends JobExecuteCommonV3Processor
    implements EsbFastExecuteSQLV3Resource {

    private final TaskExecuteService taskExecuteService;
    private final MessageI18nService i18nService;

    @Autowired
    public EsbFastExecuteSQLV3ResourceImpl(TaskExecuteService taskExecuteService,
                                           MessageI18nService i18nService) {
        this.taskExecuteService = taskExecuteService;
        this.i18nService = i18nService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_fast_execute_sql"})
    @CustomTimed(metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_API,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_FAST_SQL
        })
    @AuditEntry
    public EsbResp<EsbJobExecuteV3DTO> fastExecuteSQL(String username,
                                                      String appCode,
                                                      @AuditRequestBody EsbFastExecuteSQLV3Request request) {
        ValidateResult validateResult = checkFastExecuteSQLRequest(request);
        if (!validateResult.isPass()) {
            log.warn("Fast execute sql request is illegal!");
            throw new InvalidParamException(validateResult);
        }

        request.trimIps();

        TaskInstanceDTO taskInstance = buildFastSQLTaskInstance(username, appCode, request);
        StepInstanceDTO stepInstance = buildFastSQLStepInstance(username, request);
        TaskInstanceDTO executeTaskInstance = taskExecuteService.executeFastTask(
            FastTaskDTO.builder()
                .taskInstance(taskInstance)
                .stepInstance(stepInstance)
                .startTask(request.getStartTask())
                .build()
        );

        EsbJobExecuteV3DTO jobExecuteInfo = new EsbJobExecuteV3DTO();
        jobExecuteInfo.setTaskInstanceId(executeTaskInstance.getId());
        jobExecuteInfo.setTaskName(stepInstance.getName());
        return EsbResp.buildSuccessResp(jobExecuteInfo);
    }

    private String generateDefaultFastTaskName() {
        return i18nService.getI18n("task.type.name.fast_execute_sql") + "_"
            + DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }

    private ValidateResult checkFastExecuteSQLRequest(EsbFastExecuteSQLV3Request request) {
        boolean isSpecifiedByScriptVersionId = request.getScriptVersionId() != null;
        boolean isSpecifiedByOnlineScript = StringUtils.isNotEmpty(request.getScriptId());
        boolean isSpecifiedByScriptContent = StringUtils.isNotEmpty(request.getContent());
        if (!(isSpecifiedByScriptVersionId || isSpecifiedByOnlineScript || isSpecifiedByScriptContent)) {
            log.warn("Fast execute sql, script is not specified!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                "script_version_id|script_id|script_content");
        }
        if (isSpecifiedByScriptVersionId) {
            if (request.getScriptVersionId() < 1) {
                log.warn("Fast execute sql, scriptVersionId:{} is invalid", request.getScriptVersionId());
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_version_id");
            }
        }

        ValidateResult serverValidateResult = checkServer(request.getTargetServer());
        if (!serverValidateResult.isPass()) {
            log.warn("Fast execute sql, target server is empty!");
            return serverValidateResult;
        }

        if (request.getDbAccountId() == null || request.getDbAccountId() < 1L) {
            log.warn("Fast execute sql, account is empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "db_account_id");
        }
        return ValidateResult.pass();
    }

    private TaskInstanceDTO buildFastSQLTaskInstance(String username,
                                                     String appCode,
                                                     EsbFastExecuteSQLV3Request request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        if (StringUtils.isNotBlank(request.getName())) {
            taskInstance.setName(request.getName());
        } else {
            taskInstance.setName(generateDefaultFastTaskName());
        }
        taskInstance.setCronTaskId(-1L);
        taskInstance.setAppId(request.getAppId());
        taskInstance.setOperator(username);
        taskInstance.setPlanId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setDebugTask(false);
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance.setCurrentStepInstanceId(0L);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setType(TaskTypeEnum.SCRIPT.getValue());
        taskInstance.setAppCode(appCode);
        taskInstance.setCallbackUrl(request.getCallbackUrl());
        return taskInstance;
    }


    private StepInstanceDTO buildFastSQLStepInstance(String username, EsbFastExecuteSQLV3Request request) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setAppId(request.getAppId());
        if (StringUtils.isNotBlank(request.getName())) {
            stepInstance.setName(request.getName());
        } else {
            stepInstance.setName(generateDefaultFastTaskName());
        }
        stepInstance.setStepId(-1L);
        stepInstance.setScriptType(ScriptTypeEnum.SQL);
        if (request.getScriptVersionId() != null && request.getScriptVersionId() > 0) {
            stepInstance.setScriptVersionId(request.getScriptVersionId());
        } else if (StringUtils.isNotBlank(request.getScriptId())) {
            stepInstance.setScriptId(request.getScriptId());
        } else if (StringUtils.isNotBlank(request.getContent())) {
            stepInstance.setScriptContent(Base64Util.decodeContentToStr(request.getContent()));
        }

        stepInstance.setTimeout(request.getTimeout() == null ?
            JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS : request.getTimeout());
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SQL);
        stepInstance.setStatus(RunStatusEnum.BLANK);
        stepInstance.setTargetExecuteObjects(convertToServersDTO(request.getTargetServer()));

        stepInstance.setDbAccountId(request.getDbAccountId());
        stepInstance.setOperator(username);
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        return stepInstance;
    }
}
