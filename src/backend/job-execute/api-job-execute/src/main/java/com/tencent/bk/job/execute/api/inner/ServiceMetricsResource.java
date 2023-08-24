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

import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.execute.model.inner.request.ServiceTriggerStatisticsRequest;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Api(tags = {"job-execute:service:Metrics"})
@SmartFeignClient(value = "job-execute", contextId = "executeMetricsResource")
@InternalAPI
public interface ServiceMetricsResource {

    @ApiOperation(value = "接入（执行过一次任务）的业务Id列表", produces = "application/json")
    @GetMapping("/app/joined")
    InternalResponse<List<Long>> getJoinedAppIdList();

    @ApiOperation(value = "是否有执行记录", produces = "application/json")
    @GetMapping("/service/metrics/app/hasExecuteHistory")
    InternalResponse<Boolean> hasExecuteHistory(
        @ApiParam(value = "业务Id", required = false)
        @RequestParam(value = "appId", required = false) Long appId,
        @ApiParam(value = "定时任务Id", required = false)
        @RequestParam(value = "cronTaskId", required = false) Long cronTaskId,
        @ApiParam(value = "统计的起始时间", required = false)
        @RequestParam(value = "fromTime", required = false) Long fromTime,
        @ApiParam(value = "统计的截止时间", required = false)
        @RequestParam(value = "toTime", required = false) Long toTime
    );

    @ApiOperation(value = "获取统计数据", produces = "application/json")
    @GetMapping("/service/metrics/statistics")
    InternalResponse<StatisticsDTO> getStatistics(
        @ApiParam(value = "业务Id", required = true)
        @RequestParam(value = "appId", required = true) Long appId,
        @ApiParam(value = "资源类型", required = true)
        @RequestParam(value = "resource", required = true) String resource,
        @ApiParam(value = "资源维度", required = true)
        @RequestParam(value = "dimension", required = true) String dimension,
        @ApiParam(value = "资源维度取值", required = true)
        @RequestParam(value = "dimensionValue", required = true) String dimensionValue,
        @ApiParam(value = "统计日期(yyyy-MM-dd)", required = true)
        @RequestParam(value = "dateStr", required = true) String dateStr
    );

    @ApiOperation(value = "触发指定时间的数据统计", produces = "application/json")
    @PostMapping("/service/metrics/statistics/trigger")
    InternalResponse<Boolean> triggerStatistics(
        @ApiParam(value = "统计日期(yyyy-MM-dd)", required = false)
        @RequestBody ServiceTriggerStatisticsRequest request
    );
}
