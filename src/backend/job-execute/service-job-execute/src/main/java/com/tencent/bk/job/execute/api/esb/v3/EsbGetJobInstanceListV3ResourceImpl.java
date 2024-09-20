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
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.validation.ValidationUtil;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import com.tencent.bk.job.execute.model.esb.v3.EsbTaskInstanceV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbGetJobInstanceListV3Request;
import com.tencent.bk.job.execute.service.TaskResultService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbGetJobInstanceListV3ResourceImpl implements EsbGetJobInstanceListV3Resource {

    private final TaskResultService taskResultService;
    private final AppScopeMappingService appScopeMappingService;

    public EsbGetJobInstanceListV3ResourceImpl(TaskResultService taskResultService,
                                    AppScopeMappingService appScopeMappingService) {
        this.taskResultService = taskResultService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_job_instance_list"})
    public EsbResp<EsbPageDataV3<EsbTaskInstanceV3DTO>> getJobInstanceListUsingPost(
        String username,
        String appCode,
        EsbGetJobInstanceListV3Request request) {
        if (!ValidationUtil.validateSearchTimeRange(request.getCreateTimeStart(), request.getCreateTimeEnd())) {
            log.warn("create_time_start|create_time_end is illegal!");
            throw new InvalidParamException(
                ValidateResult.fail(ErrorCode.ILLEGAL_PARAM, "create_time_start|create_time_end")
            );
        }
        TaskInstanceQuery taskQuery = new TaskInstanceQuery();
        taskQuery.setTaskInstanceId(request.getTaskInstanceId());
        taskQuery.setAppId(request.getAppId());
        taskQuery.setTaskName(request.getTaskName());
        taskQuery.setCronTaskId(request.getCronId());
        if (request.getCreateTimeStart() != null) {
            taskQuery.setStartTime(request.getCreateTimeStart());
        }
        if (request.getCreateTimeEnd() != null) {
            taskQuery.setEndTime(request.getCreateTimeEnd());
        }

        taskQuery.setOperator(request.getOperator());
        if (request.getStartupMode() != null) {
            taskQuery.setStartupModes(Collections.singletonList(
                TaskStartupModeEnum.getStartupMode(request.getStartupMode())));
        }
        if (request.getTaskType() != null) {
            taskQuery.setTaskType(TaskTypeEnum.valueOf(request.getTaskType()));
        }
        if (request.getTaskStatus() != null) {
            taskQuery.setStatus(RunStatusEnum.valueOf(request.getTaskStatus()));
        }
        taskQuery.setIp(request.getIp());
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(request.getStart());
        baseSearchCondition.setLength(request.getLength());

        PageData<TaskInstanceDTO> pageData = taskResultService.listPageTaskInstance(taskQuery, baseSearchCondition);
        return EsbResp.buildSuccessResp(convertToEsbTaskInstancePageData(pageData));
    }

    private EsbPageDataV3<EsbTaskInstanceV3DTO> convertToEsbTaskInstancePageData(PageData<TaskInstanceDTO> pageData) {
        EsbPageDataV3<EsbTaskInstanceV3DTO> pageDataV3 = new EsbPageDataV3<>();
        pageDataV3.setStart(pageData.getStart());
        pageDataV3.setLength(pageData.getPageSize());
        pageDataV3.setTotal(pageData.getTotal());
        if (CollectionUtils.isNotEmpty(pageData.getData())) {
            List<EsbTaskInstanceV3DTO> taskInstanceList = pageData.getData().stream().map(taskInstanceDTO -> {
                EsbTaskInstanceV3DTO taskInstance = new EsbTaskInstanceV3DTO();
                EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(taskInstanceDTO.getAppId(), taskInstance);
                taskInstance.setId(taskInstanceDTO.getId());
                taskInstance.setCreateTime(taskInstanceDTO.getCreateTime());
                taskInstance.setName(taskInstanceDTO.getName());
                taskInstance.setEndTime(taskInstanceDTO.getEndTime());
                taskInstance.setStartTime(taskInstanceDTO.getStartTime());
                taskInstance.setStartupMode(taskInstanceDTO.getStartupMode());
                taskInstance.setTaskId(taskInstanceDTO.getPlanId());
                taskInstance.setOperator(taskInstanceDTO.getOperator());
                taskInstance.setStatus(taskInstanceDTO.getStatus().getValue());
                taskInstance.setTemplateId(taskInstanceDTO.getTaskTemplateId());
                taskInstance.setTotalTime(taskInstanceDTO.getTotalTime());
                taskInstance.setType(taskInstanceDTO.getType());
                return taskInstance;
            }).collect(Collectors.toList());
            pageDataV3.setData(taskInstanceList);
        }
        return pageDataV3;
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbTaskInstanceV3DTO>> getJobInstanceList(String username,
                                                                           String appCode,
                                                                           Long bizId,
                                                                           String scopeType,
                                                                           String scopeId,
                                                                           Long createTimeStart,
                                                                           Long createTimeEnd,
                                                                           Long taskInstanceId,
                                                                           String operator,
                                                                           String taskName,
                                                                           Integer startupMode,
                                                                           Integer taskType,
                                                                           Integer taskStatus,
                                                                           String ip,
                                                                           Long cronId,
                                                                           Integer start,
                                                                           Integer length) {
        EsbGetJobInstanceListV3Request request = new EsbGetJobInstanceListV3Request();
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setCreateTimeStart(createTimeStart);
        request.setCreateTimeEnd(createTimeEnd);
        request.setTaskInstanceId(taskInstanceId);
        request.setOperator(operator);
        request.setTaskName(taskName);
        request.setStartupMode(startupMode);
        request.setTaskType(taskType);
        request.setTaskStatus(taskStatus);
        request.setIp(ip);
        request.setCronId(cronId);
        request.setStart(start);
        request.setLength(length);
        request.fillAppResourceScope(appScopeMappingService);
        return getJobInstanceListUsingPost(username, appCode, request);
    }
}
