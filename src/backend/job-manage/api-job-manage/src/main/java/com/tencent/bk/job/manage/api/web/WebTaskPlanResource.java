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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.DeprecatedAppLogic;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.model.web.request.TaskPlanCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TaskVariableValueUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanSyncInfoVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.QueryParam;
import java.util.List;

@Api(tags = {"job-manage:web:Task_Plan_Management"})
@RequestMapping("/web")
@RestController
@WebAPI
public interface WebTaskPlanResource {

    @ApiOperation(value = "获取业务下的执行方案列表", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task/plan/list", "/scope/{scopeType}/{scopeId}/task/plan/list"})
    Response<PageData<TaskPlanVO>> listAllPlans(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @DeprecatedAppLogic
        @ApiParam(value = "业务 ID", required = false)
        @PathVariable(value = "appId", required = false)
            Long appId,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "执行方案 ID")
        @RequestParam(value = "planId", required = false)
            Long planId,
        @ApiParam(value = "模板名称")
        @RequestParam(value = "templateName", required = false)
            String templateName,
        @ApiParam(value = "模板 ID")
        @RequestParam(value = "templateId", required = false)
            Long templateId,
        @ApiParam(value = "执行方案名称")
        @RequestParam(value = "planName", required = false)
            String planName,
        @ApiParam(value = "创建人")
        @RequestParam(value = "creator", required = false)
            String creator,
        @ApiParam(value = "更新人")
        @RequestParam(value = "lastModifyUser", required = false)
            String lastModifyUser,
        @ApiParam(value = "分页-开始 -1 不分页")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam(value = "分页-每页大小 -1 不分页")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize
    );

    @ApiOperation(value = "获取执行方案基本信息列表", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task/plan/{templateId}", "/scope/{scopeType}/{scopeId}/task/plan/{templateId}"})
    Response<List<TaskPlanVO>> listPlans(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @DeprecatedAppLogic
        @ApiParam(value = "业务 ID", required = false)
        @PathVariable(value = "appId", required = false)
            Long appId,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "模版 ID", required = true)
        @PathVariable(value = "templateId")
            Long templateId
    );

    @ApiOperation(value = "批量获取执行方案基本信息", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task/plan:batchGet", "/scope/{scopeType}/{scopeId}/task/plan:batchGet"})
    Response<List<TaskPlanVO>> batchGetPlans(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @DeprecatedAppLogic
        @ApiParam(value = "业务 ID", required = false)
        @PathVariable(value = "appId", required = false)
            Long appId,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "模板ID列表，用英文逗号分隔", required = true)
        @RequestParam(value = "templateIds")
            String templateIds
    );

    @ApiOperation(value = "根据执行方案 ID 获取执行方案信息", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task/plan/{templateId}/{planId}",
        "/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}"})
    Response<TaskPlanVO> getPlanById(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @DeprecatedAppLogic
        @ApiParam(value = "业务 ID", required = false)
        @PathVariable(value = "appId", required = false)
            Long appId,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @ApiParam(value = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @ApiOperation(value = "获取模版对应的调试方案信息", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task/plan/{templateId}/debug",
        "/scope/{scopeType}/{scopeId}/task/plan/{templateId}/debug"})
    Response<TaskPlanVO> getDebugPlan(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @DeprecatedAppLogic
        @ApiParam(value = "业务 ID", required = false)
        @PathVariable(value = "appId", required = false)
            Long appId,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId
    );

    @ApiOperation(value = "更新执行方案", produces = "application/json")
    @PutMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}")
    Response<Long> savePlan(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @ApiParam(value = "执行方案 ID，新建时填 0", required = true)
        @PathVariable("planId")
            Long planId,
        @ApiParam(value = "更新的执行方案对象", name = "planCreateUpdateReq")
        @RequestBody
            TaskPlanCreateUpdateReq taskPlanCreateUpdateReq
    );

    @ApiOperation(value = "删除执行方案", produces = "application/json")
    @DeleteMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}")
    Response<Boolean> deletePlan(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @ApiParam(value = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @ApiOperation(value = "根据执行方案 ID 批量拉基础信息", produces = "application/json")
    @GetMapping(value = {"/app/{appId}/task/plan", "/scope/{scopeType}/{scopeId}/task/plan"})
    Response<List<TaskPlanVO>> listPlanBasicInfoByIds(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @DeprecatedAppLogic
        @ApiParam(value = "业务 ID", required = false)
        @PathVariable(value = "appId", required = false)
            Long appId,
        @ApiParam(value = "资源范围类型", required = false)
        @PathVariable(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID", required = false)
        @PathVariable(value = "scopeId", required = false)
            String scopeId,
        @ApiParam(value = "执行方案 ID 列表，逗号分隔", required = true, example = "1,2,3")
        @QueryParam("ids")
            String planIds
    );

    @ApiOperation(value = "检查执行方案名称是否已占用", produces = "application/json")
    @GetMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/check_name")
    Response<Boolean> checkPlanName(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "作业模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @ApiParam(value = "执行方案 ID，新建时填 0", required = true)
        @PathVariable("planId")
            Long planId,
        @ApiParam(value = "名称", required = true)
        @RequestParam(value = "name")
            String name
    );

    @ApiOperation(value = "获取执行方案同步信息", produces = "application/json")
    @GetMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/sync_info")
    Response<TaskPlanSyncInfoVO> syncInfo(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "作业模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @ApiParam(value = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @ApiOperation(value = "同步执行方案", produces = "application/json")
    @PostMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/sync")
    Response<Boolean> syncConfirm(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "作业模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @ApiParam(value = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId,
        @ApiParam(value = "作业模版版本", required = true)
        @RequestParam("templateVersion")
            String templateVersion
    );

    @ApiOperation(value = "新增收藏", produces = "application/json")
    @PutMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/favorite")
    Response<Boolean> addFavorite(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "模版ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @ApiParam(value = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @ApiOperation(value = "删除收藏", produces = "application/json")
    @DeleteMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/favorite")
    Response<Boolean> removeFavorite(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "模版ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @ApiParam(value = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @ApiOperation(value = "根据执行方案 ID 拉基本信息", produces = "application/json")
    @GetMapping("/task/plan/{planId}")
    Response<TaskPlanVO> getPlanBasicInfoById(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @ApiOperation(value = "批量根据变量名更新执行方案变量值", produces = "application/json")
    @PostMapping("/scope/{scopeType}/{scopeId}/task/plan/batch_update_variable")
    Response<Boolean> batchUpdatePlanVariableValueByName(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "批量更新请求体", required = true)
        @RequestBody
            List<TaskVariableValueUpdateReq> planVariableInfoList
    );
}
