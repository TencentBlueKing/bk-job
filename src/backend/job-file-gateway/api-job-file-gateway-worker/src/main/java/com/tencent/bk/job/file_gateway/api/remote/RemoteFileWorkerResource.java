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

package com.tencent.bk.job.file_gateway.api.remote;

import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.file_gateway.model.req.inner.HeartBeatReq;
import com.tencent.bk.job.file_gateway.model.req.inner.OffLineAndReDispatchReq;
import com.tencent.bk.job.file_gateway.model.req.inner.UpdateFileSourceTaskReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-file-gateway:remote:FileWorker"})
@RequestMapping("/remote/fileWorker")
@RestController
public interface RemoteFileWorkerResource {

    @ApiOperation(value = "Worker心跳", produces = "application/json")
    @PostMapping("/heartBeat")
    ServiceResponse<Long> heartBeat(
        @ApiParam(value = "Worker心跳请求") @RequestBody HeartBeatReq heartBeatReq);

    @ApiOperation(value = "Worker上报任务状态信息", produces = "application/json")
    @PostMapping("/task/update")
    ServiceResponse<String> updateFileSourceTask(
        @ApiParam(value = "Worker上报任务状态信息请求") @RequestBody UpdateFileSourceTaskReq updateFileSourceTaskReq);

    @ApiOperation(value = "Worker下线并且重调度其任务", produces = "application/json")
    @PostMapping("/offLineAndReDispatch")
    ServiceResponse<List<String>> offLineAndReDispatch(
        @ApiParam(value = "Worker下线携带的需要重调度的任务信息") @RequestBody OffLineAndReDispatchReq offLineAndReDispatchReq);

}
