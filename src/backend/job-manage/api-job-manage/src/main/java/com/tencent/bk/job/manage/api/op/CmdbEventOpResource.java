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

package com.tencent.bk.job.manage.api.op;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.op.req.EventWatchOpReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CMDB事件监听相关的OP接口
 */
@Api(tags = {"job-manage:api:CmdbEvent-OP"})
@RequestMapping("/op/cmdbEvent")
@InternalAPI
public interface CmdbEventOpResource {

    /**
     * 开启某一类事件监听
     *
     * @return 开启的事件监听器数量
     */
    @PutMapping("/enableWatch")
    @ApiOperation(value = "开启某一类事件监听", produces = "application/json")
    InternalResponse<Integer> enableWatch(
        @ApiParam(value = "事件监听操作请求体", required = true) @RequestBody EventWatchOpReq req
    );

    /**
     * 关闭某一类事件监听
     *
     * @return 关闭的事件监听器数量
     */
    @PutMapping("/disableWatch")
    @ApiOperation(value = "关闭某一类事件监听", produces = "application/json")
    InternalResponse<Integer> disableWatch(
        @ApiParam(value = "事件监听操作请求体", required = true) @RequestBody EventWatchOpReq req
    );
}
