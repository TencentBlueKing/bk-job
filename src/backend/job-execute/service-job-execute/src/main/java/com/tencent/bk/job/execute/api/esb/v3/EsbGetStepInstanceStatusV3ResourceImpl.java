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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.StepExecutionDetailDTO;
import com.tencent.bk.job.execute.model.StepExecutionResultQuery;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbStepInstanceStatusV3DTO;
import com.tencent.bk.job.execute.service.StepInstanceValidateService;
import com.tencent.bk.job.execute.service.TaskResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbGetStepInstanceStatusV3ResourceImpl implements EsbGetStepInstanceStatusV3Resource {

    private final StepInstanceValidateService stepInstanceValidateService;
    private final AppScopeMappingService appScopeMappingService;
    private final TaskResultService taskResultService;
    private final MessageI18nService messageI18nService;

    public EsbGetStepInstanceStatusV3ResourceImpl(StepInstanceValidateService stepInstanceValidateService,
                                                  AppScopeMappingService appScopeMappingService,
                                                  TaskResultService taskResultService,
                                                  MessageI18nService messageI18nService) {
        this.stepInstanceValidateService = stepInstanceValidateService;
        this.appScopeMappingService = appScopeMappingService;
        this.taskResultService = taskResultService;
        this.messageI18nService = messageI18nService;
    }

    private EsbStepInstanceStatusV3DTO buildEsbStepInstanceStatusV3DTO(StepExecutionDetailDTO executionResult) {
        StepInstanceBaseDTO stepInstance = executionResult.getStepInstance();
        EsbStepInstanceStatusV3DTO stepInst = new EsbStepInstanceStatusV3DTO();
        stepInst.setId(stepInstance.getId());
        stepInst.setExecuteCount(stepInstance.getExecuteCount());
        stepInst.setName(stepInstance.getName());
        stepInst.setType(stepInstance.getExecuteType().getValue());
        stepInst.setStatus(stepInstance.getStatus().getValue());
        stepInst.setCreateTime(stepInstance.getCreateTime());
        stepInst.setStartTime(executionResult.getStartTime());
        stepInst.setEndTime(executionResult.getEndTime());
        stepInst.setTotalTime(executionResult.getTotalTime());

        List<EsbStepInstanceStatusV3DTO.StepResultGroup> stepResultGroupList = new ArrayList<>();

        List<ResultGroupDTO> resultGroups = executionResult.getResultGroups();
        for (ResultGroupDTO resultGroup : resultGroups) {
            List<ExecuteObjectTask> executeObjectTasks = resultGroup.getExecuteObjectTasks();

            EsbStepInstanceStatusV3DTO.StepResultGroup stepResultGroup =
                new EsbStepInstanceStatusV3DTO.StepResultGroup();
            stepResultGroup.setResultType(resultGroup.getStatus());
            ExecuteObjectTaskStatusEnum taskStatusEnum = ExecuteObjectTaskStatusEnum.valOf(resultGroup.getStatus());
            if (taskStatusEnum != null) {
                stepResultGroup.setResultTypeDesc(messageI18nService.getI18n(taskStatusEnum.getI18nKey()));
            }
            stepResultGroup.setTag(resultGroup.getTag());
            stepResultGroup.setHostSize(resultGroup.getTotal());
            List<EsbStepInstanceStatusV3DTO.HostResult> hostResults = new ArrayList<>();

            if (executeObjectTasks != null) {
                for (ExecuteObjectTask executeObjectTask : executeObjectTasks) {
                    EsbStepInstanceStatusV3DTO.HostResult stepHostResult = new EsbStepInstanceStatusV3DTO.HostResult();
                    HostDTO host = executeObjectTask.getExecuteObject().getHost();
                    stepHostResult.setHostId(host.getHostId());
                    stepHostResult.setIp(host.getIp());
                    stepHostResult.setIpv6(host.getIpv6());
                    stepHostResult.setCloudAreaId(host.getBkCloudId());
                    stepHostResult.setAgentId(host.getAgentId());
                    stepHostResult.setCloudAreaName(host.getBkCloudName());
                    stepHostResult.setStatus(executeObjectTask.getStatus().getValue());
                    stepHostResult.setStatusDesc(messageI18nService.getI18n(executeObjectTask.getStatus().getI18nKey()));
                    stepHostResult.setTag(executeObjectTask.getTag());
                    stepHostResult.setExitCode(executeObjectTask.getExitCode());
                    stepHostResult.setStartTime(executeObjectTask.getStartTime());
                    stepHostResult.setEndTime(executeObjectTask.getEndTime());
                    stepHostResult.setTotalTime(executeObjectTask.getTotalTime());
                    hostResults.add(stepHostResult);
                }
                stepResultGroup.setHostResultList(hostResults);
            }
            stepResultGroupList.add(stepResultGroup);
        }
        stepInst.setStepResultGroupList(stepResultGroupList);
        return stepInst;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_step_instance_status"})
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public EsbResp<EsbStepInstanceStatusV3DTO> getStepInstanceStatus(String username,
                                                                     String appCode,
                                                                     String scopeType,
                                                                     String scopeId,
                                                                     Long taskInstanceId,
                                                                     Long stepInstanceId,
                                                                     Integer executeCount,
                                                                     Integer batch,
                                                                     Integer maxHostNumPerGroup,
                                                                     String keyword,
                                                                     String searchIp,
                                                                     Integer status,
                                                                     String tag) {
        long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);

        ValidateResult checkResult = stepInstanceValidateService.checkStepInstance(
            appId,
            taskInstanceId,
            stepInstanceId
        );
        if (!checkResult.isPass()) {
            log.warn("Get step instance status request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        StepExecutionResultQuery query = StepExecutionResultQuery.builder()
            .stepInstanceId(stepInstanceId)
            .executeCount(executeCount)
            .batch(batch == null ? null : (batch == 0 ? null : batch))
            .filterByLatestBatch(batch == null)
            .status(status)
            .tag(tag)
            .logKeyword(keyword)
            .searchIp(searchIp)
            .maxTasksForResultGroup(maxHostNumPerGroup)
            .fetchAllGroupData(status == null)
            .build();

        StepExecutionDetailDTO executionResult = taskResultService.getStepExecutionResult(username, appId, query);
        if (executionResult == null) {
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        EsbStepInstanceStatusV3DTO jobInstanceStatus = buildEsbStepInstanceStatusV3DTO(executionResult);

        return EsbResp.buildSuccessResp(jobInstanceStatus);
    }
}
