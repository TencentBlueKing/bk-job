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

package com.tencent.bk.job.file_gateway.api.inner;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.file_gateway.model.req.inner.ClearBatchTaskFilesReq;
import com.tencent.bk.job.file_gateway.model.req.inner.ClearTaskFilesReq;
import com.tencent.bk.job.file_gateway.model.req.inner.FileSourceBatchDownloadTaskReq;
import com.tencent.bk.job.file_gateway.model.req.inner.FileSourceDownloadTaskReq;
import com.tencent.bk.job.file_gateway.model.req.inner.StopBatchTaskReq;
import com.tencent.bk.job.file_gateway.model.req.inner.StopTaskReq;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskInfoDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskStatusDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.FileSourceTaskStatusDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;
import com.tencent.bk.job.file_gateway.service.BatchTaskService;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class ServiceFileSourceTaskResourceImpl implements ServiceFileSourceTaskResource {

    private final FileSourceTaskService fileSourceTaskService;
    private final BatchTaskService batchTaskService;

    @Autowired
    public ServiceFileSourceTaskResourceImpl(FileSourceTaskService fileSourceTaskService,
                                             BatchTaskService batchTaskService) {
        this.fileSourceTaskService = fileSourceTaskService;
        this.batchTaskService = batchTaskService;
    }

    @Override
    public InternalResponse<TaskInfoDTO> startFileSourceDownloadTask(String username, FileSourceDownloadTaskReq req) {
        return InternalResponse.buildSuccessResp(fileSourceTaskService.startFileSourceDownloadTask(username,
            req.getAppId(), req.getStepInstanceId(), req.getExecuteCount(), null, req.getFileSourceId(),
            req.getFilePathList()));
    }

    @Override
    public InternalResponse<Integer> stopTasks(StopTaskReq req) {
        return InternalResponse.buildSuccessResp(fileSourceTaskService.stopTasks(req.getTaskIdList()));
    }

    @Override
    public InternalResponse<FileSourceTaskStatusDTO> getFileSourceTaskStatusAndLogs(String taskId, Long logStart,
                                                                               Long logLength) {
        if (logStart == null || logStart < 0) {
            logStart = 0L;
        }
        if (logLength == null || logLength <= 0) {
            logLength = -1L;
        }
        return InternalResponse.buildSuccessResp(fileSourceTaskService.getFileSourceTaskStatusAndLogs(taskId,
            logStart, logLength));
    }

    @Override
    public InternalResponse<Integer> clearTaskFiles(ClearTaskFilesReq req) {
        return InternalResponse.buildSuccessResp(fileSourceTaskService.clearTaskFiles(req.getTaskIdList()));
    }

    @Override
    public InternalResponse<BatchTaskInfoDTO> startFileSourceBatchDownloadTask(String username,
                                                                          FileSourceBatchDownloadTaskReq req) {
        return InternalResponse.buildSuccessResp(batchTaskService.startFileSourceBatchDownloadTask(username,
            req.getAppId(), req.getStepInstanceId(), req.getExecuteCount(), req.getFileSourceTaskList()));
    }

    @Override
    public InternalResponse<Integer> stopBatchTasks(StopBatchTaskReq req) {
        return InternalResponse.buildSuccessResp(batchTaskService.stopBatchTasks(req.getBatchTaskIdList()));
    }

    @Override
    public InternalResponse<BatchTaskStatusDTO> getBatchTaskStatusAndLogs(String batchTaskId, Long logStart,
                                                                     Long logLength) {
        if (logStart == null || logStart < 0) {
            logStart = 0L;
        }
        if (logLength == null || logLength <= 0) {
            logLength = -1L;
        }
        return InternalResponse.buildSuccessResp(batchTaskService.getBatchTaskStatusAndLogs(batchTaskId, logStart,
            logLength));
    }

    @Override
    public InternalResponse<Integer> clearBatchTaskFiles(ClearBatchTaskFilesReq req) {
        return InternalResponse.buildSuccessResp(batchTaskService.clearBatchTaskFiles(req.getBatchTaskIdList()));
    }
}
