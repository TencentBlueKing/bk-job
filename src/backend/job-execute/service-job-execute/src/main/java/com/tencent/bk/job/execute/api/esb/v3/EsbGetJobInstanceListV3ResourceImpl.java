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
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.constants.Consts;
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
    private final MessageI18nService i18nService;

    public EsbGetJobInstanceListV3ResourceImpl(MessageI18nService i18nService,
                                               TaskResultService taskResultService) {
        this.i18nService = i18nService;
        this.taskResultService = taskResultService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v3_get_job_instance_list"})
    public EsbResp<EsbPageDataV3<EsbTaskInstanceV3DTO>> getJobInstanceListUsingPost(
        EsbGetJobInstanceListV3Request request) {

        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job instance ip log request is illegal!");
            throw new InvalidParamException(checkResult);
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
                taskInstance.setAppId(taskInstanceDTO.getAppId());
                taskInstance.setId(taskInstanceDTO.getId());
                taskInstance.setCreateTime(taskInstanceDTO.getCreateTime());
                taskInstance.setName(taskInstanceDTO.getName());
                taskInstance.setEndTime(taskInstanceDTO.getEndTime());
                taskInstance.setStartTime(taskInstanceDTO.getStartTime());
                taskInstance.setStartupMode(taskInstanceDTO.getStartupMode());
                taskInstance.setTaskId(taskInstanceDTO.getTaskId());
                taskInstance.setOperator(taskInstanceDTO.getOperator());
                taskInstance.setStatus(taskInstanceDTO.getStatus());
                taskInstance.setTemplateId(taskInstanceDTO.getTaskTemplateId());
                taskInstance.setTotalTime(taskInstanceDTO.getTotalTime());
                taskInstance.setType(taskInstanceDTO.getType());
                return taskInstance;
            }).collect(Collectors.toList());
            pageDataV3.setData(taskInstanceList);
        }
        return pageDataV3;
    }

    private ValidateResult checkRequest(EsbGetJobInstanceListV3Request request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("App is empty or illegal, appId={}", request.getAppId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (request.getCreateTimeStart() == null || request.getCreateTimeStart() < 1) {
            log.warn("createTimeStart is empty or illegal, createTimeStart={}", request.getCreateTimeStart());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "create_time_start");
        }
        if (request.getCreateTimeEnd() == null || request.getCreateTimeEnd() < 1) {
            log.warn("createTimeEnd is empty or illegal, createTimeEnd={}", request.getCreateTimeEnd());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "create_time_end");
        }
        long period = request.getCreateTimeEnd() - request.getCreateTimeStart();
        if (period > Consts.MAX_SEARCH_TASK_HISTORY_RANGE_MILLS) {
            log.warn("Search time range greater than 30 days!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM, "create_time_start|create_time_end");
        }
        if (request.getTaskType() != null && TaskTypeEnum.valueOf(request.getTaskType()) == null) {
            log.warn("Param type is illegal!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM, "type");
        }
        return ValidateResult.pass();
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbTaskInstanceV3DTO>> getJobInstanceList(String username,
                                                                           String appCode,
                                                                           Long appId,
                                                                           Long createTimeStart,
                                                                           Long createTimeEnd,
                                                                           Long taskInstanceId,
                                                                           String operator,
                                                                           String taskName,
                                                                           Integer startupMode,
                                                                           Integer taskType,
                                                                           Integer taskStatus,
                                                                           String ip,
                                                                           Integer start,
                                                                           Integer length) {
        EsbGetJobInstanceListV3Request request = new EsbGetJobInstanceListV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setAppId(appId);
        request.setCreateTimeStart(createTimeStart);
        request.setCreateTimeEnd(createTimeEnd);
        request.setTaskInstanceId(taskInstanceId);
        request.setOperator(operator);
        request.setTaskName(taskName);
        request.setStartupMode(startupMode);
        request.setTaskType(taskType);
        request.setTaskStatus(taskStatus);
        request.setIp(ip);
        request.setStart(start);
        request.setLength(length);
        return getJobInstanceListUsingPost(request);
    }
}
