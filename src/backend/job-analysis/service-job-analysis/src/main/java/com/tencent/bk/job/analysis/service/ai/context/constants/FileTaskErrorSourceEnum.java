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

package com.tencent.bk.job.analysis.service.ai.context.constants;

import lombok.Getter;

/**
 * 文件任务失败原因来源枚举
 */
@Getter
public enum FileTaskErrorSourceEnum {
    NO_ERROR(
        "job.analysis.ai.fileTaskErrorSource.noError",
        "任务未失败"
    ),
    SOURCE_FILE_UPLOAD_ERROR(
        "job.analysis.ai.fileTaskErrorSource.sourceFileUploadError",
        "源文件上传出错导致的任务失败"
    ),
    DOWNLOAD_ERROR(
        "job.analysis.ai.fileTaskErrorSource.downloadError",
        "目标执行对象下载文件出错导致的任务失败"
    ),
    UPLOAD_AND_DOWNLOAD_ERROR(
        "job.analysis.ai.fileTaskErrorSource.uploadAndDownloadError",
        "源文件上传与目标执行对象下载文件均出错导致的任务失败"
    );

    /**
     * 任务失败原因描述国际化key
     */
    private final String i18nKey;

    /**
     * 任务失败原因描述
     */
    private final String description;

    FileTaskErrorSourceEnum(String i18nKey, String description) {
        this.i18nKey = i18nKey;
        this.description = description;
    }
}
