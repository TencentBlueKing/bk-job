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

import com.tencent.bk.job.analysis.service.ai.context.constants.FileTaskErrorSourceEnum;
import com.tencent.bk.job.analysis.service.ai.context.model.FileTaskErrorSourceResult;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件分发任务失败日志分析器
 */
@Service
public class FileTaskFailLogAnalyzer {

    /**
     * 根据上传与下载失败的日志信息分析导致出错的源头
     *
     * @param uploadFailLogs   上传失败日志
     * @param downloadFailLogs 下载失败日志
     * @return 分析结果
     */
    public FileTaskErrorSourceResult analyze(List<ServiceFileTaskLogDTO> uploadFailLogs,
                                             List<ServiceFileTaskLogDTO> downloadFailLogs) {
        if (uploadFailLogs.isEmpty() && downloadFailLogs.isEmpty()) {
            return new FileTaskErrorSourceResult(FileTaskErrorSourceEnum.NO_ERROR, uploadFailLogs, downloadFailLogs);
        }
        // 上传失败日志为空：说明是下载出错
        if (uploadFailLogs.isEmpty()) {
            return new FileTaskErrorSourceResult(FileTaskErrorSourceEnum.DOWNLOAD_ERROR, uploadFailLogs,
                downloadFailLogs);
        }
        // 下载失败日志为空：说明是源文件上传出错
        if (downloadFailLogs.isEmpty()) {
            return new FileTaskErrorSourceResult(FileTaskErrorSourceEnum.SOURCE_FILE_UPLOAD_ERROR, uploadFailLogs,
                downloadFailLogs);
        }
        // 上传失败日志与下载失败日志均不为空
        Set<String> downloadFailSrcFilePathSet = downloadFailLogs.stream().map(fileTaskLog ->
            fileTaskLog.getSrcExecuteObjectId() + ":" + fileTaskLog.getSrcFile()
        ).collect(Collectors.toSet());
        Set<String> uploadFailFilePathSet = uploadFailLogs.stream().map(fileTaskLog ->
            fileTaskLog.getSrcExecuteObjectId() + ":" + fileTaskLog.getSrcFile()
        ).collect(Collectors.toSet());
        downloadFailSrcFilePathSet.removeAll(uploadFailFilePathSet);
        if (downloadFailSrcFilePathSet.isEmpty()) {
            // 下载失败日志关联的上传文件任务均失败：说明根本原因是源文件上传出错
            return new FileTaskErrorSourceResult(
                FileTaskErrorSourceEnum.SOURCE_FILE_UPLOAD_ERROR,
                uploadFailLogs,
                Collections.emptyList()
            );
        } else {
            // 上传失败日志与下载失败日志均不为空，且下载失败日志并不全是上传失败导致：说明根本原因是上传下载同时出错
            // 筛选出由于下载方出错导致的下载失败日志
            List<ServiceFileTaskLogDTO> realDownloadFailLogs = downloadFailLogs.stream().filter(fileTaskLog ->
                downloadFailSrcFilePathSet.contains(fileTaskLog.getSrcExecuteObjectId() + ":" + fileTaskLog.getSrcFile())
            ).collect(Collectors.toList());
            return new FileTaskErrorSourceResult(FileTaskErrorSourceEnum.UPLOAD_AND_DOWNLOAD_ERROR, uploadFailLogs,
                realDownloadFailLogs);
        }
    }
}
