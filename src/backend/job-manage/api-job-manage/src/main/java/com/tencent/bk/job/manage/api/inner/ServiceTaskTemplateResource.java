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
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.web.request.TaskTemplateCreateUpdateReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-manage:service:Task_Template_Management"})
@RequestMapping("/service")
@RestController
@InternalAPI
public interface ServiceTaskTemplateResource {

    @ApiOperation(value = "同步脚本更新消息", produces = "application/json")
    @PostMapping("/app/{appId}/template/script/{scriptId}/update_message")
    InternalResponse<Boolean> sendScriptUpdateMessage(
        @ApiParam(value = "业务 ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "脚本 ID", required = true) @PathVariable("scriptId") String scriptId,
        @ApiParam(value = "脚本版本 ID", required = true) @RequestParam("scriptVersionId") Long scriptVersionId,
        @ApiParam(value = "脚本状态 1 - 上线 2 - 下线 3 - 禁用", required = true) @RequestParam("status") Integer status);

    @ApiOperation(value = "根据模版 ID 获取模版信息", produces = "application/json")
    @GetMapping("/app/{appId}/template/{templateId}")
    InternalResponse<ServiceTaskTemplateDTO> getTemplateById(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID") @PathVariable("templateId") Long templateId);

    @ApiOperation(value = "根据模版 ID 获取模版信息", produces = "application/json")
    @GetMapping("/template/{templateId}")
    InternalResponse<ServiceTaskTemplateDTO> getTemplateById(
        @ApiParam(value = "模版 ID") @PathVariable("templateId") Long templateId);

    @ApiOperation(value = "根据模版 ID 获取模版名称", produces = "application/json")
    @GetMapping("/template/{templateId}/templateName")
    InternalResponse<String> getTemplateNameById(
        @ApiParam(value = "模版 ID") @PathVariable("templateId") Long templateId);

    @ApiOperation(value = "更新模版", produces = "application/json")
    @PutMapping("/app/{appId}/template/{templateId}/saveTemplateWithVariableId")
    InternalResponse<Long> saveTemplateForMigration(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID 新建填 0", required = true) @PathVariable("templateId") Long templateId,
        @ApiParam(value = "创建时间") @RequestHeader(value = "X-Create-Time", required = false) Long createTime,
        @ApiParam(value = "修改时间") @RequestHeader(value = "X-Update-Time", required = false) Long lastModifyTime,
        @ApiParam(value = "最后修改人") @RequestHeader(value = "X-Update-User", required = false) String lastModifyUser,
        @ApiParam(value = "来源") @RequestHeader(value = "X-Request-Source", required = false) Integer requestSource,
        @ApiParam(value = "新增/更新的模版对象", name = "templateCreateUpdateReq",
            required = true) @RequestBody TaskTemplateCreateUpdateReq taskTemplateCreateUpdateReq);

    @ApiOperation(value = "校验模版 ID 和名称", produces = "application/json")
    @GetMapping("/app/{appId}/template/check")
    InternalResponse<ServiceIdNameCheckDTO> checkIdAndName(
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID") @RequestParam("templateId") Long templateId,
        @ApiParam(value = "模版名称", required = true) @RequestParam("templateName") String name);

    @ApiOperation(value = "根据模版 ID 获取模版变量信息", produces = "application/json")
    @GetMapping("/app/{appId}/template/{templateId}/variable")
    InternalResponse<List<ServiceTaskVariableDTO>> getTemplateVariable(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID") @PathVariable("templateId") Long templateId);

    @ApiOperation(value = "获取模版信息列表", produces = "application/json")
    @GetMapping("/app/{appId}/template/list")
    InternalResponse<PageData<ServiceTaskTemplateDTO>> listPageTaskTemplates(
        @ApiParam(value = "业务 ID", required = true, example = "2")
        @PathVariable("appId")
            Long appId,
        @ApiParam("分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam("分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize);
}
