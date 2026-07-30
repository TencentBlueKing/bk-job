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
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateReq;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "job-manage:service:Script_Management")
@SmartFeignClient(value = "job-manage", contextId = "scriptResource")
@InternalAPI
public interface ServiceScriptResource {
    @Operation(summary = "根据业务ID、脚本版本ID获取脚本")
    @GetMapping("/service/script/app/{appId}/scriptVersion/{scriptVersionId}")
    InternalResponse<ServiceScriptDTO> getScriptByAppIdAndScriptVersionId(@RequestHeader("username")
                                                                             String username,
                                                                     @PathVariable("appId")
                                                                             Long appId,
                                                                     @PathVariable("scriptVersionId")
                                                                             Long scriptVersionId);

    @Operation(summary = "根据脚本版本ID获取脚本")
    @GetMapping("/service/script/scriptVersion/{scriptVersionId}")
    InternalResponse<ServiceScriptDTO> getScriptByScriptVersionId(@PathVariable("scriptVersionId") Long scriptVersionId);

    @Operation(summary = "指定Id创建脚本版本")
    @PostMapping("/service/script/app/{appId}/createScriptWithVersionId")
    InternalResponse<Pair<String, Long>> createScriptWithVersionId(
        @Parameter(description = "用户名，网关自动传入") @RequestHeader("username") String username,
        @Parameter(description = "创建时间") @RequestHeader(value = "X-Create-Time", required = false) Long createTime,
        @Parameter(description = "修改时间") @RequestHeader(value = "X-Update-Time", required = false) Long lastModifyTime,
        @Parameter(description = "最后修改人") @RequestHeader(value = "X-Update-User", required = false) String lastModifyUser,
        @Parameter(description = "脚本状态") @RequestHeader(value = "X-Script-Status", required = false) Integer scriptStatus,
        @Parameter(description = "业务ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @Parameter(description = "新增/更新的脚本对象", name = "scriptCreateUpdateReq",
            required = true) @RequestBody ScriptCreateReq scriptCreateReq);

    @Operation(summary = "获取脚本基本信息")
    @GetMapping("/service/script/{scriptId}")
    InternalResponse<ServiceScriptDTO> getBasicScriptInfo(@PathVariable("scriptId") String scriptId);

    @Operation(summary = "获取已上线版本")
    @GetMapping("/service/script/scriptVersion/online/{scriptId}")
    InternalResponse<ServiceScriptDTO> getOnlineScriptVersion(@PathVariable("scriptId") String scriptId);
}
