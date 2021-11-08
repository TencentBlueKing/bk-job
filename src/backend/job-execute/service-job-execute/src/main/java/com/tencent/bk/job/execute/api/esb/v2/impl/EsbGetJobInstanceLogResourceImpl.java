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

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.execute.api.esb.v2.EsbGetJobInstanceLogResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.GseTaskIpLogDTO;
import com.tencent.bk.job.execute.model.GseTaskLogDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbStepInstanceResultAndLog;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetJobInstanceLogRequest;
import com.tencent.bk.job.execute.service.GseTaskLogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class EsbGetJobInstanceLogResourceImpl extends JobQueryCommonProcessor implements EsbGetJobInstanceLogResource {

    private final TaskInstanceService taskInstanceService;
    private final GseTaskLogService gseTaskLogService;
    private final MessageI18nService i18nService;

    public EsbGetJobInstanceLogResourceImpl(MessageI18nService i18nService, GseTaskLogService gseTaskLogService,
                                            TaskInstanceService taskInstanceService) {
        this.i18nService = i18nService;
        this.gseTaskLogService = gseTaskLogService;
        this.taskInstanceService = taskInstanceService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_get_job_instance_log"})
    public EsbResp<List<EsbStepInstanceResultAndLog>> getJobInstanceLogUsingPost(EsbGetJobInstanceLogRequest request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance log request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long taskInstanceId = request.getTaskInstanceId();
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        authViewTaskInstance(request.getUserName(), request.getAppId(), taskInstance);

        List<StepInstanceBaseDTO> stepInstanceList =
            taskInstanceService.listStepInstanceByTaskInstanceId(taskInstanceId);
        List<EsbStepInstanceResultAndLog> stepInstResultAndLogList = Lists.newArrayList();
        for (StepInstanceBaseDTO stepInstance : stepInstanceList) {
            GseTaskLogDTO gseTaskLog = gseTaskLogService.getGseTaskLog(stepInstance.getId(),
                stepInstance.getExecuteCount());
            if (null == gseTaskLog) {
                EsbStepInstanceResultAndLog stepInstResultAndLog = new EsbStepInstanceResultAndLog();
                stepInstResultAndLog.setFinished(false);
                stepInstResultAndLog.setStepInstanceId(stepInstance.getId());
                stepInstResultAndLog.setName(stepInstance.getName());
                stepInstResultAndLog.setStatus(stepInstance.getStatus());
                stepInstResultAndLog.setStepResults(Lists.newArrayList());
                stepInstResultAndLogList.add(stepInstResultAndLog);
                continue;
            }

            EsbStepInstanceResultAndLog stepInstResultAndLog = new EsbStepInstanceResultAndLog();
            stepInstResultAndLog.setFinished(!gseTaskLog.getStatus().equals(RunStatusEnum.BLANK.getValue())
                && !gseTaskLog.getStatus().equals(RunStatusEnum.RUNNING.getValue()));
            List<AgentTaskResultGroupDTO> resultGroups = gseTaskLogService.getIpLogStatInfo(stepInstance.getId(),
                stepInstance.getExecuteCount());
            List<EsbStepInstanceResultAndLog.StepInstResultDTO> stepInstResultList =
                Lists.newArrayListWithCapacity(resultGroups.size());
            for (AgentTaskResultGroupDTO resultGroup : resultGroups) {
                EsbStepInstanceResultAndLog.StepInstResultDTO stepInstResult =
                    new EsbStepInstanceResultAndLog.StepInstResultDTO();
                stepInstResult.setIpStatus(resultGroup.getResultType().getValue());
                stepInstResult.setTag(resultGroup.getTag());
                List<GseTaskIpLogDTO> ipLogList = gseTaskLogService.getIpLogContentByResultType(stepInstance.getId(),
                    stepInstance.getExecuteCount(), resultGroup.getResultType().getValue(), resultGroup.getTag());
                List<EsbStepInstanceResultAndLog.IpLogDTO> resultIpLogList =
                    Lists.newArrayListWithCapacity(ipLogList.size());
                for (GseTaskIpLogDTO taskIpLog : ipLogList) {
                    EsbStepInstanceResultAndLog.IpLogDTO ipLogDTO = new EsbStepInstanceResultAndLog.IpLogDTO();
                    ipLogDTO.setLogContent(Utils.htmlEncode(taskIpLog.getLogContent()));
                    ipLogDTO.setExecuteCount(taskIpLog.getExecuteCount());
                    ipLogDTO.setEndTime(taskIpLog.getEndTime());
                    ipLogDTO.setStartTime(taskIpLog.getStartTime());
                    ipLogDTO.setErrCode(taskIpLog.getErrCode());
                    ipLogDTO.setExitCode(taskIpLog.getExitCode());
                    ipLogDTO.setTotalTime(taskIpLog.getTotalTime());
                    ipLogDTO.setCloudAreaId(taskIpLog.getCloudAreaId());
                    ipLogDTO.setIp(taskIpLog.getIp());
                    resultIpLogList.add(ipLogDTO);
                }
                stepInstResult.setIpLogs(resultIpLogList);
                stepInstResultList.add(stepInstResult);
            }

            stepInstResultAndLog.setStepInstanceId(stepInstance.getId());
            stepInstResultAndLog.setName(stepInstance.getName());
            stepInstResultAndLog.setStatus(stepInstance.getStatus());
            stepInstResultAndLog.setStepResults(stepInstResultList);
            stepInstResultAndLogList.add(stepInstResultAndLog);
        }
        return EsbResp.buildSuccessResp(stepInstResultAndLogList);
    }

    private ValidateResult checkRequest(EsbGetJobInstanceLogRequest request) {
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
    public EsbResp<List<EsbStepInstanceResultAndLog>> getJobInstanceLog(String appCode, String username,
                                                                        Long appId, Long taskInstanceId) {
        EsbGetJobInstanceLogRequest req = new EsbGetJobInstanceLogRequest();
        req.setAppCode(appCode);
        req.setUserName(username);
        req.setAppId(appId);
        req.setTaskInstanceId(taskInstanceId);
        return getJobInstanceLogUsingPost(req);
    }
}
