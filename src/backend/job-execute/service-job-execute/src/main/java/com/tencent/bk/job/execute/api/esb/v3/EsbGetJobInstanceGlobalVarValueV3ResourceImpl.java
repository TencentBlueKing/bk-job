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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.execute.api.esb.v2.impl.JobQueryCommonProcessor;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobInstanceGlobalVarValueV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobInstanceGlobalVarValueV3DTO.EsbStepInstanceGlobalVarValuesV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobInstanceGlobalVarValueV3DTO.GlobalVarValueV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbGetJobInstanceGlobalVarValueV3Request;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbGetJobInstanceGlobalVarValueV3ResourceImpl
    extends JobQueryCommonProcessor
    implements EsbGetJobInstanceGlobalVarValueV3Resource {

    private final TaskInstanceService taskInstanceService;
    private final MessageI18nService i18nService;
    private final StepInstanceVariableValueService stepInstanceVariableValueService;

    public EsbGetJobInstanceGlobalVarValueV3ResourceImpl(MessageI18nService i18nService,
                                                         TaskInstanceService taskInstanceService,
                                                         StepInstanceVariableValueService stepInstanceVariableValueService) {
        this.i18nService = i18nService;
        this.taskInstanceService = taskInstanceService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v3_get_job_instance_var_value"})
    public EsbResp<EsbJobInstanceGlobalVarValueV3DTO> getJobInstanceGlobalVarValueUsingPost(
        EsbGetJobInstanceGlobalVarValueV3Request request) {

        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance global var value, request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        authViewTaskInstance(request.getUserName(), request.getAppId(), taskInstance);

        EsbJobInstanceGlobalVarValueV3DTO result = new EsbJobInstanceGlobalVarValueV3DTO();
        result.setTaskInstanceId(taskInstanceId);

        List<StepInstanceVariableValuesDTO> stepInstanceVariableValuesList =
            stepInstanceVariableValueService.computeOutputVariableValuesForAllStep(taskInstanceId);
        List<EsbStepInstanceGlobalVarValuesV3DTO> stepGlobalVarValues = new ArrayList<>();
        stepInstanceVariableValuesList.forEach(stepInstanceVariableValues -> {
            EsbStepInstanceGlobalVarValuesV3DTO globalVarValues = new EsbStepInstanceGlobalVarValuesV3DTO();
            globalVarValues.setStepInstanceId(stepInstanceVariableValues.getStepInstanceId());
            if (CollectionUtils.isNotEmpty(stepInstanceVariableValues.getGlobalParams())) {
                List<GlobalVarValueV3DTO> stepInstanceGlobalVarValues = new ArrayList<>();
                stepInstanceVariableValues.getGlobalParams().stream()
                    // only return string global variable
                    .filter(globalVarValue -> TaskVariableTypeEnum.STRING.getType() == globalVarValue.getType())
                    .forEach(globalVarValue -> {
                        GlobalVarValueV3DTO esbGlobalVarValue = new GlobalVarValueV3DTO();
                        esbGlobalVarValue.setName(globalVarValue.getName());
                        esbGlobalVarValue.setType(globalVarValue.getType());
                        esbGlobalVarValue.setValue(globalVarValue.getValue());
                        stepInstanceGlobalVarValues.add(esbGlobalVarValue);
                    });
                globalVarValues.setGlobalVarValues(stepInstanceGlobalVarValues);
            }
            stepGlobalVarValues.add(globalVarValues);
        });
        result.setStepGlobalVarValues(stepGlobalVarValues);

        return EsbResp.buildSuccessResp(result);
    }

    private ValidateResult checkRequest(EsbGetJobInstanceGlobalVarValueV3Request request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("App is empty or illegal, appId={}", request.getAppId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
        }
        return ValidateResult.pass();
    }

    @Override
    public EsbResp<EsbJobInstanceGlobalVarValueV3DTO> getJobInstanceGlobalVarValue(String username,
                                                                                   String appCode,
                                                                                   Long appId,
                                                                                   Long taskInstanceId) {
        EsbGetJobInstanceGlobalVarValueV3Request request = new EsbGetJobInstanceGlobalVarValueV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setAppId(appId);
        request.setTaskInstanceId(taskInstanceId);
        return getJobInstanceGlobalVarValueUsingPost(request);
    }
}
