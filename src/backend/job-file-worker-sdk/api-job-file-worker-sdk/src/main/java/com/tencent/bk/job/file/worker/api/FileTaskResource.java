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
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.file.worker.model.req.ClearTaskFilesReq;
import com.tencent.bk.job.file.worker.model.req.DownloadFilesTaskReq;
import com.tencent.bk.job.file.worker.model.req.StopTasksReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = {"job-file-worker:api:FileTask"})
@RequestMapping("/worker/api/filetask")
@RestController
@InternalAPI
public interface FileTaskResource {

    // 子路径与gateway转发请求子路径保持一致
    @ApiOperation(value = "从文件源下载文件至本地", produces = "application/json")
    @PostMapping("/downloadFiles/start")
    Response<Integer> downloadFiles(
        @ApiParam(value = "文件下载任务", required = true) @RequestBody DownloadFilesTaskReq req
    );

    @ApiOperation(value = "停止正在进行中的任务", produces = "application/json")
    @PostMapping("/downloadFiles/stop")
    Response<Integer> stopTasks(
        @ApiParam(value = "停止任务请求", required = true) @RequestBody StopTasksReq req
    );

    @ApiOperation(value = "删除文件任务下载到本地的文件", produces = "application/json")
    @PostMapping("/clearFiles")
    Response<Integer> clearFiles(
        @ApiParam(value = "删除请求", required = true) @RequestBody ClearTaskFilesReq req
    );
}
