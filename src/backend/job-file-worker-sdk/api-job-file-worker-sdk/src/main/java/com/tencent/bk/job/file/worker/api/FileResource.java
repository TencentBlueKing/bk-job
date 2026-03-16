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

package com.tencent.bk.job.file.worker.api;

import com.tencent.bk.job.common.annotation.WorkerAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.file.worker.model.req.BaseReq;
import com.tencent.bk.job.file.worker.model.req.ExecuteActionReq;
import com.tencent.bk.job.file.worker.model.req.ListFileNodeReq;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "job-file-worker:api:File")
@RequestMapping("/worker/api/file")
@RestController
@WorkerAPI
public interface FileResource {

    @Operation(summary = "测试文件源是否可用", produces = "application/json")
    @PostMapping("/available")
    InternalResponse<Boolean> isFileAvailable(
        @Parameter(description = "文件源是否可用", required = true) @RequestBody BaseReq req);

    @Operation(summary = "获取文件源/FileNode下的子FileNode列表", produces = "application/json")
    @PostMapping("/listFileNode")
    InternalResponse<FileNodesDTO> listFileNode(
        @Parameter(description = "获取文件源/FileNode下的子FileNode列表", required = true) @RequestBody ListFileNodeReq req);

    @Operation(summary = "文件源操作", produces = "application/json")
    @PostMapping("/executeAction")
    InternalResponse<Boolean> executeAction(
        @Parameter(description = "文件源操作", required = true) @RequestBody ExecuteActionReq req
    );

}
