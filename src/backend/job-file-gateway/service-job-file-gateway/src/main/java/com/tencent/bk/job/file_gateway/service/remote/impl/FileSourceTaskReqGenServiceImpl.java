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

package com.tencent.bk.job.file_gateway.service.remote.impl;

import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.file.worker.model.req.ClearTaskFilesReq;
import com.tencent.bk.job.file.worker.model.req.DownloadFilesTaskReq;
import com.tencent.bk.job.file.worker.model.req.StopTasksReq;
import com.tencent.bk.job.file_gateway.consts.TaskCommandEnum;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.service.CredentialService;
import com.tencent.bk.job.file_gateway.service.remote.FileSourceTaskReqGenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class FileSourceTaskReqGenServiceImpl
    extends BaseRemoteFileReqGenServiceImpl implements FileSourceTaskReqGenService {

    @Autowired
    public FileSourceTaskReqGenServiceImpl(CredentialService credentialService) {
        super(credentialService);
    }

    @Override
    public HttpReq genDownloadFilesReq(Long appId, FileWorkerDTO fileWorkerDTO, FileSourceDTO fileSourceDTO,
                                       FileSourceTaskDTO fileSourceTaskDTO) {
        DownloadFilesTaskReq req = new DownloadFilesTaskReq();
        String url = fillBaseReqGetUrl(req, fileWorkerDTO, fileSourceDTO, "/filetask/downloadFiles/start");

        req.setTaskId(fileSourceTaskDTO.getId());
        // 前缀生成
        String filePrefix = fileSourceDTO.getFilePrefix();
        if ("${UUID}".equals(filePrefix)) {
            filePrefix = JobUUID.getUUID();
        }
        req.setFilePrefix(filePrefix);
        req.setFilePathList(fileSourceTaskDTO.getFileTaskList().parallelStream()
            .map(FileTaskDTO::getFilePath).collect(Collectors.toList()));

        return genRemoteFileReq(url, req);
    }

    @Override
    public HttpReq genClearTaskFilesReq(FileWorkerDTO fileWorkerDTO, List<String> taskIdList) {
        ClearTaskFilesReq req = new ClearTaskFilesReq(taskIdList);
        String url = getCompleteUrl(fileWorkerDTO, "/filetask/clearFiles");
        req.setTaskIdList(taskIdList);
        return genRemoteFileReq(url, req);
    }

    @Override
    public HttpReq genStopTasksReq(FileWorkerDTO fileWorkerDTO, List<String> taskIdList, TaskCommandEnum command) {
        StopTasksReq req = new StopTasksReq();
        String url = getCompleteUrl(fileWorkerDTO, "/filetask/downloadFiles/stop");
        req.setTaskIdList(taskIdList);
        req.setTaskCommand(command.getValue());
        return genRemoteFileReq(url, req);
    }
}
