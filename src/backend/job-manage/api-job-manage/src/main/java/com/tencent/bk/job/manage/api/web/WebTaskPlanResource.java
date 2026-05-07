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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.model.web.request.TaskPlanCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TaskVariableValueUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanBasicInfoVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanSyncInfoVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

import jakarta.ws.rs.QueryParam;
import java.util.List;

@Tag(name = "job-manage:web:Task_Plan_Management")
@RequestMapping("/web")
@RestController
@WebAPI
public interface WebTaskPlanResource {

    @Operation(summary = "获取业务下的执行方案列表")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task/plan/list"})
    Response<PageData<TaskPlanVO>> listAllPlans(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "执行方案 ID")
        @RequestParam(value = "planId", required = false)
            Long planId,
        @Parameter(description = "模板名称")
        @RequestParam(value = "templateName", required = false)
            String templateName,
        @Parameter(description = "模板 ID")
        @RequestParam(value = "templateId", required = false)
            Long templateId,
        @Parameter(description = "执行方案名称")
        @RequestParam(value = "planName", required = false)
            String planName,
        @Parameter(description = "创建人")
        @RequestParam(value = "creator", required = false)
            String creator,
        @Parameter(description = "更新人")
        @RequestParam(value = "lastModifyUser", required = false)
            String lastModifyUser,
        @Parameter(description = "分页-开始 -1 不分页")
        @RequestParam(value = "start", required = false)
            Integer start,
        @Parameter(description = "分页-每页大小 -1 不分页")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize
    );

    @Operation(summary = "获取执行方案基本信息列表")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task/plan/{templateId}"})
    Response<List<TaskPlanVO>> listPlans(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID", required = true)
        @PathVariable(value = "templateId")
            Long templateId
    );

    @Operation(summary = "批量获取执行方案基本信息")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task/plan:batchGet"})
    Response<List<TaskPlanVO>> batchGetPlans(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模板ID列表，用英文逗号分隔", required = true)
        @RequestParam(value = "templateIds")
            String templateIds
    );

    @Operation(summary = "根据执行方案 ID 获取执行方案信息")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}"})
    Response<TaskPlanVO> getPlanById(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @Operation(summary = "获取模版对应的调试方案信息")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task/plan/{templateId}/debug"})
    Response<TaskPlanVO> getDebugPlan(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId
    );

    @Operation(summary = "更新执行方案")
    @PutMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}")
    Response<TaskPlanVO> updatePlan(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId,
        @Parameter(description = "更新的执行方案对象", name = "planCreateUpdateReq")
        @RequestBody
            TaskPlanCreateUpdateReq taskPlanCreateUpdateReq
    );

    @Operation(summary = "新增执行方案")
    @PostMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}")
    Response<TaskPlanVO> createPlan(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "更新的执行方案对象", name = "planCreateUpdateReq")
        @RequestBody
            TaskPlanCreateUpdateReq taskPlanCreateUpdateReq
    );

    @Operation(summary = "删除执行方案")
    @DeleteMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}")
    Response<Boolean> deletePlan(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @Operation(summary = "根据执行方案 ID 批量拉基础信息")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task/plan"})
    Response<List<TaskPlanVO>> listPlanBasicInfoByIds(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "执行方案 ID 列表，逗号分隔", required = true, example = "1,2,3")
        @QueryParam("ids")
            String planIds
    );

    @Operation(summary = "根据执行方案 ID 批量拉执行方案基础信息")
    @GetMapping(value = {"/scope/{scopeType}/{scopeId}/task/plan/basicInfo"})
    Response<List<TaskPlanBasicInfoVO>> listTaskPlanBasicInfoByIds(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @Parameter(description = "执行方案 ID 列表，逗号分隔", required = true, example = "1,2,3")
        @QueryParam("ids")
        String planIds
    );

    @Operation(summary = "检查执行方案名称是否已占用")
    @GetMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/check_name")
    Response<Boolean> checkPlanName(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "作业模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "执行方案 ID，新建时填 0", required = true)
        @PathVariable("planId")
            Long planId,
        @Parameter(description = "名称", required = true)
        @RequestParam(value = "name")
            String name
    );

    @Operation(summary = "获取执行方案同步信息")
    @GetMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/sync_info")
    Response<TaskPlanSyncInfoVO> syncInfo(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "作业模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @Operation(summary = "同步执行方案")
    @PostMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/sync")
    Response<Boolean> syncConfirm(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "作业模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId,
        @Parameter(description = "作业模版版本", required = true)
        @RequestParam("templateVersion")
            String templateVersion
    );

    @Operation(summary = "新增收藏")
    @PutMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/favorite")
    Response<Boolean> addFavorite(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @Operation(summary = "删除收藏")
    @DeleteMapping("/scope/{scopeType}/{scopeId}/task/plan/{templateId}/{planId}/favorite")
    Response<Boolean> removeFavorite(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @Operation(summary = "根据执行方案 ID 拉基本信息")
    @GetMapping("/task/plan/{planId}")
    Response<TaskPlanVO> getPlanBasicInfoById(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(description = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @Operation(summary = "批量根据变量名更新执行方案变量值")
    @PostMapping("/scope/{scopeType}/{scopeId}/task/plan/batch_update_variable")
    Response<Boolean> batchUpdatePlanVariableValueByName(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(hidden = true)
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "批量更新请求体", required = true)
        @RequestBody
            List<TaskVariableValueUpdateReq> planVariableInfoList
    );
}
