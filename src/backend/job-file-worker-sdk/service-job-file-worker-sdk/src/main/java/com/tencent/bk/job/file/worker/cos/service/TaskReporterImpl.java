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

package com.tencent.bk.job.file.worker.cos.service;

import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.http.AbstractHttpHelper;
import com.tencent.bk.job.common.util.http.DefaultHttpHelper;
import com.tencent.bk.job.common.util.http.HttpReqGenUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.model.req.inner.UpdateFileSourceTaskReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TaskReporterImpl implements TaskReporter {

    private final AbstractHttpHelper httpHelper = new DefaultHttpHelper();
    private final GatewayInfoService gatewayInfoService;

    @Autowired
    public TaskReporterImpl(GatewayInfoService gatewayInfoService) {
        this.gatewayInfoService = gatewayInfoService;
    }

    public void reportFileDownloadStart(String taskId, String filePath, String downloadPath) {
        UpdateFileSourceTaskReq req = new UpdateFileSourceTaskReq();
        req.setFileSourceTaskId(taskId);
        req.setFilePath(filePath);
        req.setDownloadPath(downloadPath);
        req.setStatus(TaskStatusEnum.RUNNING);
        req.setSpeed("0 KB/s");
        req.setContent("Start pulling");
        req.setProgress(0);
        reportTaskStatus(req);
    }

    public void reportFileDownloadProgress(String taskId, String filePath, String downloadPath, Long fileSize,
                                           Integer speed, Integer progress) {
        UpdateFileSourceTaskReq req = new UpdateFileSourceTaskReq();
        req.setFileSourceTaskId(taskId);
        req.setFilePath(filePath);
        req.setDownloadPath(downloadPath);
        req.setStatus(TaskStatusEnum.RUNNING);
        req.setFileSize(fileSize);
        req.setSpeed(speed + " KB/s");
        req.setContent("Pulling OSS file...");
        req.setProgress(progress);
        reportTaskStatus(req);
    }

    public void reportFileDownloadSuccess(String taskId, String filePath, String downloadPath, Long fileSize,
                                          Integer speed, Integer progress) {
        UpdateFileSourceTaskReq req = new UpdateFileSourceTaskReq();
        req.setFileSourceTaskId(taskId);
        req.setFilePath(filePath);
        req.setDownloadPath(downloadPath);
        req.setStatus(TaskStatusEnum.SUCCESS);
        req.setFileSize(fileSize);
        req.setSpeed(speed + " KB/s");
        req.setContent("Pulling finished");
        req.setProgress(progress);
        reportTaskStatus(req);
    }

    public void reportFileDownloadFailure(String taskId, String filePath, String downloadPath) {
        reportFileDownloadFailure(taskId, filePath, downloadPath, "Pulling failed");
    }

    @Override
    public void reportFileDownloadStopped(String taskId, String filePath, String downloadPath, Long fileSize,
                                          Integer progress) {
        UpdateFileSourceTaskReq req = new UpdateFileSourceTaskReq();
        req.setFileSourceTaskId(taskId);
        req.setFilePath(filePath);
        req.setDownloadPath(downloadPath);
        req.setStatus(TaskStatusEnum.STOPPED);
        req.setFileSize(fileSize);
        req.setSpeed("--");
        req.setContent("Pulling stopped");
        req.setProgress(progress);
        reportTaskStatus(req);
    }

    public void reportFileDownloadFailure(String taskId, String filePath, String downloadPath, String content) {
        UpdateFileSourceTaskReq req = new UpdateFileSourceTaskReq();
        req.setFileSourceTaskId(taskId);
        req.setFilePath(filePath);
        req.setDownloadPath(downloadPath);
        req.setStatus(TaskStatusEnum.FAILED);
        req.setSpeed("--");
        req.setContent(content);
        req.setProgress(0);
        reportTaskStatus(req);
    }

    @Override
    public void reportWorkerOffLine(List<String> taskIdList, String content) {
        // TODO
    }


    public void reportTaskStatus(UpdateFileSourceTaskReq updateFileSourceTaskReq) {
        String url = gatewayInfoService.getReportTaskStatusUrl();
        HttpReq req = HttpReqGenUtil.genSimpleJsonReq(url, updateFileSourceTaskReq);
        String respStr = null;
        try {
            log.info(String.format("url=%s,body=%s,headers=%s", url, req.getBody(),
                JsonUtils.toJson(req.getHeaders())));
            respStr = httpHelper.post(url, req.getBody(), req.getHeaders());
            log.info(String.format("respStr=%s", respStr));
        } catch (Exception e) {
            log.error("Fail to request file-gateway:", e);
        }
    }
}
