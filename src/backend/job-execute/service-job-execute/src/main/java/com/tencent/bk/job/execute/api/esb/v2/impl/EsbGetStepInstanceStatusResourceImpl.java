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
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.api.esb.v2.EsbGetStepInstanceStatusResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.GseTaskLogDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbStepInstanceStatusDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetStepInstanceStatusRequest;
import com.tencent.bk.job.execute.service.GseTaskLogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.StringUtils;
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
    private final GseTaskLogService gseTaskLogService;
    private final MessageI18nService i18nService;

    public EsbGetStepInstanceStatusResourceImpl(MessageI18nService i18nService, GseTaskLogService gseTaskLogService,
                                                TaskInstanceService taskInstanceService) {
        this.i18nService = i18nService;
        this.gseTaskLogService = gseTaskLogService;
        this.taskInstanceService = taskInstanceService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_get_step_instance_status"})
    public EsbResp<EsbStepInstanceStatusDTO> getJobStepInstanceStatus(EsbGetStepInstanceStatusRequest request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get step instance status request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        long stepInstanceId = request.getStepInstanceId();

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(request.getTaskInstanceId());
        authViewTaskInstance(request.getUserName(), request.getAppId(), taskInstance);

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        if (stepInstance == null) {
            log.warn("Get step instance status by taskInstanceId:{}, stepInstanceId:{}, stepInstance is null!",
                taskInstanceId, stepInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        EsbStepInstanceStatusDTO resultData = new EsbStepInstanceStatusDTO();
        GseTaskLogDTO gseTaskLog = gseTaskLogService.getGseTaskLog(stepInstanceId, stepInstance.getExecuteCount());
        if (null == gseTaskLog) {
            resultData.setIsFinished(false);
            return EsbResp.buildSuccessResp(null);
        } else {
            resultData.setIsFinished(!gseTaskLog.getStatus().equals(RunStatusEnum.BLANK.getValue())
                && !gseTaskLog.getStatus().equals(RunStatusEnum.RUNNING.getValue()));
            List<AgentTaskResultGroupDTO> analyseResult = gseTaskLogService.getLogStatInfoWithIp(stepInstance.getId()
                , stepInstance.getExecuteCount());
            resultData.setAyalyseResult(convertToStandardAnalyseResult(analyseResult));

            EsbStepInstanceStatusDTO.StepInstance stepDetail = convertStepInstance(stepInstance);
            resultData.setStepInstance(stepDetail);
            return EsbResp.buildSuccessResp(resultData);
        }
    }

    private EsbStepInstanceStatusDTO.StepInstance convertStepInstance(StepInstanceBaseDTO stepInstance) {
        EsbStepInstanceStatusDTO.StepInstance stepInst = new EsbStepInstanceStatusDTO.StepInstance();
        stepInst.setAppId(stepInstance.getAppId());
        stepInst.setId(stepInstance.getId());
        stepInst.setBadIpList(stepInstance.getBadIpList());
        stepInst.setBadIPNum(StringUtils.isEmpty(stepInstance.getBadIpList()) ? 0 :
            stepInstance.getBadIpList().split(",").length);
        stepInst.setEndTime(stepInstance.getEndTime());
        stepInst.setStartTime(stepInstance.getStartTime());
        stepInst.setFailIPNum(stepInstance.getFailIPNum());
        stepInst.setIpList(stepInstance.getIpList());
        stepInst.setName(stepInstance.getName());
        stepInst.setOperator(stepInstance.getOperator());
        stepInst.setExecuteCount(stepInstance.getExecuteCount());
        stepInst.setRunIPNum(stepInstance.getRunIPNum());
        stepInst.setStatus(stepInstance.getStatus());
        stepInst.setStepId(stepInstance.getStepId());
        stepInst.setSuccessIPNum(stepInstance.getSuccessIPNum());
        stepInst.setTaskInstanceId(stepInstance.getTaskInstanceId());
        stepInst.setTotalIPNum(stepInstance.getTotalIPNum());
        stepInst.setTotalTime(stepInstance.getTotalTime());
        stepInst.setType(stepInstance.getExecuteType());

        return stepInst;
    }


    private ValidateResult checkRequest(EsbGetStepInstanceStatusRequest request) {
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() < 1) {
            log.warn("TaskInstanceId is empty or illegal, taskInstanceId={}", request.getTaskInstanceId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
        }
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("App is empty or illegal, appId={}", request.getAppId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
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
            standardStepAnalyseResult.put("count", resultGroup.getCount());
            if (resultGroup.getIpList() != null) {
                List<EsbIpDTO> ipDTOS = new ArrayList<>();
                for (IpDTO ipDTO : resultGroup.getIpList()) {
                    ipDTOS.add(new EsbIpDTO(ipDTO.getCloudAreaId(), ipDTO.getIp()));
                }
                standardStepAnalyseResult.put("ip_list", ipDTOS);
            }

            standardStepAnalyseResult.put("result_type", resultGroup.getResultType().getValue());
            standardStepAnalyseResult.put("result_type_text",
                i18nService.getI18n(resultGroup.getResultType().getI18nKey()));

            standardStepAnalyseResultList.add(standardStepAnalyseResult);
        }
        return standardStepAnalyseResultList;
    }

}
