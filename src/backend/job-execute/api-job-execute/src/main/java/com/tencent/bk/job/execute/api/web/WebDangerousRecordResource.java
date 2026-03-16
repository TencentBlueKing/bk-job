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

package com.tencent.bk.job.execute.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.execute.model.web.vo.DangerousRecordVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 高危检测记录API-前端调用
 */
@Tag(name = "job-execute:web:Dangerous_Record")
@RequestMapping("/web/dangerous-record")
@RestController
@WebAPI
public interface WebDangerousRecordResource {
    @Operation(summary = "分页获取高危检测记录")
    @GetMapping("/list")
    Response<PageData<DangerousRecordVO>> pageListDangerousRecords(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(name = "id", value = "记录ID")
        @RequestParam(value = "id", required = false)
            Long id,
        @Parameter(description = "资源范围类型")
        @RequestParam(value = "scopeType", required = false)
            String scopeType,
        @Parameter(description = "资源范围ID")
        @RequestParam(value = "scopeId", required = false)
            String scopeId,
        @Parameter(description = "规则ID", name = "ruleId")
        @RequestParam(value = "ruleId", required = false)
            Long ruleId,
        @Parameter(description = "规则表达式", name = "ruleExpression")
        @RequestParam(value = "ruleExpression", required = false)
            String ruleExpression,
        @Parameter(description = "时区, 配合startTime和endTime使用", name = "timezone")
        @RequestParam(value = "timezone", required = false)
            String timezone,
        @Parameter(description = "时间范围-开始时间", name = "startTime", required = true)
        @RequestParam(value = "startTime")
            String startTime,
        @Parameter(description = "时间范围-结束时间", name = "endTime", required = true)
        @RequestParam(value = "endTime")
            String endTime,
        @Parameter(description = "分页-开始", required = true)
        @RequestParam(value = "start")
            Integer start,
        @Parameter(description = "分页-每页大小", required = true)
        @RequestParam(value = "pageSize")
            Integer pageSize,
        @Parameter(description = "执行方式,1-页面执行,2-API调用,3-定时任务", name = "startupMode")
        @RequestParam(value = "startupMode", required = false)
            Integer startupMode,
        @Parameter(description = "模式，1:扫描,2:拦截", name = "action")
        @RequestParam(value = "action", required = false)
            Integer action,
        @Parameter(description = "执行人", name = "operator")
        @RequestParam(value = "operator", required = false)
            String operator,
        @Parameter(description = "调用方", name = "client")
        @RequestParam(value = "client", required = false)
            String client
    );

}
