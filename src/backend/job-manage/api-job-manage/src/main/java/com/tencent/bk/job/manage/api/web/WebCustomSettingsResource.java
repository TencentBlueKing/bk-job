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
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.model.web.request.customsetting.BatchGetCustomSettingsReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.DeleteCustomSettingsReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.SaveCustomSettingsReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

@Api(tags = {"job-manage:web:CustomSettings"})
@RequestMapping("/web/customSettings/scope/{scopeType}/{scopeId}")
@RestController
@WebAPI
public interface WebCustomSettingsResource {

    // 标准接口19
    @ApiOperation(value = "保存多个配置项，返回成功保存的配置项内容", produces = "application/json")
    @PostMapping("")
    Response<Map<String, Map<String, Object>>> saveCustomSettings(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "保存配置项请求体", required = true)
        @RequestBody
            SaveCustomSettingsReq req
    );

    // 标准接口20
    @ApiOperation(value = "批量获取多个配置项内容，返回配置项内容Map，Key为配置模块", produces = "application/json")
    @PostMapping("/batchGet")
    Response<Map<String, Map<String, Object>>> batchGetCustomSettings(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "批量获取配置项请求体", required = true)
        @RequestBody(required = false)
            BatchGetCustomSettingsReq req);

    // 标准接口21
    @ApiOperation(value = "删除多个模块配置项内容，返回删除成功的模块配置数量", produces = "application/json")
    @DeleteMapping("")
    Response<Integer> deleteCustomSettings(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "删除配置项请求体", required = true)
        @RequestBody
            DeleteCustomSettingsReq req);

}
