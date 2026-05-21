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

package com.tencent.bk.job.execute.api.op;

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.execute.model.op.req.BatchAddCallbackUrlWhitelistReq;
import com.tencent.bk.job.execute.model.op.req.BatchDeleteCallbackUrlWhitelistReq;
import com.tencent.bk.job.execute.model.op.vo.CallbackUrlWhitelistVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回调地址白名单 OP 管理接口。
 * <p>
 * 仅提供批量接口；单条新增/删除通过传入仅含 1 个元素的列表实现。
 */
@Tag(name = "job-execute:OP:回调地址白名单OP管理接口")
@RequestMapping("/op/callbackUrlWhitelist")
@RestController
public interface CallbackUrlWhitelistOpResource {

    @Operation(summary = "批量新增回调地址白名单")
    @PostMapping("/batchAdd")
    Response<Integer> batchAdd(
        @Parameter(description = "用户名")
        @RequestHeader("username")
        String username,
        @RequestBody
        BatchAddCallbackUrlWhitelistReq req
    );

    @Operation(summary = "分页查询回调地址白名单")
    @GetMapping("/list")
    Response<PageData<CallbackUrlWhitelistVO>> list(
        @Parameter(description = "用户名")
        @RequestHeader("username")
        String username,
        @RequestParam(value = "start", required = false, defaultValue = "0")
        Integer start,
        @RequestParam(value = "length", required = false, defaultValue = "10")
        Integer length
    );

    @Operation(summary = "批量删除回调地址白名单")
    @DeleteMapping("/batchDelete")
    Response<Integer> batchDelete(
        @Parameter(description = "用户名")
        @RequestHeader("username")
        String username,
        @RequestBody
        BatchDeleteCallbackUrlWhitelistReq req
    );
}
