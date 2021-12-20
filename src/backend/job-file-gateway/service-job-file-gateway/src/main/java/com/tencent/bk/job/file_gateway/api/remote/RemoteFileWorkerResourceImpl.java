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

import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.req.inner.HeartBeatReq;
import com.tencent.bk.job.file_gateway.model.req.inner.OffLineAndReDispatchReq;
import com.tencent.bk.job.file_gateway.model.req.inner.UpdateFileSourceTaskReq;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskService;
import com.tencent.bk.job.file_gateway.service.FileWorkerService;
import com.tencent.bk.job.file_gateway.service.ReDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@Slf4j
public class RemoteFileWorkerResourceImpl implements RemoteFileWorkerResource {

    private final FileWorkerService fileWorkerService;
    private final FileSourceTaskService fileSourceTaskService;
    private final ReDispatchService reDispatchService;

    @Autowired
    public RemoteFileWorkerResourceImpl(FileWorkerService fileWorkerService,
                                        FileSourceTaskService fileSourceTaskService,
                                        ReDispatchService reDispatchService) {
        this.fileWorkerService = fileWorkerService;
        this.fileSourceTaskService = fileSourceTaskService;
        this.reDispatchService = reDispatchService;
    }

    @Override
    public Response<Long> heartBeat(HeartBeatReq heartBeatReq) {
        log.info("Input=(heartBeatReq={})", heartBeatReq.toString());
        return Response.buildSuccessResp(fileWorkerService.heartBeat(FileWorkerDTO.fromReq(heartBeatReq)));
    }

    @Override
    public Response<String> updateFileSourceTask(UpdateFileSourceTaskReq updateFileSourceTaskReq) {
        log.debug("Input=({})", updateFileSourceTaskReq);
        String taskId = updateFileSourceTaskReq.getFileSourceTaskId();
        String filePath = updateFileSourceTaskReq.getFilePath();
        String downloadPath = updateFileSourceTaskReq.getDownloadPath();
        Long fileSize = updateFileSourceTaskReq.getFileSize();
        String speed = updateFileSourceTaskReq.getSpeed();
        Integer progress = updateFileSourceTaskReq.getProgress();
        String content = updateFileSourceTaskReq.getContent();
        TaskStatusEnum status = updateFileSourceTaskReq.getStatus();
        return Response.buildSuccessResp(fileSourceTaskService.updateFileSourceTask(taskId, filePath,
            downloadPath, fileSize, speed, progress, content, status));
    }

    @Override
    public Response<List<String>> offLineAndReDispatch(OffLineAndReDispatchReq offLineAndReDispatchReq) {
        return Response.buildSuccessResp(
            reDispatchService.reDispatchByWorker(
                offLineAndReDispatchReq.getAccessHost(),
                offLineAndReDispatchReq.getAccessPort(),
                offLineAndReDispatchReq.getTaskIdList(),
                offLineAndReDispatchReq.getInitDelayMills(),
                offLineAndReDispatchReq.getIntervalMills())
        );
    }
}
