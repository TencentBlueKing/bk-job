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
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbGlobalVarV3DTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobExecuteV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbExecuteJobV3Request;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbExecuteJobPlanV3ResourceImpl
    extends JobExecuteCommonV3Processor
    implements EsbExecuteJobPlanV3Resource {

    private final TaskExecuteService taskExecuteService;

    @Autowired
    public EsbExecuteJobPlanV3ResourceImpl(TaskExecuteService taskExecuteService) {
        this.taskExecuteService = taskExecuteService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_execute_job_plan"})
    @CustomTimed(metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_API,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_EXECUTE_PLAN
        })
    @AuditEntry(actionId = ActionId.LAUNCH_JOB_PLAN)
    public EsbResp<EsbJobExecuteV3DTO> executeJobPlan(String username,
                                                      String appCode,
                                                      @AuditRequestBody EsbExecuteJobV3Request request) {
        ValidateResult checkResult = checkExecuteTaskRequest(request);
        log.info("Execute task, request={}", JsonUtils.toJson(request));
        if (!checkResult.isPass()) {
            log.warn("Execute job request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        request.trimIps();

        List<TaskVariableDTO> executeVariableValues = new ArrayList<>();
        if (request.getGlobalVars() != null) {
            for (EsbGlobalVarV3DTO globalVar : request.getGlobalVars()) {
                TaskVariableDTO taskVariableDTO = new TaskVariableDTO();
                taskVariableDTO.setId(globalVar.getId());
                taskVariableDTO.setName(globalVar.getName());
                EsbServerV3DTO server = globalVar.getServer();
                if (StringUtils.isEmpty(globalVar.getValue()) && server != null && server.checkHostParamsNonEmpty()) {
                    ExecuteTargetDTO executeTargetDTO = convertToServersDTO(globalVar.getServer());
                    taskVariableDTO.setExecuteTarget(executeTargetDTO);
                } else {
                    taskVariableDTO.setValue(globalVar.getValue());
                }
                executeVariableValues.add(taskVariableDTO);
            }
        }
        TaskInstanceDTO taskInstanceDTO = taskExecuteService.executeJobPlan(
            TaskExecuteParam
                .builder()
                .appId(request.getAppId())
                .planId(request.getTaskId())
                .operator(username)
                .executeVariableValues(executeVariableValues)
                .startupMode(TaskStartupModeEnum.API)
                .callbackUrl(request.getCallbackUrl())
                .appCode(appCode)
                .startTask(request.getStartTask())
                .build());

        EsbJobExecuteV3DTO result = new EsbJobExecuteV3DTO();
        result.setTaskInstanceId(taskInstanceDTO.getId());
        result.setTaskName(taskInstanceDTO.getName());
        return EsbResp.buildSuccessResp(result);
    }

    private ValidateResult checkExecuteTaskRequest(EsbExecuteJobV3Request request) {
        if (request.getTaskId() == null || request.getTaskId() <= 0) {
            log.warn("Execute task, taskId is empty!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "job_plan_id");
        }
        if (request.getGlobalVars() != null) {
            for (EsbGlobalVarV3DTO globalVar : request.getGlobalVars()) {
                if ((globalVar.getId() == null || globalVar.getId() <= 0) && StringUtils.isBlank(globalVar.getName())) {
                    log.warn("Execute task, both variable id and name are empty");
                    return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                        "global_var.id|global_var.name");
                }
            }
        }
        return ValidateResult.pass();
    }
}
