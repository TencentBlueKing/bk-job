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

package com.tencent.bk.job.manage.api.op;

import com.tencent.bk.job.common.model.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-manage:api:CronJobCustomNotifyPolicy-OP"})
@RequestMapping("/op/CronJobCustomNotifyPolicy")
@RestController
public interface CronJobCustomNotifyPolicyOpResource {

    @ApiOperation(value = "批量同步自定义定时任务级别的通知配置", produces = "application/json")
    @PostMapping("/sync")
    Response<List<Long>> batchSyncCronJobCustomNotifyPolicy(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiParam("定时任务ID列表")
        @RequestBody
        List<Long> cronTaskIdList
    );

    @ApiOperation(value = "批量删除自定义定时任务级别的通知配置", produces = "application/json")
    @DeleteMapping("/batchDelete/appId/{appId}")
    Response<List<Long>> batchDelete(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiParam(value = "业务ID", required = true)
        @PathVariable(value = "appId")
        Long scopeId,
        @ApiParam("定时任务ID列表")
        @RequestBody
        List<Long> cronTaskIdList
    );

}
