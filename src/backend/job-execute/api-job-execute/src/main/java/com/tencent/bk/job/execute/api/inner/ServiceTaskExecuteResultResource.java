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

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.execute.model.inner.ServiceCronTaskExecuteResultStatistics;
import com.tencent.bk.job.execute.model.inner.ServiceTaskInstanceDTO;
import com.tencent.bk.job.execute.model.inner.request.ServiceGetCronTaskExecuteStatisticsRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 作业执行结果API-服务内部调用
 */
@Api(tags = {"job-execute:service:Task_Execution_Result"})
@RequestMapping("/service/execution")
@RestController
@InternalAPI
public interface ServiceTaskExecuteResultResource {
    /**
     * @param request 定时任务结果统计请求
     * @return Map<定时任务ID, 统计信息>
     */
    @ApiOperation(value = "获取定时作业执行结果统计", produces = "application/json")
    @PostMapping("/task-execution-history/execute-result-statistics/cron")
    InternalResponse<Map<Long, ServiceCronTaskExecuteResultStatistics>> getCronTaskExecuteResultStatistics(
        @ApiParam("获取定时作业执行结果统计") @RequestBody ServiceGetCronTaskExecuteStatisticsRequest request);

    @ApiOperation(value = "获取作业执行历史列表", produces = "application/json")
    @GetMapping("/app/{appId}/task-execution-history/list")
    InternalResponse<PageData<ServiceTaskInstanceDTO>> getTaskExecuteResult(
        @ApiParam(value = "业务ID", required = true, example = "1") @PathVariable("appId") Long appId,
        @ApiParam(value = "任务名称", name = "taskName", required = false) @RequestParam(value = "taskName",
            required = false) String taskName,
        @ApiParam(value = "任务ID", name = "taskInstanceId", required = false) @RequestParam(value = "taskInstanceId",
            required = false) Long taskInstanceId,
        @ApiParam(value = "任务状态", name = "status", required = false) @RequestParam(value = "status",
            required = false) Integer status,
        @ApiParam(value = "执行人", name = "operator", required = false) @RequestParam(value = "operator",
            required = false) String operator,
        @ApiParam(value = "执行方式", name = "startupMode", required = false) @RequestParam(value = "startupMode",
            required = false) Integer startupMode,
        @ApiParam(value = "任务类型", name = "taskType", required = false) @RequestParam(value = "taskType",
            required = false) Integer taskType,
        @ApiParam(value = "开始时间", name = "startTime", required = false) @RequestParam(value = "startTime",
            required = false) String startTime,
        @ApiParam(value = "结束时间", name = "endTime", required = false) @RequestParam(value = "endTime",
            required = false) String endTime,
        @ApiParam(value = "分页-开始", required = false) @RequestParam(value = "start", required = false) Integer start,
        @ApiParam(value = "分页-每页大小", required = false) @RequestParam(value = "pageSize",
            required = false) Integer pageSize,
        @ApiParam(value = "定时任务ID", required = false) @RequestParam(value = "cronTaskId",
            required = false) Long cronTaskId);
}
