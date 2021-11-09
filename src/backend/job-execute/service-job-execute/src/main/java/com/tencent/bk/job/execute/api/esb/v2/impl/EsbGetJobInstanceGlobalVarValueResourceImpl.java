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
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.execute.api.esb.v2.EsbGetJobInstanceGlobalVarValueResource;
import com.tencent.bk.job.execute.api.esb.v3.EsbGetJobInstanceGlobalVarValueV3Resource;
import com.tencent.bk.job.execute.model.esb.v2.EsbTaskInstanceGlobalVarValueDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbTaskInstanceGlobalVarValueDTO.EsbStepInstanceGlobalVarValues;
import com.tencent.bk.job.execute.model.esb.v2.EsbTaskInstanceGlobalVarValueDTO.GlobalVarValue;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetJobInstanceGlobalVarValueRequest;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobInstanceGlobalVarValueV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbGetJobInstanceGlobalVarValueV3Request;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbGetJobInstanceGlobalVarValueResourceImpl
    extends JobQueryCommonProcessor implements EsbGetJobInstanceGlobalVarValueResource {

    private final MessageI18nService i18nService;
    private final EsbGetJobInstanceGlobalVarValueV3Resource proxyGetJobInstanceGlobalVarService;

    @Autowired
    public EsbGetJobInstanceGlobalVarValueResourceImpl(
        MessageI18nService i18nService,
        EsbGetJobInstanceGlobalVarValueV3Resource proxyGetJobInstanceGlobalVarService) {
        this.i18nService = i18nService;
        this.proxyGetJobInstanceGlobalVarService = proxyGetJobInstanceGlobalVarService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_get_job_instance_global_var_value"})
    public EsbResp<EsbTaskInstanceGlobalVarValueDTO> getJobInstanceGlobalVarValue(
        EsbGetJobInstanceGlobalVarValueRequest request) {

        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance global var value, request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        EsbGetJobInstanceGlobalVarValueV3Request newRequest =
            convertToEsbGetJobInstanceGlobalVarValueV3Request(request);
        EsbResp<EsbJobInstanceGlobalVarValueV3DTO> esbResp =
            proxyGetJobInstanceGlobalVarService.getJobInstanceGlobalVarValueUsingPost(newRequest);

        return EsbResp.convertData(esbResp, this::convertToEsbJobInstanceGlobalVarValueDTO);
    }

    private EsbGetJobInstanceGlobalVarValueV3Request convertToEsbGetJobInstanceGlobalVarValueV3Request
        (EsbGetJobInstanceGlobalVarValueRequest request) {
        EsbGetJobInstanceGlobalVarValueV3Request newRequest = new EsbGetJobInstanceGlobalVarValueV3Request();
        newRequest.setAppCode(request.getAppCode());
        newRequest.setUserName(request.getUserName());
        newRequest.setAppId(request.getAppId());
        newRequest.setTaskInstanceId(request.getTaskInstanceId());
        return newRequest;
    }

    private EsbTaskInstanceGlobalVarValueDTO convertToEsbJobInstanceGlobalVarValueDTO(
        EsbJobInstanceGlobalVarValueV3DTO originResult) {
        if (originResult == null) {
            return null;
        }

        EsbTaskInstanceGlobalVarValueDTO result = new EsbTaskInstanceGlobalVarValueDTO();
        result.setTaskInstanceId(originResult.getTaskInstanceId());

        if (CollectionUtils.isNotEmpty(originResult.getStepGlobalVarValues())) {
            List<EsbStepInstanceGlobalVarValues> globalVarValuesForSteps = new ArrayList<>();
            originResult.getStepGlobalVarValues().forEach(originStepGlobalVarValues -> {
                EsbStepInstanceGlobalVarValues stepGlobalVarValues =
                    new EsbStepInstanceGlobalVarValues();
                stepGlobalVarValues.setStepInstanceId(originStepGlobalVarValues.getStepInstanceId());
                if (CollectionUtils.isNotEmpty(originStepGlobalVarValues.getGlobalVarValues())) {
                    List<GlobalVarValue> globalVarValues = new ArrayList<>();
                    originStepGlobalVarValues.getGlobalVarValues().forEach(originGlobalVarValue -> {
                        GlobalVarValue globalVarValue = new GlobalVarValue();
                        globalVarValue.setCategory(originGlobalVarValue.getType());
                        globalVarValue.setName(originGlobalVarValue.getName());
                        globalVarValue.setValue(originGlobalVarValue.getValue());
                        globalVarValues.add(globalVarValue);
                    });
                    stepGlobalVarValues.setGlobalVarValues(globalVarValues);
                }
                globalVarValuesForSteps.add(stepGlobalVarValues);
            });
            result.setStepGlobalVarValues(globalVarValuesForSteps);
        }

        return result;
    }


    private ValidateResult checkRequest(EsbGetJobInstanceGlobalVarValueRequest request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("App is empty or illegal, appId={}", request.getAppId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME,
                "job_instance_id");
        }
        return ValidateResult.pass();
    }
}
