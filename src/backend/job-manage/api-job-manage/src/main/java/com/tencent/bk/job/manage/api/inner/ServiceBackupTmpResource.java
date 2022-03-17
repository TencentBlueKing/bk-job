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
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TODO 后续需要优化
 * 临时提供给job-backup的API
 */
@RequestMapping("/service/tmp")
@Api(tags = {"job-manage:service:tmp_for_backup"})
@RestController
@InternalAPI
public interface ServiceBackupTmpResource {
    @ApiOperation(value = "根据模版 ID 获取模版信息", produces = "application/json")
    @GetMapping("/app/{appId}/template/{templateId}")
    Response<TaskTemplateVO> getTemplateById(
        @RequestHeader("username")
            String username,
        @PathVariable(value = "appId")
            Long appId,
        @PathVariable("templateId")
            Long templateId
    );

    @ApiOperation(value = "根据执行方案 ID 获取执行方案信息", produces = "application/json")
    @GetMapping("/app/{appId}/task/plan/{templateId}/{planId}")
    Response<TaskPlanVO> getPlanById(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @PathVariable(value = "appId")
            Long appId,
        @ApiParam(value = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @ApiParam(value = "执行方案 ID", required = true)
        @PathVariable("planId")
            Long planId
    );

    @ApiOperation(value = "获取执行方案基本信息列表", produces = "application/json")
    @GetMapping("/app/{appId}/task/plan/{templateId}")
    Response<List<TaskPlanVO>> listPlans(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @PathVariable(value = "appId")
            Long appId,
        @ApiParam(value = "模版 ID", required = true)
        @PathVariable(value = "templateId")
            Long templateId
    );

}
