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

package com.tencent.bk.job.file_gateway.service;

import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.FileSourceTaskStatusDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;

import java.util.List;

public interface FileSourceTaskService {
    TaskInfoDTO startFileSourceDownloadTask(String username, Long appId, Long stepInstanceId, Integer executeCount,
                                            String batchTaskId, Integer fileSourceId, List<String> filePathList);

    TaskInfoDTO startFileSourceDownloadTaskWithId(String username, Long appId, Long stepInstanceId,
                                                  Integer executeCount, String batchTaskId, Integer fileSourceId,
                                                  List<String> filePathList, String fileSourceTaskId);

    String updateFileSourceTask(String taskId, String filePath, String downloadPath, Long fileSize, String speed,
                                Integer progress, String content, TaskStatusEnum status);

    FileSourceTaskStatusDTO getFileSourceTaskStatusAndLogs(String taskId, Long logStart, Long logLength);

    Integer stopTasks(List<String> taskIdList);

    Integer recallTasks(List<String> taskIdList);

    Integer clearTaskFiles(List<String> taskIdList);

    FileSourceTaskDTO getFileSourceTaskById(String id);

    Integer deleteFileSourceTaskById(String id);
}
