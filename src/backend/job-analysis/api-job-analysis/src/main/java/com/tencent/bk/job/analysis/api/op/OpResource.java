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

package com.tencent.bk.job.analysis.api.op;

import com.tencent.bk.job.analysis.model.op.CancelTasksReq;
import com.tencent.bk.job.analysis.model.op.ClearStatisticsReq;
import com.tencent.bk.job.analysis.model.op.ConfigStatisticsReq;
import com.tencent.bk.job.analysis.model.op.ConfigThreadsReq;
import com.tencent.bk.job.analysis.model.op.StartTasksReq;
import com.tencent.bk.job.common.model.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-analysis:api:OP"})
@RequestMapping("/op")
@RestController
public interface OpResource {

    @ApiOperation(value = "查询统计配置", produces = "application/json")
    @GetMapping("/config")
    Response<String> getStatisticsConfig(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username
    );

    @ApiOperation(value = "更新统计配置", produces = "application/json")
    @PostMapping("/config")
    Response<Boolean> configStatistics(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "统计配置参数", required = true) @RequestBody ConfigStatisticsReq req
    );

    @ApiOperation(value = "清理某些日期的统计数据", produces = "application/json")
    @PostMapping("/clear")
    Response<Integer> clearStatistics(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "清理参数", required = true) @RequestBody ClearStatisticsReq req
    );

    @ApiOperation(value = "调整统计线程数量", produces = "application/json")
    @PostMapping("/config/threads")
    Response<Boolean> configThreads(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "线程数量参数", required = true) @RequestBody ConfigThreadsReq req
    );

    @ApiOperation(value = "开始某些统计任务", produces = "application/json")
    @PostMapping("/start")
    Response<List<String>> startTasks(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "开始任务参数", required = true) @RequestBody StartTasksReq req
    );

    @ApiOperation(value = "取消全部统计任务", produces = "application/json")
    @PostMapping("/cancelAll")
    Response<List<String>> cancelAllTasks(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username
    );

    @ApiOperation(value = "取消某些统计任务", produces = "application/json")
    @PostMapping("/cancel")
    Response<List<String>> cancelTasks(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "取消任务参数", required = true) @RequestBody CancelTasksReq req
    );

    @ApiOperation(value = "查询所有统计任务", produces = "application/json")
    @PostMapping("/taskList")
    Response<List<String>> taskList(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username
    );

    @ApiOperation(value = "查询当前正在跑的统计任务", produces = "application/json")
    @PostMapping("/taskList/arranged")
    Response<List<Pair<String, Integer>>> arrangedTaskList(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username
    );
}
