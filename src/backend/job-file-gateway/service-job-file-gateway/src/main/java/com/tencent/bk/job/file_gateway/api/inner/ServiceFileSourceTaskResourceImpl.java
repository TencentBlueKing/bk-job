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

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.file_gateway.model.req.inner.*;
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
    public ServiceResponse<TaskInfoDTO> startFileSourceDownloadTask(String username, FileSourceDownloadTaskReq req) {
        try {
            return ServiceResponse.buildSuccessResp(fileSourceTaskService.startFileSourceDownloadTask(username,
                req.getAppId(), req.getStepInstanceId(), req.getExecuteCount(), null, req.getFileSourceId(),
                req.getFilePathList()));
        } catch (ServiceException e) {
            log.error("Fail to startFileSourceDownloadTask", e);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public ServiceResponse<Integer> stopTasks(StopTaskReq req) {
        try {
            return ServiceResponse.buildSuccessResp(fileSourceTaskService.stopTasks(req.getTaskIdList()));
        } catch (ServiceException e) {
            log.error("Fail to stopTasks", e);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public ServiceResponse<FileSourceTaskStatusDTO> getFileSourceTaskStatusAndLogs(String taskId, Long logStart,
                                                                                   Long logLength) {
        if (logStart == null || logStart < 0) {
            logStart = 0L;
        }
        if (logLength == null || logLength <= 0) {
            logLength = -1L;
        }
        try {
            return ServiceResponse.buildSuccessResp(fileSourceTaskService.getFileSourceTaskStatusAndLogs(taskId,
                logStart, logLength));
        } catch (ServiceException e) {
            log.error("Fail to getFileSourceTaskStatus", e);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public ServiceResponse<Integer> clearTaskFiles(ClearTaskFilesReq req) {
        try {
            return ServiceResponse.buildSuccessResp(fileSourceTaskService.clearTaskFiles(req.getTaskIdList()));
        } catch (ServiceException e) {
            log.error("Fail to clearTaskFiles", e);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public ServiceResponse<BatchTaskInfoDTO> startFileSourceBatchDownloadTask(String username,
                                                                              FileSourceBatchDownloadTaskReq req) {
        try {
            return ServiceResponse.buildSuccessResp(batchTaskService.startFileSourceBatchDownloadTask(username,
                req.getAppId(), req.getStepInstanceId(), req.getExecuteCount(), req.getFileSourceTaskList()));
        } catch (ServiceException e) {
            log.error("Fail to startFileSourceBatchDownloadTask", e);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public ServiceResponse<Integer> stopBatchTasks(StopBatchTaskReq req) {
        try {
            return ServiceResponse.buildSuccessResp(batchTaskService.stopBatchTasks(req.getBatchTaskIdList()));
        } catch (ServiceException e) {
            log.error("Fail to stopBatchTasks", e);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public ServiceResponse<BatchTaskStatusDTO> getBatchTaskStatusAndLogs(String batchTaskId, Long logStart,
                                                                         Long logLength) {
        if (logStart == null || logStart < 0) {
            logStart = 0L;
        }
        if (logLength == null || logLength <= 0) {
            logLength = -1L;
        }
        try {
            return ServiceResponse.buildSuccessResp(batchTaskService.getBatchTaskStatusAndLogs(batchTaskId, logStart,
                logLength));
        } catch (ServiceException e) {
            log.error("Fail to getBatchTaskStatusAndLogs", e);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public ServiceResponse<Integer> clearBatchTaskFiles(ClearBatchTaskFilesReq req) {
        try {
            return ServiceResponse.buildSuccessResp(batchTaskService.clearBatchTaskFiles(req.getBatchTaskIdList()));
        } catch (ServiceException e) {
            log.error("Fail to clearBatchTaskFiles", e);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }
}
