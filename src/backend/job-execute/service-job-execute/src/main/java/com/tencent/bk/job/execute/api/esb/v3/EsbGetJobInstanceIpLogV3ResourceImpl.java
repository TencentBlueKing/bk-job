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
import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.model.ScriptIpLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbIpLogV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbGetJobInstanceIpLogV3Request;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class EsbGetJobInstanceIpLogV3ResourceImpl extends JobQueryCommonV3Processor implements EsbGetJobInstanceIpLogV3Resource {

    private final TaskInstanceService taskInstanceService;
    private final LogService logService;
    private final MessageI18nService i18nService;

    public EsbGetJobInstanceIpLogV3ResourceImpl(MessageI18nService i18nService,
                                                LogService logService,
                                                TaskInstanceService taskInstanceService) {
        this.i18nService = i18nService;
        this.logService = logService;
        this.taskInstanceService = taskInstanceService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v3_get_job_instance_ip_log"})
    public EsbResp<EsbIpLogV3DTO> getJobInstanceIpLog(String lang, EsbGetJobInstanceIpLogV3Request request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance ip log request is illegal!");
            return EsbResp.buildCommonFailResp(i18nService, checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        EsbResp authResult = authViewTaskInstance(request.getUserName(), request.getAppId(), taskInstance);
        if (!authResult.getCode().equals(EsbResp.SUCCESS_CODE)) {
            return authResult;
        }

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(request.getStepInstanceId());
        if (stepInstance == null) {
            return EsbResp.buildCommonFailResp(ErrorCode.TASK_INSTANCE_NOT_EXIST, i18nService);
        }

        EsbIpLogV3DTO ipLog = new EsbIpLogV3DTO();
        ipLog.setCloudAreaId(request.getCloudAreaId());
        ipLog.setIp(request.getIp());
        if (stepInstance.isScriptStep()) {
            ScriptIpLogContent logContent = logService.getScriptIpLogContent(request.getStepInstanceId(),
                stepInstance.getExecuteCount(),
                new IpDTO(request.getCloudAreaId(), request.getIp()));
            if (logContent != null && StringUtils.isNotBlank(logContent.getContent())) {
                ipLog.setLogContent(logContent.getContent());
            }
        } else if (stepInstance.isFileStep()) {
            // TODO
        }
        return EsbResp.buildSuccessResp(ipLog);
    }

    private ValidateResult checkRequest(EsbGetJobInstanceIpLogV3Request request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("App is empty or illegal, appId={}", request.getAppId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
        }
        if (request.getStepInstanceId() == null || request.getStepInstanceId() < 1) {
            log.warn("StepInstanceId is empty or illegal, stepInstanceId={}", request.getStepInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "step_instance_id");
        }
        if (request.getCloudAreaId() == null || request.getCloudAreaId() < 0) {
            log.warn("CloudAreaId is empty or illegal, cloudAreaId={}", request.getCloudAreaId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_cloud_id");
        }
        if (StringUtils.isBlank(request.getIp())) {
            log.warn("Ip is empty");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "ip");
        }
        if (!IpUtils.checkIp(request.getIp())) {
            log.warn("Ip is illegal, ip={}", request.getIp());
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "ip");
        }

        return ValidateResult.pass();
    }
}
