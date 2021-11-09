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
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.constants.TaskOperationEnum;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobExecuteV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbOperateJobInstanceV3Request;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class EsbOperateJobInstanceV3ResourceImpl implements EsbOperateJobInstanceV3Resource {
    private final TaskExecuteService taskExecuteService;

    private final MessageI18nService i18nService;

    private final AuthService authService;

    public EsbOperateJobInstanceV3ResourceImpl(TaskExecuteService taskExecuteService, MessageI18nService i18nService,
                                               AuthService authService) {
        this.taskExecuteService = taskExecuteService;
        this.i18nService = i18nService;
        this.authService = authService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v3_operate_job_instance"})
    public EsbResp<EsbJobExecuteV3DTO> operateJobInstance(EsbOperateJobInstanceV3Request request) {
        log.info("Operate task instance, request={}", JsonUtils.toJson(request));
        if (!checkRequest(request)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        TaskOperationEnum taskOperation = TaskOperationEnum.getTaskOperation(request.getOperationCode());
        taskExecuteService.doTaskOperation(request.getAppId(), request.getUserName(),
            request.getTaskInstanceId(), taskOperation);
        EsbJobExecuteV3DTO result = new EsbJobExecuteV3DTO();
        result.setTaskInstanceId(request.getTaskInstanceId());
        return EsbResp.buildSuccessResp(result);
    }

    private boolean checkRequest(EsbOperateJobInstanceV3Request request) {
        if (request.getAppId() == null || request.getAppId() <= 0) {
            log.warn("Operate task instance, appId is empty!");
            return false;
        }
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() <= 0) {
            log.warn("Operate task instance, taskInstanceId is empty!");
            return false;
        }
        if (request.getOperationCode() == null) {
            log.warn("Operate task instance, operation code is empty!");
            return false;
        }
        TaskOperationEnum operation = TaskOperationEnum.getTaskOperation(request.getOperationCode());
        if (operation == null) {
            log.warn("Operate task instance, operation-code:{} is invalid!", request.getOperationCode());
            return false;
        }
        return true;
    }
}
