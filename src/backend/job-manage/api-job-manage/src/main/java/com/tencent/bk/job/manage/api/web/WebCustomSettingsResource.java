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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.model.web.request.customsetting.BatchGetCustomSettingsReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.DeleteCustomSettingsReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.SaveCustomSettingsReq;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.Map;

@Tag(name = "job-manage:web:CustomSettings")
@RequestMapping("/web/customSettings")
@RestController
@WebAPI
public interface WebCustomSettingsResource {

    // 标准接口19
    @Deprecated
    @CompatibleImplementation(
        name = "tenant",
        explain = "兼容发布过程中老的调用，发布完成后删除",
        deprecatedVersion = "3.12.x",
        type = CompatibleType.DEPLOY
    )
    @Operation(summary = "保存多个配置项，返回成功保存的配置项内容")
    @PostMapping("/scope/{scopeType}/{scopeId}")
    Response<Map<String, Map<String, Object>>> saveCustomSettingsForScope(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
        String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @Parameter(description = "保存配置项请求体", required = true)
        @RequestBody
        SaveCustomSettingsReq req
    );

    // 标准接口19
    @Operation(summary = "保存多个配置项，返回成功保存的配置项内容")
    @PostMapping("")
    Response<Map<String, Map<String, Object>>> saveCustomSettings(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
        String username,
        @Parameter(description = "保存配置项请求体", required = true)
        @RequestBody
        SaveCustomSettingsReq req
    );

    // 标准接口20
    @Deprecated
    @CompatibleImplementation(
        name = "tenant",
        explain = "兼容发布过程中老的调用，发布完成后删除",
        deprecatedVersion = "3.12.x",
        type = CompatibleType.DEPLOY
    )
    @Operation(summary = "批量获取多个配置项内容，返回配置项内容Map，Key为配置模块")
    @PostMapping("/scope/{scopeType}/{scopeId}/batchGet")
    Response<Map<String, Map<String, Object>>> batchGetCustomSettingsForScope(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @Parameter(description = "批量获取配置项请求体", required = true)
        @RequestBody(required = false)
        BatchGetCustomSettingsReq req);

    // 标准接口20
    @Operation(summary = "批量获取多个配置项内容，返回配置项内容Map，Key为配置模块")
    @PostMapping("/batchGet")
    Response<Map<String, Map<String, Object>>> batchGetCustomSettings(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @Parameter(description = "批量获取配置项请求体", required = true)
        @RequestBody(required = false)
        BatchGetCustomSettingsReq req);

    // 标准接口21
    @Deprecated
    @CompatibleImplementation(
        name = "tenant",
        explain = "兼容发布过程中老的调用，发布完成后删除",
        deprecatedVersion = "3.12.x",
        type = CompatibleType.DEPLOY
    )
    @Operation(summary = "删除多个模块配置项内容，返回删除成功的模块配置数量")
    @DeleteMapping("/scope/{scopeType}/{scopeId}")
    Response<Integer> deleteCustomSettingsForScope(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @Parameter(description = "删除配置项请求体", required = true)
        @RequestBody
        DeleteCustomSettingsReq req);

    // 标准接口21
    @Operation(summary = "删除多个模块配置项内容，返回删除成功的模块配置数量")
    @DeleteMapping("")
    Response<Integer> deleteCustomSettings(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @Parameter(description = "删除配置项请求体", required = true)
        @RequestBody
        DeleteCustomSettingsReq req);

}
