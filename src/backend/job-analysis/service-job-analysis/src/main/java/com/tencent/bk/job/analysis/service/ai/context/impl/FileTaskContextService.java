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

package com.tencent.bk.job.analysis.service.ai.context.impl;

import com.tencent.bk.job.analysis.service.ai.context.model.FileTaskContext;
import com.tencent.bk.job.analysis.service.ai.context.model.FileTaskErrorSourceResult;
import com.tencent.bk.job.analysis.service.ai.context.model.TaskContext;
import com.tencent.bk.job.analysis.service.ai.context.model.TaskContextQuery;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.model.inner.ServiceStepInstanceDTO;
import com.tencent.bk.job.logsvr.api.ServiceLogResource;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.util.LogFieldUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件任务上下文服务
 */
@Service
public class FileTaskContextService {

    private final ServiceLogResource logResource;
    private final FileTaskFailLogAnalyzer fileTaskFailLogAnalyzer;

    @Autowired
    public FileTaskContextService(ServiceLogResource logResource,
                                  FileTaskFailLogAnalyzer fileTaskFailLogAnalyzer) {
        this.logResource = logResource;
        this.fileTaskFailLogAnalyzer = fileTaskFailLogAnalyzer;
    }

    /**
     * 根据步骤实例与上下文查询条件获取对应的任务上下文
     *
     * @param stepInstance 步骤实例
     * @param contextQuery 上下文查询条件
     * @return 任务上下文
     */
    public TaskContext getTaskContext(ServiceStepInstanceDTO stepInstance, TaskContextQuery contextQuery) {
        String jobCreateDate = LogFieldUtil.buildJobCreateDate(stepInstance.getCreateTime());
        // 上传日志
        ServiceFileLogQueryRequest request = new ServiceFileLogQueryRequest();
        request.setJobCreateDate(jobCreateDate);
        request.setStepInstanceId(contextQuery.getStepInstanceId());
        request.setExecuteCount(contextQuery.getExecuteCount());
        request.setMode(FileDistModeEnum.UPLOAD.getValue());
        request.setBatch(contextQuery.getBatch());
        InternalResponse<List<ServiceExecuteObjectLogDTO>> uploadLogResp = logResource.listFileExecuteObjectLogs(
            jobCreateDate,
            contextQuery.getStepInstanceId(),
            contextQuery.getExecuteCount(),
            request
        );
        if (!uploadLogResp.isSuccess()) {
            throw new ServiceException(
                uploadLogResp.getErrorMsg(),
                ErrorType.valOf(uploadLogResp.getErrorType()),
                uploadLogResp.getCode()
            );
        }
        List<ServiceExecuteObjectLogDTO> uploadExecuteObjectLogList = uploadLogResp.getData();
        // 下载日志
        InternalResponse<ServiceExecuteObjectLogDTO> downloadLogResp = logResource.getFileLogByExecuteObjectId(
            jobCreateDate,
            contextQuery.getStepInstanceId(),
            contextQuery.getExecuteCount(),
            contextQuery.getExecuteObjectId(),
            FileDistModeEnum.DOWNLOAD.getValue(),
            contextQuery.getBatch()
        );
        if (!downloadLogResp.isSuccess()) {
            throw new ServiceException(
                downloadLogResp.getErrorMsg(),
                ErrorType.valOf(downloadLogResp.getErrorType()),
                downloadLogResp.getCode()
            );
        }
        ServiceExecuteObjectLogDTO downloadExecuteObjectLog = downloadLogResp.getData();
        return buildContextForFileTask(stepInstance, uploadExecuteObjectLogList, downloadExecuteObjectLog);
    }

    /**
     * 构建文件任务上下文
     *
     * @param stepInstance               步骤实例
     * @param uploadExecuteObjectLogList 上传执行对象日志
     * @param downloadExecuteObjectLog   下载执行对象日志
     * @return 任务上下文
     */
    private TaskContext buildContextForFileTask(ServiceStepInstanceDTO stepInstance,
                                                List<ServiceExecuteObjectLogDTO> uploadExecuteObjectLogList,
                                                ServiceExecuteObjectLogDTO downloadExecuteObjectLog) {
        List<ServiceFileTaskLogDTO> fileTaskLogs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(uploadExecuteObjectLogList)) {
            uploadExecuteObjectLogList.forEach(uploadExecuteObjectLog -> {
                List<ServiceFileTaskLogDTO> uploadFileTaskLogs = uploadExecuteObjectLog.getFileTaskLogs();
                fileTaskLogs.addAll(uploadFileTaskLogs);
            });
        }
        List<ServiceFileTaskLogDTO> downloadFileTaskLogs = downloadExecuteObjectLog.getFileTaskLogs();
        if (CollectionUtils.isNotEmpty(downloadFileTaskLogs)) {
            fileTaskLogs.addAll(downloadFileTaskLogs);
        }
        List<ServiceFileTaskLogDTO> uploadFailLogs = new ArrayList<>();
        List<ServiceFileTaskLogDTO> downloadFailLogs = new ArrayList<>();
        for (ServiceFileTaskLogDTO fileTaskLog : fileTaskLogs) {
            if (isUploadFailLog(fileTaskLog)) {
                uploadFailLogs.add(fileTaskLog);
            } else if (isDownloadFailLog(fileTaskLog)) {
                downloadFailLogs.add(fileTaskLog);
            }
        }
        // 对上传失败和下载失败的日志进行分析，确定导致任务失败的主要原因
        FileTaskErrorSourceResult result = fileTaskFailLogAnalyzer.analyze(uploadFailLogs, downloadFailLogs);
        FileTaskContext fileTaskContext = new FileTaskContext(
            stepInstance.getName(),
            stepInstance.getFileStepInstance(),
            result
        );
        return new TaskContext(stepInstance.getExecuteType(), stepInstance.getStatus(), null, fileTaskContext);
    }

    /**
     * 判断是否为上传失败日志
     *
     * @param fileTaskLog 文件任务日志
     * @return 是否为上传失败日志
     */
    private boolean isUploadFailLog(ServiceFileTaskLogDTO fileTaskLog) {
        Integer mode = fileTaskLog.getMode();
        Integer status = fileTaskLog.getStatus();
        return FileTaskModeEnum.UPLOAD.getValue().equals(mode)
            && FileDistStatusEnum.FAILED.getValue().equals(status);
    }

    /**
     * 判断是否为下载失败日志
     *
     * @param fileTaskLog 文件任务日志
     * @return 是否为下载失败日志
     */
    private boolean isDownloadFailLog(ServiceFileTaskLogDTO fileTaskLog) {
        Integer mode = fileTaskLog.getMode();
        Integer status = fileTaskLog.getStatus();
        return FileTaskModeEnum.DOWNLOAD.getValue().equals(mode)
            && FileDistStatusEnum.FAILED.getValue().equals(status);
    }
}
