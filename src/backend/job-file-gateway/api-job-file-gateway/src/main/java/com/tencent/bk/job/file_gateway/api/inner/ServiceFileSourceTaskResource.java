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

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.file_gateway.model.req.inner.ClearTaskFilesReq;
import com.tencent.bk.job.file_gateway.model.req.inner.FileSourceBatchDownloadTaskReq;
import com.tencent.bk.job.file_gateway.model.req.inner.StopBatchTaskReq;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskInfoDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskStatusDTO;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;


@Api(tags = {"job-file-gateway:service:FileSourceTask"})
@SmartFeignClient(value = "job-file-gateway", contextId = "fileSourceTaskResource")
@InternalAPI
public interface ServiceFileSourceTaskResource {

    @ApiOperation(value = "清理任务已下载的文件", produces = "application/json")
    @PostMapping("/service/fileSource/filetask/clearFiles")
    InternalResponse<Integer> clearTaskFiles(
        @ApiParam("文件源下载任务请求")
        @RequestBody ClearTaskFilesReq req
    );

    @ApiOperation(value = "创建并启动批量文件下载任务", produces = "application/json")
    @PostMapping("/service/fileSource/filetask/batch/downloadFiles/start")
    InternalResponse<BatchTaskInfoDTO> startFileSourceBatchDownloadTask(
        @ApiParam("用户名")
        @RequestHeader("username")
        String username,
        @ApiParam("文件源下载任务请求")
        @RequestBody FileSourceBatchDownloadTaskReq req
    );

    @ApiOperation(value = "清理批量任务已下载的文件", produces = "application/json")
    @PostMapping("/service/fileSource/filetask/batch/downloadFiles/stop")
    InternalResponse<Integer> stopBatchTasks(
        @ApiParam("文件源下载任务请求")
        @RequestBody StopBatchTaskReq req
    );

    @ApiOperation(value = "获取文件批量任务状态", produces = "application/json")
    @GetMapping("/service/fileSource/filetask/batch/batchTaskIds/{batchTaskId}/status")
    InternalResponse<BatchTaskStatusDTO> getBatchTaskStatusAndLogs(
        @ApiParam("任务Id")
        @PathVariable("batchTaskId")
        String batchTaskId,
        @ApiParam("日志开始位置")
        @RequestParam(value = "logStart", required = false)
        Long logStart,
        @ApiParam("获取日志数量")
        @RequestParam(value = "logLength", required = false)
        Long logLength
    );
}
