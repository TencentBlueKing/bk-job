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

package com.tencent.bk.job.file.worker.api;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.file.worker.model.req.BaseReq;
import com.tencent.bk.job.file.worker.model.req.ExecuteActionReq;
import com.tencent.bk.job.file.worker.model.req.ListFileNodeReq;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Api(tags = {"job-file-worker:api:File"})
@RequestMapping("/worker/api/file")
@RestController
@InternalAPI
public interface FileResource {

    @ApiOperation(value = "测试文件源是否可用", produces = "application/json")
    @PostMapping("/available")
    InternalResponse<Boolean> isFileAvailable(
        @ApiParam(value = "文件源是否可用", required = true) @RequestBody BaseReq req);

    @ApiOperation(value = "获取文件源/FileNode下的子FileNode列表", produces = "application/json")
    @PostMapping("/listFileNode")
    InternalResponse<FileNodesDTO> listFileNode(
        @ApiParam(value = "获取文件源/FileNode下的子FileNode列表", required = true) @RequestBody ListFileNodeReq req);

    @ApiOperation(value = "文件源操作", produces = "application/json")
    @PostMapping("/executeAction")
    InternalResponse<Boolean> executeAction(
        @ApiParam(value = "文件源操作", required = true) @RequestBody ExecuteActionReq req
    );

}
