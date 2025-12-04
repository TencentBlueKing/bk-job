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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Api(tags = {"job-manage:service:WhiteIP"})
@SmartFeignClient(value = "job-manage", contextId = "whiteIpResource")
@InternalAPI
public interface ServiceWhiteIPResource {

    @Deprecated
    @CompatibleImplementation(
        name = "tenant",
        explain = "兼容发布过程中老的调用，发布完成后删除",
        deprecatedVersion = "3.12.x",
        type = CompatibleType.DEPLOY
    )
    @ApiOperation(value = "获取白名单内IP详情信息", produces = "application/json")
    @GetMapping("/service/whiteip/listWhiteIPInfos")
    InternalResponse<List<ServiceWhiteIPInfo>> listWhiteIPInfos();

    @ApiOperation(value = "获取某个租户下白名单内IP详情信息", produces = "application/json")
    @GetMapping("/service/whiteip/listWhiteIPInfosByTenantId")
    InternalResponse<List<ServiceWhiteIPInfo>> listWhiteIPInfosByTenantId(
        @ApiParam("租户ID")
        @RequestParam(value = "tenantId", required = true)
        String tenantId
    );
}
