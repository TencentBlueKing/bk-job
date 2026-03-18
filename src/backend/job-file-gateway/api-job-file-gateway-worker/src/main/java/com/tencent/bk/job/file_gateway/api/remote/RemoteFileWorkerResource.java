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

package com.tencent.bk.job.file_gateway.api.remote;

import com.tencent.bk.job.common.annotation.RemoteAPI;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.file_gateway.model.req.inner.HeartBeatReq;
import com.tencent.bk.job.file_gateway.model.req.inner.OffLineAndReDispatchReq;
import com.tencent.bk.job.file_gateway.model.req.inner.UpdateFileSourceTaskReq;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "job-file-gateway:remote:FileWorker")
@RequestMapping("/remote/fileWorker")
@RestController
@RemoteAPI
public interface RemoteFileWorkerResource {

    @Operation(summary = "Worker心跳")
    @PostMapping("/heartBeat")
    Response<Long> heartBeat(
        @Parameter(description = "Worker心跳请求") @RequestBody HeartBeatReq heartBeatReq);

    @Operation(summary = "Worker上报任务状态信息")
    @PostMapping("/task/update")
    Response<String> updateFileSourceTask(
        @Parameter(description = "Worker上报任务状态信息请求") @RequestBody UpdateFileSourceTaskReq updateFileSourceTaskReq);

    @Operation(summary = "Worker下线并且重调度其任务")
    @PostMapping("/offLineAndReDispatch")
    Response<List<String>> offLineAndReDispatch(
        @Parameter(description = "Worker下线携带的需要重调度的任务信息") @RequestBody OffLineAndReDispatchReq offLineAndReDispatchReq);

}
