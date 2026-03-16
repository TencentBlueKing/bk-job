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

package com.tencent.bk.job.manage.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Tag(name = "job-manage:service:Metrics")
@SmartFeignClient(value = "job-manage", contextId = "manageMetricsResource")
@InternalAPI
public interface ServiceMetricsResource {

    @Operation(summary = "作业模板量", produces = "application/json")
    @GetMapping("/service/metrics/templates/count")
    InternalResponse<Integer> countTemplates(
        @Parameter(description = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId
    );

    @Operation(summary = "执行方案量", produces = "application/json")
    @GetMapping("/service/metrics/taskPlans/count")
    InternalResponse<Integer> countTaskPlans(
        @Parameter(description = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId
    );

    @Operation(summary = "作业模板的步骤量", produces = "application/json")
    @GetMapping("/service/metrics/templates/step/count")
    InternalResponse<Integer> countTemplateSteps(
        @Parameter(description = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId,
        @Parameter(description = "步骤类型")
        @RequestParam(value = "taskStepType", required = false)
            TaskStepTypeEnum taskStepType,
        @Parameter(description = "脚本来源")
        @RequestParam(value = "scriptSource", required = false)
            TaskScriptSourceEnum scriptSource,
        @Parameter(description = "文件类型")
        @RequestParam(value = "fileType", required = false)
            TaskFileTypeEnum fileType
    );

    @Operation(summary = "脚本总量", produces = "application/json")
    @GetMapping("/service/metrics/script/count")
    InternalResponse<Integer> countScripts(
        @Parameter(description = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId,
        @Parameter(description = "脚本类型")
        @RequestParam(value = "scriptType", required = false)
            ScriptTypeEnum scriptTypeEnum,
        @Parameter(description = "Job资源状态")
        @RequestParam(value = "jobResourceStatus", required = false)
            JobResourceStatusEnum jobResourceStatusEnum
    );

    @Operation(summary = "被引用的脚本总量", produces = "application/json")
    @GetMapping("/service/metrics/script/cited/count")
    InternalResponse<Integer> countCiteScripts(
        @Parameter(description = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId
    );

    @Operation(summary = "引用脚本的步骤总量", produces = "application/json")
    @GetMapping("/service/metrics/step/citeScript/count")
    InternalResponse<Integer> countCiteScriptSteps(
        @Parameter(description = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId
    );

    @Operation(summary = "脚本版本总量", produces = "application/json")
    @GetMapping("/service/metrics/scriptVersions/count")
    InternalResponse<Integer> countScriptVersions(
        @Parameter(description = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId,
        @Parameter(description = "脚本类型")
        @RequestParam(value = "scriptType", required = false)
            ScriptTypeEnum scriptTypeEnum,
        @Parameter(description = "Job资源状态")
        @RequestParam(value = "jobResourceStatus", required = false)
            JobResourceStatusEnum jobResourceStatusEnum
    );

    @Operation(summary = "账号总量", produces = "application/json")
    @GetMapping("/service/metrics/accounts/count")
    InternalResponse<Integer> countAccounts(
        @RequestHeader(value = JobCommonHeaders.BK_TENANT_ID, required = false)
        String tenantId,
        @Parameter(description = "账号类型")
        @RequestParam(value = "accountType", required = false)
            AccountTypeEnum accountType
    );

    @Operation(summary = "主机的操作系统类型分布数据", produces = "application/json")
    @GetMapping("/service/metrics/hosts/groupByOsType")
    InternalResponse<Map<String, Integer>> groupHostByOsType(
        @RequestHeader(value = JobCommonHeaders.BK_TENANT_ID, required = false)
        String tenantId
    );

    @Operation(summary = "某个标签在某业务下的被引数量", produces = "application/json")
    @GetMapping("/service/metrics/tags/citedCount")
    InternalResponse<Long> tagCitedCount(
        @Parameter(description = "业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId,
        @Parameter(description = "标签Id")
        @RequestParam(value = "tagId", required = false)
            Long tagId
    );

}
