/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.file_gateway.model.dto;

import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.model.req.inner.UpdateFileSourceTaskReq;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 单个文件任务进度信息
 */
@NoArgsConstructor
@Getter
@Setter
public class FileTaskProgressDTO {
    /**
     * ID
     */
    private String fileSourceTaskId;
    /**
     * 文件路径（含bucketName）
     */
    private String filePath;
    /**
     * 文件下载到机器上的真实路径
     */
    private String downloadPath;
    /**
     * 任务文件状态
     */
    private TaskStatusEnum status;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 速度
     */
    private String speed;
    /**
     * 进度
     */
    private Integer progress;
    /**
     * 日志内容
     */
    private String content;

    public static FileTaskProgressDTO fromUpdateFileSourceTaskReq(UpdateFileSourceTaskReq req) {
        if (req == null) {
            return null;
        }
        FileTaskProgressDTO fileTaskProgressDTO = new FileTaskProgressDTO();
        fileTaskProgressDTO.setFileSourceTaskId(req.getFileSourceTaskId());
        fileTaskProgressDTO.setFilePath(req.getFilePath());
        fileTaskProgressDTO.setDownloadPath(req.getDownloadPath());
        fileTaskProgressDTO.setStatus(req.getStatus());
        fileTaskProgressDTO.setFileSize(req.getFileSize());
        fileTaskProgressDTO.setSpeed(req.getSpeed());
        fileTaskProgressDTO.setProgress(req.getProgress());
        fileTaskProgressDTO.setContent(req.getContent());
        return fileTaskProgressDTO;
    }
}
