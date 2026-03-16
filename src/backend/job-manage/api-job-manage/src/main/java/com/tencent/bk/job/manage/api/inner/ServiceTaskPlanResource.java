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
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "job-manage:service:Task_Plan_Management")
@SmartFeignClient(value = "job-manage", contextId = "taskPlanResource")
@InternalAPI
public interface ServiceTaskPlanResource {

    @Operation(summary = "根据执行方案ID获取执行方案信息")
    @GetMapping("/service/app/{appId}/plan/planId/{planId}/basic")
    InternalResponse<ServiceTaskPlanDTO> getPlanBasicInfoById(
        @Parameter(description = "业务ID", required = true) @PathVariable("appId") Long appId,
        @Parameter(description = "执行方案ID", required = true) @PathVariable("planId") Long planId);

    @Operation(summary = "根据执行方案ID获取执行方案信息")
    @GetMapping("/service/plan/{planId}/planName")
    InternalResponse<String> getPlanName(
        @Parameter(description = "执行方案ID", required = true) @PathVariable("planId") Long planId);

    @Operation(summary = "根据执行方案全局变量name获取id")
    @GetMapping("/service/plan/{planId}/globalVar/getIdByName/{globalVarName}")
    InternalResponse<Long> getGlobalVarIdByName(
        @Parameter(description = "执行方案ID", required = true) @PathVariable("planId") Long planId,
        @Parameter(description = "全局变量名称", required = true) @PathVariable("globalVarName") String globalVarName
    );

    @Operation(summary = "根据执行方案全局变量name获取实例")
    @GetMapping("/service/plan/{planId}/globalVar/name/{globalVarName}")
    InternalResponse<ServiceTaskVariableDTO> getGlobalVarByName(
        @Parameter(description = "执行方案ID", required = true) @PathVariable("planId") Long planId,
        @Parameter(description = "全局变量名称", required = true) @PathVariable("globalVarName") String globalVarName
    );

    @Operation(summary = "根据执行方案全局变量id获取name")
    @GetMapping("/service/plan/{planId}/globalVar/getNameById/{globalVarId}")
    InternalResponse<String> getGlobalVarNameById(
        @Parameter(description = "执行方案ID", required = true) @PathVariable("planId") Long planId,
        @Parameter(description = "全局变量ID", required = true) @PathVariable("globalVarId") Long globalVarId
    );

    @Operation(summary = "根据执行方案全局变量id获取实例")
    @GetMapping("/service/plan/{planId}/globalVar/id/{globalVarId}")
    InternalResponse<ServiceTaskVariableDTO> getGlobalVarById(
        @Parameter(description = "执行方案ID", required = true) @PathVariable("planId") Long planId,
        @Parameter(description = "全局变量ID", required = true) @PathVariable("globalVarId") Long globalVarId
    );

    @Operation(summary = "根据执行方案ID获取执行方案业务Id")
    @GetMapping("/service/plan/{planId}/planAppId")
    InternalResponse<Long> getPlanAppId(
        @Parameter(description = "执行方案ID", required = true) @PathVariable("planId") Long planId);

    @Operation(summary = "根据执行方案ID获取执行方案信息")
    @GetMapping("/service/app/{appId}/plan/planId/{planId}")
    InternalResponse<ServiceTaskPlanDTO> getPlanById(
        @Parameter(description = "业务ID", required = true)
        @PathVariable("appId") Long appId,
        @Parameter(description = "执行方案ID", required = true)
        @PathVariable("planId") Long planId,
        @Parameter(description = "是否包含未启用的步骤")
        @RequestParam(value = "includeDisabledSteps", required = false, defaultValue = "false")
            Boolean includeDisabledSteps);

    @GetMapping("/service/app/{appId}/plan/check")
    InternalResponse<ServiceIdNameCheckDTO> checkIdAndName(
        @Parameter(description = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @Parameter(description = "模版 ID") @RequestParam("templateId") Long templateId,
        @Parameter(description = "执行方案 ID") @RequestParam("planId") Long planId,
        @Parameter(description = "执行方案名称", required = true) @RequestParam("planName") String name);

    @Operation(summary = "导入执行方案")
    @PutMapping("/service/app/{appId}/plan/{templateId}/savePlanForImport")
    InternalResponse<Long> savePlanForImport(
        @Parameter(description = "用户名，网关自动传入") @RequestHeader("username") String username,
        @Parameter(description = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @Parameter(description = "模版 ID") @PathVariable("templateId") Long templateId,
        @Parameter(description = "创建时间") @RequestHeader(value = "X-Create-Time", required = false) Long createTime,
        @Parameter(description = "执行方案信息", required = true) @RequestBody TaskPlanVO planInfo);

    @GetMapping("/service/app/{appId}/plan/{templateId}/{planId}/variable")
    InternalResponse<List<ServiceTaskVariableDTO>> getPlanVariable(
        @Parameter(description = "用户名，网关自动传入") @RequestHeader("username") String username,
        @Parameter(description = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @Parameter(description = "模版 ID") @PathVariable("templateId") Long templateId,
        @Parameter(description = "执行方案 ID") @PathVariable("planId") Long planId);

    @Operation(summary = "获取执行方案基本信息列表")
    @GetMapping("/service/{templateId}")
    InternalResponse<List<ServiceTaskPlanDTO>> listPlans(
        @Parameter(description = "用户名，网关自动传入") @RequestHeader("username") String username,
        @Parameter(description = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @Parameter(description = "模版 ID", required = true) @PathVariable(value = "templateId") Long templateId);

    @Operation(summary = "获取模板对应的执行方案Id")
    @GetMapping("/service/plan/planIds/template/{templateId}")
    InternalResponse<List<Long>> listPlanIds(
        @Parameter(description = "模版 ID", required = true) @PathVariable(value = "templateId") Long templateId);
}
