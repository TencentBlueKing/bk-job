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
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.ExecuteObjectsDTO;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobExecuteV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbBkCIPluginFastExecuteScriptRequest;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Slf4j
public class EsbBkCIPluginFastExecuteScriptV3ResourceImpl extends JobExecuteCommonV3Processor
    implements EsbBkCIPluginFastExecuteScriptV3Resource {

    private final TaskExecuteService taskExecuteService;
    private final TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    private final MessageI18nService i18nService;

    @Autowired
    public EsbBkCIPluginFastExecuteScriptV3ResourceImpl(TaskExecuteService taskExecuteService,
                                                        TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                                        MessageI18nService i18nService) {
        this.taskExecuteService = taskExecuteService;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.i18nService = i18nService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_bkci_plugin_fast_execute_script"})
    @CustomTimed(metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_API,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_FAST_SCRIPT
        })
    @AuditEntry
    public EsbResp<EsbJobExecuteV3DTO> fastExecuteScript(
        String username,
        String appCode,
        @AuditRequestBody EsbBkCIPluginFastExecuteScriptRequest request) {

        StepInstanceDTO stepInstance = buildFastScriptStepInstance(username, request);
        StepRollingConfigDTO rollingConfig = null;
        if (request.getRollingConfig() != null) {
            rollingConfig = StepRollingConfigDTO.fromEsbRollingConfig(request.getRollingConfig());
        }
        TaskInstanceDTO executeTaskInstance = taskExecuteService.executeFastTask(
            FastTaskDTO.builder()
                .taskInstance(taskInstance)
                .stepInstance(stepInstance)
                .rollingConfig(rollingConfig)
                .build()
        );

        EsbJobExecuteV3DTO jobExecuteInfo = new EsbJobExecuteV3DTO();
        jobExecuteInfo.setTaskInstanceId(executeTaskInstance.getId());
        jobExecuteInfo.setTaskName(stepInstance.getName());
        jobExecuteInfo.setStepInstanceId(stepInstance.getId());
        return EsbResp.buildSuccessResp(jobExecuteInfo);
    }

    private String generateDefaultFastTaskName() {
        return i18nService.getI18n("task.type.name.fast_execute_script") + "_"
            + DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }

    private TaskInstanceDTO buildFastScriptTaskInstance(String username,
                                                        String appCode,
                                                        EsbBkCIPluginFastExecuteScriptRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        if (StringUtils.isNotBlank(request.getName())) {
            taskInstance.setName(request.getName());
        } else {
            taskInstance.setName(generateDefaultFastTaskName());
        }
        taskInstance.setPlanId(-1L);
        taskInstance.setCronTaskId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setDebugTask(false);
        taskInstance.setAppId(request.getAppId());
        taskInstance.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setOperator(username);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setType(TaskTypeEnum.SCRIPT.getValue());
        taskInstance.setCurrentStepInstanceId(0L);
        if (request.getCallback() != null) {
            taskInstance.setCallbackUrl(request.getCallback().getUrl());
        }
        taskInstance.setAppCode(appCode);
        return taskInstance;
    }

    private StepInstanceDTO buildFastScriptStepInstance(String username,
                                                        EsbBkCIPluginFastExecuteScriptRequest request) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setAppId(request.getAppId());
        if (StringUtils.isNotBlank(request.getName())) {
            stepInstance.setName(request.getName());
        } else {
            stepInstance.setName(generateDefaultFastTaskName());
        }
        stepInstance.setStepId(-1L);

        if (request.getScriptVersionId() != null && request.getScriptVersionId() > 0) {
            stepInstance.setScriptVersionId(request.getScriptVersionId());
        } else if (StringUtils.isNotBlank(request.getScriptId())) {
            stepInstance.setScriptId(request.getScriptId());
        } else if (StringUtils.isNotBlank(request.getContent())) {
            stepInstance.setScriptContent(Base64Util.decodeContentToStr(request.getContent()));
            stepInstance.setScriptType(ScriptTypeEnum.valOf(request.getScriptLanguage()));
        }

        if (StringUtils.isNotEmpty(request.getScriptParam())) {
            String scriptParam = Base64Util.decodeContentToStr(request.getScriptParam());
            // 需要把换行转换成空格，否则脚本执行报错
            if (StringUtils.isNotBlank(scriptParam)) {
                stepInstance.setScriptParam(scriptParam.replace("\n", " "));
            }
        }
        stepInstance.setSecureParam(request.isParamSensitive());
        stepInstance.setTimeout(
            request.getTimeout() == null ? JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS : request.getTimeout());

        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setStatus(RunStatusEnum.BLANK);
        stepInstance.setTargetExecuteObjects(ExecuteObjectsDTO.buildFrom(request.getExecuteTarget()));
        stepInstance.setAccountId(request.getAccountId());
        stepInstance.setAccountAlias(request.getAccountAlias());
        stepInstance.setOperator(username);
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());

        return stepInstance;
    }
}
