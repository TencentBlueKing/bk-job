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

package com.tencent.bk.job.file_gateway.api.inner;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Api(tags = {"job-file-gateway:service:FileSource"})
@SmartFeignClient(value = "job-file-gateway", contextId = "fileSourceResource")
@InternalAPI
public interface ServiceFileSourceResource {

    @ApiOperation(value = "获取文件源ID", produces = "application/json")
    @GetMapping("/service/app/{appId}/fileSource/getFileSourceIdByCode/codes/{code}")
    InternalResponse<Integer> getFileSourceIdByCode(
        @ApiParam(value = "Job业务ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "文件源标识", required = true) @PathVariable("code") String code);

    @Deprecated
    @CompatibleImplementation(name = "fileSourceId", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "文件源标识仅在appId下唯一，发布完成后可删除")
    @ApiOperation(value = "获取文件源ID", produces = "application/json")
    @GetMapping("/service/fileSource/getFileSourceIdByCode/codes/{code}")
    InternalResponse<Integer> getFileSourceIdByCode(
        @ApiParam(value = "文件源标识", required = true) @PathVariable("code") String code);

    @ApiOperation(value = "判断是否存在文件源引用了指定凭证", produces = "application/json")
    @GetMapping("/service/app/{appId}/fileSource/existsFileSourceUsingCredential/credentialIds/{credentialId}")
    InternalResponse<Boolean> existsFileSourceUsingCredential(
        @ApiParam(value = "Job业务ID", required = true) @PathVariable("appId") Long appId,
        @ApiParam(value = "凭证ID", required = true) @PathVariable("credentialId") String credentialId);
}
