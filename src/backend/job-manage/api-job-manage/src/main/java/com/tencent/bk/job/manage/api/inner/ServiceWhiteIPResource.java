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
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
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

@Api(tags = {"job-manage:service:WhiteIP"})
@RequestMapping("/service/whiteip")
@RestController
@InternalAPI
public interface ServiceWhiteIPResource {

    @ApiOperation(value = "获取指定IP在白名单中的生效范围（脚本执行：SCRIPT_EXECUTE/文件分发：FILE_DISTRIBUTION）", produces = "application/json")
    @GetMapping("/getWhiteIPActionScopes")
    InternalResponse<List<String>> getWhiteIPActionScopes(
        @ApiParam("业务Id")
        @RequestParam(value = "appId", required = false)
            Long appId,
        @ApiParam("IP")
        @RequestParam(value = "ip", required = false)
            String ip,
        @ApiParam("云区域Id")
        @RequestParam(value = "cloudAreaId", required = false)
            Long cloudAreaId
    );

    @ApiOperation(value = "获取白名单内IP详情信息", produces = "application/json")
    @GetMapping("/listWhiteIPInfos")
    InternalResponse<List<ServiceWhiteIPInfo>> listWhiteIPInfos();

    @ApiOperation(value = "新增/更新IP白名单", produces = "application/json")
    @PostMapping("")
    InternalResponse<Long> saveWhiteIP(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @ApiParam(value = "创建或更新请求体", required = true)
        @RequestBody
            WhiteIPRecordCreateUpdateReq createUpdateReq
    );
}
