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

package com.tencent.bk.job.execute.api.inner;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import com.tencent.bk.job.execute.model.converter.TaskInstanceConverter;
import com.tencent.bk.job.execute.model.inner.ServiceCronTaskExecuteResultStatistics;
import com.tencent.bk.job.execute.model.inner.ServiceTaskInstanceDTO;
import com.tencent.bk.job.execute.model.inner.request.ServiceGetCronTaskExecuteStatisticsRequest;
import com.tencent.bk.job.execute.service.TaskResultService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class ServiceTaskExecuteResultResourceImpl implements ServiceTaskExecuteResultResource {
    private final TaskResultService taskResultService;
    private final MessageI18nService i18nService;

    public ServiceTaskExecuteResultResourceImpl(TaskResultService taskResultService, MessageI18nService i18nService) {
        this.taskResultService = taskResultService;
        this.i18nService = i18nService;
    }

    @Override
    public InternalResponse<Map<Long, ServiceCronTaskExecuteResultStatistics>>
    getCronTaskExecuteResultStatistics(ServiceGetCronTaskExecuteStatisticsRequest request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("Illegal param appId:{}", request.getAppId());
            throw new InvalidParamException(ErrorCode.MISSING_OR_ILLEGAL_PARAM);
        }
        if (request.getCronTaskIdList() == null || request.getCronTaskIdList().isEmpty()) {
            log.warn("Param:cronTaskIdList is empty");
            throw new InvalidParamException(ErrorCode.MISSING_OR_ILLEGAL_PARAM);
        }
        Map<Long, ServiceCronTaskExecuteResultStatistics> statisticsMap =
            taskResultService.getCronTaskExecuteResultStatistics(request.getAppId(), request.getCronTaskIdList());
        return InternalResponse.buildSuccessResp(statisticsMap);
    }

    @Override
    public InternalResponse<PageData<ServiceTaskInstanceDTO>> getTaskExecuteResult(Long appId, String taskName,
                                                                              Long taskInstanceId, Integer status
        , String operator, Integer startupMode, Integer taskType, String startTime,
                                                                              String endTime, Integer start,
                                                                              Integer pageSize, Long cronTaskId) {
        TaskInstanceQuery taskQuery = new TaskInstanceQuery();
        taskQuery.setTaskInstanceId(taskInstanceId);
        taskQuery.setAppId(appId);
        taskQuery.setTaskName(taskName);
        taskQuery.setCronTaskId(cronTaskId);
        if (StringUtils.isNotBlank(startTime)) {
            taskQuery.setStartTime(DateUtils.convertUnixTimestampFromDateTimeStr(startTime, "yyyy-MM-dd HH:mm:ss",
                ChronoUnit.MILLIS, ZoneId.of("UTC")));
        }
        if (StringUtils.isNotBlank(endTime)) {
            taskQuery.setEndTime(DateUtils.convertUnixTimestampFromDateTimeStr(endTime, "yyyy-MM-dd HH:mm:ss",
                ChronoUnit.MILLIS, ZoneId.of("UTC")));
        }
        taskQuery.setOperator(operator);
        if (startupMode != null) {
            taskQuery.setStartupModes(Collections.singletonList(TaskStartupModeEnum.getStartupMode(startupMode)));
        }
        if (taskType != null) {
            taskQuery.setTaskType(TaskTypeEnum.valueOf(taskType));
        }
        if (status != null) {
            taskQuery.setStatus(RunStatusEnum.valueOf(status));
        }
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);

        PageData<TaskInstanceDTO> pageData = taskResultService.listPageTaskInstance(taskQuery, baseSearchCondition);

        PageData<ServiceTaskInstanceDTO> pageDataVO = new PageData<>();
        pageDataVO.setTotal(pageData.getTotal());
        pageDataVO.setStart(pageData.getStart());
        pageDataVO.setPageSize(pageData.getPageSize());

        List<ServiceTaskInstanceDTO> serviceTaskInstanceDTOS = new ArrayList<>();
        if (pageData.getData() != null) {
            pageData.getData().forEach(taskInstanceDTO -> serviceTaskInstanceDTOS
                .add(TaskInstanceConverter.convertToServiceTaskInstanceDTO(taskInstanceDTO, i18nService)));
        }
        pageDataVO.setData(serviceTaskInstanceDTOS);
        return InternalResponse.buildSuccessResp(pageDataVO);
    }
}
