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

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.model.web.request.GenUploadTargetReq;
import com.tencent.bk.job.manage.model.web.vo.UploadLocalFileResultVO;
import com.tencent.bk.job.manage.model.web.vo.UploadTargetVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 本地文件上传-前端调用
 */
@Api(tags = {"job-manage:web:File_Upload_Management"})
@RequestMapping("/web/upload")
@RestController
@WebAPI
public interface WebFileUploadResource {
    @ApiOperation(value = "上传本地文件", produces = "application/json")
    @PostMapping("/localFile")
    Response<List<UploadLocalFileResultVO>> uploadLocalFile(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("本地文件")
        @RequestParam("uploadFiles")
            MultipartFile[] uploadFiles);

    @ApiOperation(value = "生成上传目标地址信息", produces = "application/json")
    @PostMapping("/genUploadTarget")
    Response<UploadTargetVO> genUploadTarget(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("要上传的文件信息")
        @RequestBody
            GenUploadTargetReq req
    );
}
