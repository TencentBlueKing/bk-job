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

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.inner.request.ServiceTriggerStatisticsRequest;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.statistics.StatisticsService;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tencent.bk.job.execute.monitor.ExecuteMetricTags.BOOLEAN_TRUE_TAG_VALUE;
import static com.tencent.bk.job.execute.monitor.ExecuteMetricTags.IGNORE_TAG;

@Slf4j
@RestController
public class ServiceMetricsResourceImpl implements ServiceMetricsResource {
    private final TaskInstanceService taskInstanceService;
    private final StatisticsService statisticsService;

    public ServiceMetricsResourceImpl(TaskInstanceService taskInstanceService, StatisticsService statisticsService) {
        this.taskInstanceService = taskInstanceService;
        this.statisticsService = statisticsService;
    }

    @Override
    @Timed(extraTags = {IGNORE_TAG, BOOLEAN_TRUE_TAG_VALUE})
    public InternalResponse<List<Long>> getJoinedAppIdList() {
        return InternalResponse.buildSuccessResp(taskInstanceService.getJoinedAppIdList());
    }

    @Override
    @Timed(extraTags = {IGNORE_TAG, BOOLEAN_TRUE_TAG_VALUE})
    public InternalResponse<Boolean> hasExecuteHistory(Long appId, Long cronTaskId, Long fromTime, Long toTime) {
        return InternalResponse.buildSuccessResp(taskInstanceService.hasExecuteHistory(appId, cronTaskId, fromTime,
            toTime));
    }

    @Override
    @Timed(extraTags = {IGNORE_TAG, BOOLEAN_TRUE_TAG_VALUE})
    public InternalResponse<Integer> countFastPushFile(Long appId, Integer transferMode, Boolean localUpload,
                                                  RunStatusEnum runStatus, Long fromTime, Long toTime) {
        return InternalResponse.buildSuccessResp(taskInstanceService.countFastPushFile(appId, transferMode,
            localUpload, runStatus, fromTime, toTime));
    }

    @Override
    @Timed(extraTags = {IGNORE_TAG, BOOLEAN_TRUE_TAG_VALUE})
    public InternalResponse<Integer> countStepInstances(Long appId, List<Long> stepIdList,
                                                   StepExecuteTypeEnum stepExecuteType, Integer scriptType,
                                                   RunStatusEnum runStatus, Long fromTime, Long toTime) {
        return InternalResponse.buildSuccessResp(taskInstanceService.countStepInstances(appId, stepIdList,
            stepExecuteType, ScriptTypeEnum.valueOf(scriptType), runStatus, fromTime, toTime));
    }

    @Override
    @Timed(extraTags = {IGNORE_TAG, BOOLEAN_TRUE_TAG_VALUE})
    public InternalResponse<Integer> countTaskInstances(Long appId, Long minTotalTime, Long maxTotalTime,
                                                   TaskStartupModeEnum taskStartupMode, TaskTypeEnum taskType,
                                                   List<Byte> runStatusList, Long fromTime, Long toTime) {
        return InternalResponse.buildSuccessResp(taskInstanceService.countTaskInstances(appId, minTotalTime,
            maxTotalTime, taskStartupMode, taskType, runStatusList, fromTime, toTime));
    }

    @Override
    public InternalResponse<StatisticsDTO> getStatistics(Long appId, String resource, String dimension,
                                                    String dimensionValue, String dateStr) {
        return InternalResponse.buildSuccessResp(statisticsService.getStatistics(appId, resource, dimension,
            dimensionValue, dateStr));
    }

    @Override
    public InternalResponse<List<StatisticsDTO>> listStatistics(Long appId, String resource, String dimension,
                                                           String dimensionValue, String dateStr) {
        return InternalResponse.buildSuccessResp(statisticsService.listStatistics(appId, resource, dimension,
            dimensionValue, dateStr));
    }

    @Override
    public InternalResponse<Boolean> triggerStatistics(ServiceTriggerStatisticsRequest request) {
        return InternalResponse.buildSuccessResp(statisticsService.triggerStatistics(request.getDateList()));
    }
}
