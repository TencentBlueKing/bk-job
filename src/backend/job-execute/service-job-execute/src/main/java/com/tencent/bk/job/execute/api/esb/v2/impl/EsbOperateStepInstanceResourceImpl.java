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
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.api.esb.v2.EsbOperateStepInstanceResource;
import com.tencent.bk.job.execute.constants.StepOperationEnum;
import com.tencent.bk.job.execute.model.StepOperationDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbJobExecuteDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbOperateStepInstanceRequest;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class EsbOperateStepInstanceResourceImpl implements EsbOperateStepInstanceResource {
    private final TaskExecuteService taskExecuteService;

    public EsbOperateStepInstanceResourceImpl(TaskExecuteService taskExecuteService) {
        this.taskExecuteService = taskExecuteService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_operate_step_instance"})
    public EsbResp<EsbJobExecuteDTO> operateStepInstance(String username,
                                                         String appCode,
                                                         EsbOperateStepInstanceRequest request) {
        log.info("Operate step instance, request={}", JsonUtils.toJson(request));
        if (!checkRequest(request)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        StepOperationEnum operationEnum = StepOperationEnum.getStepOperation(request.getOperationCode());
        StepOperationDTO stepOperation = new StepOperationDTO();
        stepOperation.setStepInstanceId(request.getStepInstanceId());
        stepOperation.setOperation(operationEnum);
        taskExecuteService.doStepOperation(request.getAppId(), username, stepOperation);
        EsbJobExecuteDTO result = new EsbJobExecuteDTO();
        result.setTaskInstanceId(request.getTaskInstanceId());
        result.setStepInstanceId(request.getStepInstanceId());
        return EsbResp.buildSuccessResp(result);
    }

    private boolean checkRequest(EsbOperateStepInstanceRequest request) {
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() <= 0) {
            log.warn("Operate step instance, taskInstanceId is empty!");
            return false;
        }
        if (request.getStepInstanceId() == null || request.getStepInstanceId() <= 0) {
            log.warn("Operate step instance, stepInstanceId is empty!");
            return false;
        }
        if (request.getOperationCode() == null) {
            log.warn("Operate step instance, operation code is empty!");
            return false;
        }
        StepOperationEnum operation = StepOperationEnum.getStepOperation(request.getOperationCode());
        if (operation == null) {
            log.warn("Operate step instance, operation-code:{} is invalid!", request.getOperationCode());
            return false;
        }
        return true;
    }
}
