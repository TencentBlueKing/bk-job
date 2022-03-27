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
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.api.esb.v2.EsbGetStepInstanceStatusResource;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.StepExecutionDetailDTO;
import com.tencent.bk.job.execute.model.StepExecutionResultQuery;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbStepInstanceStatusDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetStepInstanceStatusRequest;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskResultService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class EsbGetStepInstanceStatusResourceImpl
    extends JobQueryCommonProcessor
    implements EsbGetStepInstanceStatusResource {
    private final TaskInstanceService taskInstanceService;
    private final TaskResultService taskResultService;
    private final MessageI18nService i18nService;
    private final AppScopeMappingService appScopeMappingService;

    public EsbGetStepInstanceStatusResourceImpl(MessageI18nService i18nService,
                                                TaskInstanceService taskInstanceService,
                                                AppScopeMappingService appScopeMappingService,
                                                TaskResultService taskResultService) {
        this.i18nService = i18nService;
        this.taskInstanceService = taskInstanceService;
        this.appScopeMappingService = appScopeMappingService;
        this.taskResultService = taskResultService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_step_instance_status"})
    public EsbResp<EsbStepInstanceStatusDTO> getJobStepInstanceStatus(EsbGetStepInstanceStatusRequest request) {
        request.fillAppResourceScope(appScopeMappingService);

        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get step instance status request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        EsbStepInstanceStatusDTO resultData = new EsbStepInstanceStatusDTO();

        StepExecutionResultQuery query = StepExecutionResultQuery.builder()
            .stepInstanceId(request.getStepInstanceId()).build();
        StepExecutionDetailDTO stepExecutionDetail = taskResultService.getStepExecutionResult(request.getUserName(),
            request.getAppId(), query);

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(request.getTaskInstanceId());
        authViewTaskInstance(request.getUserName(), request.getAppResourceScope(), taskInstance);
        resultData.setIsFinished(stepExecutionDetail.isFinished());
        resultData.setAyalyseResult(convertToStandardAnalyseResult(stepExecutionDetail.getResultGroups()));

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(request.getStepInstanceId());
        if (stepInstance == null) {
            log.warn("Get step instance status by taskInstanceId:{}, stepInstanceId:{}, stepInstance is null!",
                request.getTaskInstanceId(), request.getStepInstanceId());
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        EsbStepInstanceStatusDTO.StepInstance stepDetail = convertStepInstance(stepInstance);
        resultData.setStepInstance(stepDetail);
        return EsbResp.buildSuccessResp(resultData);
    }

    private EsbStepInstanceStatusDTO.StepInstance convertStepInstance(StepInstanceBaseDTO stepInstance) {
        EsbStepInstanceStatusDTO.StepInstance stepInst = new EsbStepInstanceStatusDTO.StepInstance();
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(stepInstance.getAppId(), stepInst);
        stepInst.setId(stepInstance.getId());
        stepInst.setEndTime(stepInstance.getEndTime());
        stepInst.setStartTime(stepInstance.getStartTime());
        stepInst.setIpList(stepInstance.getIpList());
        stepInst.setName(stepInstance.getName());
        stepInst.setOperator(stepInstance.getOperator());
        stepInst.setExecuteCount(stepInstance.getExecuteCount());
        stepInst.setStatus(stepInstance.getStatus());
        stepInst.setStepId(stepInstance.getStepId());
        stepInst.setTaskInstanceId(stepInstance.getTaskInstanceId());
        stepInst.setTotalTime(stepInstance.getTotalTime());
        stepInst.setType(stepInstance.getExecuteType());

        return stepInst;
    }


    private ValidateResult checkRequest(EsbGetStepInstanceStatusRequest request) {
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
        }
        if (request.getStepInstanceId() == null || request.getStepInstanceId() < 1) {
            log.warn("StepInstanceId is empty or illegal, stepInstanceId={}", request.getStepInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "step_instance_id");
        }
        return ValidateResult.pass();
    }

    private List<Map<String, Object>> convertToStandardAnalyseResult(List<AgentTaskResultGroupDTO> resultGroups) {
        List<Map<String, Object>> standardStepAnalyseResultList = new ArrayList<>();
        if (resultGroups == null || resultGroups.isEmpty()) {
            return standardStepAnalyseResultList;
        }
        for (AgentTaskResultGroupDTO resultGroup : resultGroups) {
            Map<String, Object> standardStepAnalyseResult = new HashMap<>();
            List<AgentTaskDTO> agentTasks = resultGroup.getAgentTasks();
            standardStepAnalyseResult.put("count", CollectionUtils.isEmpty(agentTasks) ? 0 : agentTasks.size());
            if (CollectionUtils.isNotEmpty(agentTasks)) {
                List<EsbIpDTO> ips = new ArrayList<>();
                for (AgentTaskDTO agentTask : agentTasks) {
                    ips.add(new EsbIpDTO(agentTask.getCloudId(), agentTask.getIp()));
                }
                standardStepAnalyseResult.put("ip_list", ips);

            }

            standardStepAnalyseResult.put("result_type", resultGroup.getStatus());
            standardStepAnalyseResult.put("result_type_text",
                i18nService.getI18n(IpStatus.valueOf(resultGroup.getStatus()).getI18nKey()));

            standardStepAnalyseResultList.add(standardStepAnalyseResult);
        }
        return standardStepAnalyseResultList;
    }

}
