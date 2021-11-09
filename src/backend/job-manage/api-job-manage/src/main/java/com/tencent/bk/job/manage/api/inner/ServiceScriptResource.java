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
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateUpdateReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = {"job-manage:service:Script_Management"})
@RequestMapping("/service/script")
@RestController
@InternalAPI
public interface ServiceScriptResource {
    @ApiOperation(value = "根据业务ID、脚本版本ID获取脚本", produces = "application/json")
    @GetMapping("/app/{appId}/scriptVersion/{scriptVersionId}")
    InternalResponse<ServiceScriptDTO> getScriptByAppIdAndScriptVersionId(@RequestHeader("username")
                                                                             String username,
                                                                     @PathVariable("appId")
                                                                             Long appId,
                                                                     @PathVariable("scriptVersionId")
                                                                             Long scriptVersionId);

    @ApiOperation(value = "根据脚本版本ID获取脚本", produces = "application/json")
    @GetMapping("/scriptVersion/{scriptVersionId}")
    InternalResponse<ServiceScriptDTO> getScriptByScriptVersionId(@PathVariable("scriptVersionId") Long scriptVersionId);

    @ApiOperation(value = "指定Id创建脚本版本", produces = "application/json")
    @PostMapping("/app/{appId}/createScriptWithVersionId")
    InternalResponse<Pair<String, Long>> createScriptWithVersionId(
        @ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam("创建时间") @RequestHeader(value = "X-Create-Time", required = false) Long createTime,
        @ApiParam("修改时间") @RequestHeader(value = "X-Update-Time", required = false) Long lastModifyTime,
        @ApiParam("最后修改人") @RequestHeader(value = "X-Update-User", required = false) String lastModifyUser,
        @ApiParam("脚本状态") @RequestHeader(value = "X-Script-Status", required = false) Integer scriptStatus,
        @ApiParam(value = "业务ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "新增/更新的脚本对象", name = "scriptCreateUpdateReq",
            required = true) @RequestBody ScriptCreateUpdateReq scriptCreateUpdateReq);

    @ApiOperation(value = "获取脚本基本信息", produces = "application/json")
    @GetMapping("/{scriptId}")
    InternalResponse<ServiceScriptDTO> getBasicScriptInfo(@PathVariable("scriptId") String scriptId);

    @ApiOperation(value = "获取已上线版本", produces = "application/json")
    @GetMapping("/scriptVersion/online/{scriptId}")
    InternalResponse<ServiceScriptDTO> getOnlineScriptVersion(@PathVariable("scriptId") String scriptId);
}
