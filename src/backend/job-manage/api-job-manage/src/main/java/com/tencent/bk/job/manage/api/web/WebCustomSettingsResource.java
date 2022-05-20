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

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.model.web.request.customsetting.ScriptTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.ScriptTemplateRenderReq;
import com.tencent.bk.job.manage.model.web.vo.customsetting.ScriptTemplateVO;
import com.tencent.bk.job.manage.model.web.vo.customsetting.ScriptTemplateVariableVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-manage:web:CustomSettings_ScriptTemplate"})
@RequestMapping("/web/customSettings/scriptTemplate")
@RestController
@WebAPI
public interface WebCustomSettingsResource {

    @ApiOperation(value = "获取用户自定义的脚本模板", produces = "application/json")
    @GetMapping
    Response<List<ScriptTemplateVO>> listUserCustomScriptTemplate(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("脚本类型，1:shell,2:bat,3:perl,4:python,5:PowerShell,6:sql;支持传入多个,用英文逗号分隔;如果不传入任何值，默认返回全部脚本类型的模板")
        @RequestParam(value = "scriptLanguages", required = false)
            String scriptLanguages);

    @ApiOperation(value = "获取渲染后的用户自定义的脚本模板", produces = "application/json")
    @GetMapping("/rendered")
    Response<List<ScriptTemplateVO>> listRenderedUserCustomScriptTemplate(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("脚本类型，1:shell,2:bat,3:perl,4:python,5:PowerShell,6:sql;支持传入多个,用英文逗号分隔;如果不传入任何值，默认返回全部脚本类型的模板")
        @RequestParam(value = "scriptLanguages", required = false)
            String scriptLanguages,
        @ApiParam(value = "资源范围类型")
        @RequestParam(value = "scopeType", required = false)
            String scopeType,
        @ApiParam(value = "资源范围ID")
        @RequestParam(value = "scopeId", required = false)
            String scopeId
    );

    @ApiOperation(value = "保存用户自定义的脚本模板", produces = "application/json")
    @PostMapping
    Response saveScriptTemplate(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @ApiParam(value = "创建或更新请求体", required = true)
        @RequestBody
            ScriptTemplateCreateUpdateReq req
    );

    @ApiOperation(value = "渲染自定义的脚本模板", produces = "application/json")
    @PostMapping("/render")
    Response<ScriptTemplateVO> renderScriptTemplate(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @ApiParam(value = "脚本模板渲染请求", required = true)
        @RequestBody
            ScriptTemplateRenderReq req
    );

    @ApiOperation(value = "获取用户自定义的脚本模板变量", produces = "application/json")
    @GetMapping("/variables")
    Response<List<ScriptTemplateVariableVO>> listScriptTemplateVariables(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

}
