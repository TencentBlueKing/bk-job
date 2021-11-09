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
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.execute.api.esb.v2.impl.JobQueryCommonProcessor;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.GseTaskIpLogDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbJobInstanceStatusV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbGetJobInstanceStatusV3Request;
import com.tencent.bk.job.execute.service.GseTaskLogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbGetJobInstanceStatusV3ResourceImpl
    extends JobQueryCommonProcessor
    implements EsbGetJobInstanceStatusV3Resource {

    private final TaskInstanceService taskInstanceService;
    private final GseTaskLogService gseTaskLogService;
    private final MessageI18nService i18nService;

    public EsbGetJobInstanceStatusV3ResourceImpl(MessageI18nService i18nService, GseTaskLogService gseTaskLogService,
                                                 TaskInstanceService taskInstanceService) {
        this.i18nService = i18nService;
        this.gseTaskLogService = gseTaskLogService;
        this.taskInstanceService = taskInstanceService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v3_get_job_instance_status"})
    public EsbResp<EsbJobInstanceStatusV3DTO> getJobInstanceStatusUsingPost(EsbGetJobInstanceStatusV3Request request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance status request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(request.getTaskInstanceId());

        authViewTaskInstance(request.getUserName(), request.getAppId(), taskInstance);


        List<StepInstanceBaseDTO> stepInstances = taskInstanceService.listStepInstanceByTaskInstanceId(taskInstanceId);
        if (stepInstances == null || stepInstances.isEmpty()) {
            log.warn("Get job instance status by taskInstanceId:{}, stepInstanceList is empty!", taskInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        boolean isReturnIpResult = request.getReturnIpResult() != null && request.getReturnIpResult();
        EsbJobInstanceStatusV3DTO jobInstanceStatus = buildEsbJobInstanceStatusDTO(taskInstance, stepInstances,
            isReturnIpResult);

        return EsbResp.buildSuccessResp(jobInstanceStatus);
    }

    private ValidateResult checkRequest(EsbGetJobInstanceStatusV3Request request) {
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

    private EsbJobInstanceStatusV3DTO buildEsbJobInstanceStatusDTO(TaskInstanceDTO taskInstance,
                                                                   List<StepInstanceBaseDTO> stepInstances,
                                                                   boolean isReturnIpResult) {
        EsbJobInstanceStatusV3DTO jobInstanceStatus = new EsbJobInstanceStatusV3DTO();
        jobInstanceStatus.setFinished(!taskInstance.getStatus().equals(RunStatusEnum.BLANK.getValue())
            && !taskInstance.getStatus().equals(RunStatusEnum.RUNNING.getValue()));

        EsbJobInstanceStatusV3DTO.JobInstance jobInstance = new EsbJobInstanceStatusV3DTO.JobInstance();
        jobInstance.setAppId(taskInstance.getAppId());
        jobInstance.setId(taskInstance.getId());
        jobInstance.setName(taskInstance.getName());
        jobInstance.setCreateTime(taskInstance.getCreateTime());
        jobInstance.setStartTime(taskInstance.getStartTime());
        jobInstance.setEndTime(taskInstance.getEndTime());
        jobInstance.setStatus(taskInstance.getStatus());
        jobInstance.setTotalTime(taskInstance.getTotalTime());
        jobInstanceStatus.setJobInstance(jobInstance);

        List<EsbJobInstanceStatusV3DTO.StepInst> stepInsts = new ArrayList<>(stepInstances.size());
        for (StepInstanceBaseDTO stepInstance : stepInstances) {
            EsbJobInstanceStatusV3DTO.StepInst stepInst = new EsbJobInstanceStatusV3DTO.StepInst();
            stepInst.setId(stepInstance.getId());
            stepInst.setName(stepInstance.getName());
            stepInst.setCreateTime(stepInstance.getCreateTime());
            stepInst.setEndTime(stepInstance.getEndTime());
            stepInst.setStartTime(stepInstance.getStartTime());
            stepInst.setType(stepInstance.getExecuteType());
            stepInst.setExecuteCount(stepInstance.getExecuteCount());
            stepInst.setStatus(stepInstance.getStatus());
            stepInst.setTotalTime(stepInstance.getTotalTime());

            if (isReturnIpResult) {
                List<EsbJobInstanceStatusV3DTO.IpResult> stepIpResults = new ArrayList<>();
                List<GseTaskIpLogDTO> ipLogList = gseTaskLogService.getIpLog(stepInstance.getId(),
                    stepInstance.getExecuteCount(), true);
                if (CollectionUtils.isNotEmpty(ipLogList)) {
                    for (GseTaskIpLogDTO ipLog : ipLogList) {
                        EsbJobInstanceStatusV3DTO.IpResult stepIpResult = new EsbJobInstanceStatusV3DTO.IpResult();
                        stepIpResult.setCloudAreaId(ipLog.getCloudAreaId());
                        stepIpResult.setIp(ipLog.getIp());
                        stepIpResult.setExitCode(ipLog.getExitCode());
                        stepIpResult.setErrorCode(ipLog.getErrCode());
                        stepIpResult.setStartTime(ipLog.getStartTime());
                        stepIpResult.setEndTime(ipLog.getEndTime());
                        stepIpResult.setTotalTime(ipLog.getTotalTime());
                        stepIpResult.setTag(ipLog.getTag());
                        stepIpResult.setStatus(ipLog.getStatus());
                        stepIpResults.add(stepIpResult);
                    }
                }
                stepInst.setStepIpResult(stepIpResults);
            }
            stepInsts.add(stepInst);
        }
        jobInstanceStatus.setStepInstances(stepInsts);

        return jobInstanceStatus;
    }

    @Override
    public EsbResp<EsbJobInstanceStatusV3DTO> getJobInstanceStatus(String username,
                                                                   String appCode,
                                                                   Long appId,
                                                                   Long taskInstanceId,
                                                                   boolean returnIpResult) {
        EsbGetJobInstanceStatusV3Request request = new EsbGetJobInstanceStatusV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setAppId(appId);
        request.setTaskInstanceId(taskInstanceId);
        request.setReturnIpResult(returnIpResult);
        return getJobInstanceStatusUsingPost(request);
    }
}
