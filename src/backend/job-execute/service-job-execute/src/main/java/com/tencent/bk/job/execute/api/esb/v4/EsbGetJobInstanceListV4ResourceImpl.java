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

package com.tencent.bk.job.execute.api.esb.v4;

import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.SimplePaginationCondition;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4GetJobInstanceListResult;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobInstanceDTO;
import com.tencent.bk.job.execute.service.TaskResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbGetJobInstanceListV4ResourceImpl implements EsbGetJobInstanceListV4Resource {

    private final TaskResultService taskResultService;
    private final AppScopeMappingService appScopeMappingService;

    public EsbGetJobInstanceListV4ResourceImpl(TaskResultService taskResultService,
                                               AppScopeMappingService appScopeMappingService) {
        this.taskResultService = taskResultService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_get_job_instance_list"})
    public EsbV4Response<V4GetJobInstanceListResult> getJobInstanceList(String username,
                                                                        String appCode,
                                                                        String scopeType,
                                                                        String scopeId,
                                                                        Long createTimeStart,
                                                                        Long createTimeEnd,
                                                                        Long jobInstanceId,
                                                                        String operator,
                                                                        String taskName,
                                                                        Integer startupMode,
                                                                        Integer taskType,
                                                                        Integer taskStatus,
                                                                        String ip,
                                                                        Long cronId,
                                                                        Integer offset,
                                                                        Integer length) {
        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
        TaskInstanceQuery query = new TaskInstanceQuery();
        query.setAppId(appId);
        query.setTaskInstanceId(jobInstanceId);
        query.setOperator(operator);
        query.setTaskName(taskName);
        if (taskStatus != null) {
            query.setStatus(RunStatusEnum.valueOf(taskStatus));
        }
        if (startupMode != null) {
            query.setStartupModes(Collections.singletonList(TaskStartupModeEnum.getStartupMode(startupMode)));
        }
        if (taskType != null) {
            query.setTaskType(TaskTypeEnum.valueOf(taskType));
        }
        query.setIp(ip);
        query.setCronTaskId(cronId);
        query.setStartTime(createTimeStart);
        query.setEndTime(createTimeEnd);
        query.setIp(ip);

        SimplePaginationCondition condition = new SimplePaginationCondition();
        if (offset != null) {
            condition.setOffset(offset);
        }
        if (length != null) {
            condition.setLength(length);
        }

        List<TaskInstanceDTO> jobInstanceList = taskResultService.listJobInstance(query, condition);
        V4GetJobInstanceListResult result = new V4GetJobInstanceListResult();
        result.setList(jobInstanceList.stream().map(this::convertTaskInstanceToV4DTO).collect(Collectors.toList()));
        return EsbV4Response.success(result);
    }

    private V4JobInstanceDTO convertTaskInstanceToV4DTO(TaskInstanceDTO taskInstanceDTO) {
        V4JobInstanceDTO v4JobInstanceDTO = new V4JobInstanceDTO();
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(taskInstanceDTO.getAppId(), v4JobInstanceDTO);
        v4JobInstanceDTO.setId(taskInstanceDTO.getId());
        v4JobInstanceDTO.setName(taskInstanceDTO.getName());
        v4JobInstanceDTO.setTemplateId(taskInstanceDTO.getTaskTemplateId());
        v4JobInstanceDTO.setTaskId(taskInstanceDTO.getPlanId());
        v4JobInstanceDTO.setCreateTime(taskInstanceDTO.getCreateTime());
        v4JobInstanceDTO.setStartTime(taskInstanceDTO.getStartTime());
        v4JobInstanceDTO.setEndTime(taskInstanceDTO.getEndTime());
        v4JobInstanceDTO.setStartupMode(taskInstanceDTO.getStartupMode());
        v4JobInstanceDTO.setOperator(taskInstanceDTO.getOperator());
        v4JobInstanceDTO.setStatus(taskInstanceDTO.getStatus().getValue());
        v4JobInstanceDTO.setTotalTime(taskInstanceDTO.getTotalTime());
        v4JobInstanceDTO.setType(taskInstanceDTO.getType());

        return v4JobInstanceDTO;
    }

}
