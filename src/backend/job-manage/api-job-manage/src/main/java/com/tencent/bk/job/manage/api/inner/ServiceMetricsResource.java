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

package com.tencent.bk.job.manage.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = {"job-manage:service:Metrics"})
@RequestMapping("/service/metrics")
@RestController
@InternalAPI
public interface ServiceMetricsResource {

    @ApiOperation(value = "接入业务总量", produces = "application/json")
    @GetMapping("/apps/count")
    InternalResponse<Integer> countApps(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @ApiOperation(value = "作业模板量", produces = "application/json")
    @GetMapping("/templates/count")
    InternalResponse<Integer> countTemplates(
        @ApiParam(value = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId
    );

    @ApiOperation(value = "执行方案量", produces = "application/json")
    @GetMapping("/taskPlans/count")
    InternalResponse<Integer> countTaskPlans(
        @ApiParam(value = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId
    );

    @ApiOperation(value = "作业模板的步骤量", produces = "application/json")
    @GetMapping("/templates/step/count")
    InternalResponse<Integer> countTemplateSteps(
        @ApiParam(value = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId,
        @ApiParam("步骤类型")
        @RequestParam(value = "taskStepType", required = false)
            TaskStepTypeEnum taskStepType,
        @ApiParam("脚本来源")
        @RequestParam(value = "scriptSource", required = false)
            TaskScriptSourceEnum scriptSource,
        @ApiParam("文件类型")
        @RequestParam(value = "fileType", required = false)
            TaskFileTypeEnum fileType
    );

    @ApiOperation(value = "脚本总量", produces = "application/json")
    @GetMapping("/script/count")
    InternalResponse<Integer> countScripts(
        @ApiParam(value = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId,
        @ApiParam("脚本类型")
        @RequestParam(value = "scriptType", required = false)
            ScriptTypeEnum scriptTypeEnum,
        @ApiParam("Job资源状态")
        @RequestParam(value = "jobResourceStatus", required = false)
            JobResourceStatusEnum jobResourceStatusEnum
    );

    @ApiOperation(value = "被引用的脚本总量", produces = "application/json")
    @GetMapping("/script/cited/count")
    InternalResponse<Integer> countCiteScripts(
        @ApiParam(value = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId
    );

    @ApiOperation(value = "引用脚本的步骤总量", produces = "application/json")
    @GetMapping("/step/citeScript/count")
    InternalResponse<Integer> countCiteScriptSteps(
        @ApiParam(value = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId
    );

    @ApiOperation(value = "脚本版本总量", produces = "application/json")
    @GetMapping("/scriptVersions/count")
    InternalResponse<Integer> countScriptVersions(
        @ApiParam(value = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId,
        @ApiParam("脚本类型")
        @RequestParam(value = "scriptType", required = false)
            ScriptTypeEnum scriptTypeEnum,
        @ApiParam("Job资源状态")
        @RequestParam(value = "jobResourceStatus", required = false)
            JobResourceStatusEnum jobResourceStatusEnum
    );

    @ApiOperation(value = "账号总量", produces = "application/json")
    @GetMapping("/accounts/count")
    InternalResponse<Integer> countAccounts(
        @ApiParam("账号类型")
        @RequestParam(value = "accountType", required = false)
            AccountTypeEnum accountType
    );

    @ApiOperation(value = "主机总量", produces = "application/json")
    @GetMapping("/hosts/count")
    InternalResponse<Long> countHostsByOsType(
        @ApiParam("系统类型")
        @RequestParam(value = "osType", required = false)
            String osType
    );

    @ApiOperation(value = "某个标签在某业务下的被引数量", produces = "application/json")
    @GetMapping("/tags/citedCount")
    InternalResponse<Long> tagCitedCount(
        @ApiParam(value = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId,
        @ApiParam(value = "标签Id")
        @RequestParam(value = "tagId", required = false)
            Long tagId
    );

}
