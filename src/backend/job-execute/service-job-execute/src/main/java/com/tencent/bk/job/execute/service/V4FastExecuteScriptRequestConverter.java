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

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.util.FastTaskUtil;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * v4 快速执行脚本请求 -> 内部执行模型的转换。
 * <p>
 * 从 OpenApiFastExecuteScriptV4ResourceImpl 抽取而来，供 ESB v4 接口与审批预检/放行的
 * inner 接口共用，保证预检与执行走同一份转换逻辑、不产生行为漂移。
 */
public class V4FastExecuteScriptRequestConverter {

    private V4FastExecuteScriptRequestConverter() {
    }

    /**
     * 把 v4 快速执行脚本请求转换为快速任务
     *
     * @param request  v4 请求
     * @param operator 操作人
     * @param appCode  调用方 appCode
     * @param dryRun   是否只做预检，不产生任何副作用
     * @return 快速任务
     */
    public static FastTaskDTO convert(V4FastExecuteScriptRequest request,
                                      User operator,
                                      String appCode,
                                      boolean dryRun) {
        String username = operator == null ? null : operator.getUsername();
        StepRollingConfigDTO rollingConfig = null;
        if (request.getRollingConfig() != null) {
            rollingConfig = StepRollingConfigDTO.fromEsbRollingConfig(request.getRollingConfig());
        }
        return FastTaskDTO.builder()
            .taskInstance(buildTaskInstance(username, appCode, request))
            .stepInstance(buildStepInstance(username, request))
            .operator(operator)
            .rollingConfig(rollingConfig)
            .startTask(request.getStartTask())
            .hostPasswordList(request.getHostPasswordList())
            .dryRun(dryRun)
            .build();
    }

    public static TaskInstanceDTO buildTaskInstance(String username,
                                                    String appCode,
                                                    V4FastExecuteScriptRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        if (StringUtils.isNotBlank(request.getName())) {
            taskInstance.setName(request.getName());
        } else {
            taskInstance.setName(FastTaskUtil.getFastScriptTaskName());
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

    public static StepInstanceDTO buildStepInstance(String username, V4FastExecuteScriptRequest request) {
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
