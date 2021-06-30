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

import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.file.worker.cos.service.FileTaskService;
import com.tencent.bk.job.file.worker.cos.service.RemoteClient;
import com.tencent.bk.job.file.worker.cos.service.ThreadCommandBus;
import com.tencent.bk.job.file.worker.model.req.BaseReq;
import com.tencent.bk.job.file.worker.model.req.ClearTaskFilesReq;
import com.tencent.bk.job.file.worker.model.req.DownloadFilesTaskReq;
import com.tencent.bk.job.file.worker.model.req.StopTasksReq;
import com.tencent.bk.job.file_gateway.consts.TaskCommandEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class FileTaskResourceImpl implements RemoteClientAccess, FileTaskResource {

    protected final FileTaskService fileTaskService;

    public FileTaskResourceImpl(FileTaskService fileTaskService) {
        this.fileTaskService = fileTaskService;
    }

    public abstract RemoteClient getRemoteClient(BaseReq req);

    @Override
    public ServiceResponse<Integer> downloadFiles(DownloadFilesTaskReq req) {
        log.debug("req={}", req);
        RemoteClient remoteClient = getRemoteClient(req);
        return ServiceResponse.buildSuccessResp(fileTaskService.downloadFiles(remoteClient, req.getTaskId(),
            req.getFilePathList(), req.getFilePrefix()));
    }

    @Override
    public ServiceResponse<Integer> stopTasks(StopTasksReq req) {
        List<String> taskIdList = req.getTaskIdList();
        Integer count;
        count = fileTaskService.stopTasksAtOnce(taskIdList,
            new ThreadCommandBus.Command(TaskCommandEnum.valueOf(req.getTaskCommand()), null));
        return ServiceResponse.buildSuccessResp(count);
    }

    @Override
    public ServiceResponse<Integer> clearFiles(ClearTaskFilesReq req) {
        List<String> taskIdList = req.getTaskIdList();
        Integer count = 0;
        count = fileTaskService.clearTaskFilesAtOnce(taskIdList);
        return ServiceResponse.buildSuccessResp(count);
    }
}
