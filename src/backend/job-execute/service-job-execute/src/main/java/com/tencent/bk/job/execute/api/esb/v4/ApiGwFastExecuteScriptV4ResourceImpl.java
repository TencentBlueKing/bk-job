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

package com.tencent.bk.job.execute.api.esb.v4;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
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
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobExecuteDTO;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.execute.service.V4ExecuteTargetConverter;
import com.tencent.bk.job.execute.util.FastTaskUtil;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ApiGwFastExecuteScriptV4ResourceImpl implements ApiGwFastExecuteScriptV4Resource {

    private final TaskExecuteService taskExecuteService;

    @Autowired
    public ApiGwFastExecuteScriptV4ResourceImpl(TaskExecuteService taskExecuteService) {
        this.taskExecuteService = taskExecuteService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_fast_execute_script"})
    @CustomTimed(
        metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_API,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_FAST_SCRIPT
        })
    @AuditEntry
    public EsbV4Response<V4JobExecuteDTO> fastExecuteScript(String username,
                                                            String appCode,
                                                            @AuditRequestBody V4FastExecuteScriptRequest request) {

        TaskInstanceDTO taskInstance = buildFastScriptTaskInstance(username, appCode, request);
        StepInstanceDTO stepInstance = buildFastScriptStepInstance(username, request);
        StepRollingConfigDTO rollingConfig = null;
        if (request.getRollingConfig() != null) {
            rollingConfig = StepRollingConfigDTO.fromEsbRollingConfig(request.getRollingConfig());
        }
        taskExecuteService.executeFastTask(
            FastTaskDTO.builder()
                .taskInstance(taskInstance)
                .stepInstance(stepInstance)
                .rollingConfig(rollingConfig)
                .startTask(request.getStartTask())
                .hostPasswordList(request.getHostPasswordList())
                .build()
        );

        V4JobExecuteDTO jobExecuteDTO = new V4JobExecuteDTO();
        jobExecuteDTO.setTaskInstanceId(taskInstance.getId());
        jobExecuteDTO.setStepInstanceId(stepInstance.getId());
        jobExecuteDTO.setTaskName(taskInstance.getName());
        return EsbV4Response.success(jobExecuteDTO);
    }

    private TaskInstanceDTO buildFastScriptTaskInstance(String username,
                                                        String appCode,
                                                        V4FastExecuteScriptRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        if (StringUtils.isNotBlank(request.getName())) {
            taskInstance.setName(request.getName());
        } else {
            String taskName = FastTaskUtil.getFastScriptTaskName();
            taskInstance.setName(taskName);
        }
        taskInstance.setPlanId(-1L);
        taskInstance.setCronTaskId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setDebugTask(false);
        taskInstance.setAppId(request.getAppId());
        taskInstance.setOperator(username);
        taskInstance.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setType(TaskTypeEnum.SCRIPT.getValue());
        taskInstance.setCurrentStepInstanceId(0L);
        taskInstance.setCallbackUrl(request.getCallbackUrl());
        taskInstance.setAppCode(appCode);
        return taskInstance;
    }

    private StepInstanceDTO buildFastScriptStepInstance(String username,
                                                        V4FastExecuteScriptRequest request) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();

        if (StringUtils.isNotBlank(request.getName())) {
            stepInstance.setName(request.getName());
        } else {
            stepInstance.setName(FastTaskUtil.getFastScriptTaskName());
        }

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

        stepInstance.setAppId(request.getAppId());
        stepInstance.setStepId(-1L);
        stepInstance.setSecureParam(request.isParamSensitive());
        stepInstance.setWindowsInterpreter(request.getTrimmedWindowsInterpreter());
        stepInstance.setTimeout(
            request.getTimeout() == null ? JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS : request.getTimeout());
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setStatus(RunStatusEnum.BLANK);
        stepInstance.setTargetExecuteObjects(V4ExecuteTargetConverter.v4ToExecuteTargetDTO(request.getExecuteTarget()));
        stepInstance.setAccountId(request.getAccountId());
        stepInstance.setAccountAlias(request.getAccountAlias());
        stepInstance.setOperator(username);
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        return stepInstance;
    }
}
