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
import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-manage:service:Task_Plan_Management"})
@RequestMapping("/service")
@RestController
@InternalAPI
public interface ServiceTaskPlanResource {

    @ApiOperation(value = "根据执行方案ID获取执行方案信息", produces = "application/json")
    @GetMapping("/app/{appId}/plan/planId/{planId}/basic")
    InternalResponse<ServiceTaskPlanDTO> getPlanBasicInfoById(
        @ApiParam(value = "业务ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "执行方案ID", required = true) @PathVariable("planId") Long planId);

    @ApiOperation(value = "根据执行方案ID获取执行方案信息", produces = "application/json")
    @GetMapping("/plan/{planId}/planName")
    InternalResponse<String> getPlanName(
        @ApiParam(value = "执行方案ID", required = true) @PathVariable("planId") Long planId);

    @ApiOperation(value = "根据执行方案全局变量name获取id", produces = "application/json")
    @GetMapping("/plan/{planId}/globalVar/getIdByName/{globalVarName}")
    InternalResponse<Long> getGlobalVarIdByName(
        @ApiParam(value = "执行方案ID", required = true) @PathVariable("planId") Long planId,
        @ApiParam(value = "全局变量名称", required = true) @PathVariable("globalVarName") String globalVarName
    );

    @ApiOperation(value = "根据执行方案全局变量id获取name", produces = "application/json")
    @GetMapping("/plan/{planId}/globalVar/getNameById/{globalVarId}")
    InternalResponse<String> getGlobalVarNameById(
        @ApiParam(value = "执行方案ID", required = true) @PathVariable("planId") Long planId,
        @ApiParam(value = "全局变量ID", required = true) @PathVariable("globalVarId") Long globalVarId
    );

    @ApiOperation(value = "根据执行方案ID获取执行方案业务Id", produces = "application/json")
    @GetMapping("/plan/{planId}/planAppId")
    InternalResponse<Long> getPlanAppId(
        @ApiParam(value = "执行方案ID", required = true) @PathVariable("planId") Long planId);

    @ApiOperation(value = "根据执行方案ID获取执行方案信息", produces = "application/json")
    @GetMapping("/app/{appId}/plan/planId/{planId}")
    InternalResponse<ServiceTaskPlanDTO> getPlanById(
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "执行方案ID", required = true)
        @PathVariable("planId") Long planId,
        @ApiParam(value = "是否包含未启用的步骤")
        @RequestParam(value = "includeDisabledSteps", required = false, defaultValue = "false")
            Boolean includeDisabledSteps);

    @PutMapping("/app/{appId}/plan/{templateId}/{planId}/createPlanWithId")
    InternalResponse<Long> createPlanWithIdForMigration(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID") @PathVariable("templateId") Long templateId,
        @ApiParam(value = "执行方案 ID") @PathVariable("planId") Long planId,
        @ApiParam(value = "创建时间") @RequestHeader(value = "X-Create-Time", required = false) Long createTime,
        @ApiParam(value = "修改时间") @RequestHeader(value = "X-Update-Time", required = false) Long lastModifyTime,
        @ApiParam(value = "最后修改人") @RequestHeader(value = "X-Update-User", required = false) String lastModifyUser);

    @GetMapping("/app/{appId}/plan/check")
    InternalResponse<ServiceIdNameCheckDTO> checkIdAndName(
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID") @RequestParam("templateId") Long templateId,
        @ApiParam(value = "执行方案 ID") @RequestParam("planId") Long planId,
        @ApiParam(value = "执行方案名称", required = true) @RequestParam("planName") String name);

    @ApiOperation(value = "导入执行方案", produces = "application/json")
    @PutMapping("/app/{appId}/plan/{templateId}/savePlanForImport")
    InternalResponse<Long> savePlanForImport(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID") @PathVariable("templateId") Long templateId,
        @ApiParam(value = "创建时间") @RequestHeader(value = "X-Create-Time", required = false) Long createTime,
        @ApiParam(value = "执行方案信息", required = true) @RequestBody TaskPlanVO planInfo);

    @GetMapping("/app/{appId}/plan/{templateId}/{planId}/variable")
    InternalResponse<List<ServiceTaskVariableDTO>> getPlanVariable(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID") @PathVariable("templateId") Long templateId,
        @ApiParam(value = "执行方案 ID") @PathVariable("planId") Long planId);

    @ApiOperation(value = "获取执行方案基本信息列表", produces = "application/json")
    @GetMapping("/{templateId}")
    InternalResponse<List<ServiceTaskPlanDTO>> listPlans(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID", required = true) @PathVariable(value = "templateId") Long templateId);

    @ApiOperation(value = "获取模板对应的执行方案Id", produces = "application/json")
    @GetMapping("/plan/planIds/template/{templateId}")
    InternalResponse<List<Long>> listPlanIds(
        @ApiParam(value = "模版 ID", required = true) @PathVariable(value = "templateId") Long templateId);
}
