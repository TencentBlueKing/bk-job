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

package com.tencent.bk.job.execute.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class FileStepInstanceDTO {
    private Long stepInstanceId;
    /**
     * 文件传输的源文件
     */
    private List<FileSourceDTO> fileSourceList;
    /**
     * 变量解析之后的文件传输的源文件
     */
    private List<FileSourceDTO> resolvedFileSourceList;
    /**
     * 文件传输的目标目录
     */
    private String fileTargetPath;
    /**
     * 变量解析之后的目标路径
     */
    private String resolvedFileTargetPath;
    /**
     * 任务超时时间
     */
    private Integer timeout;
    /**
     * 系统执行账号ID
     */
    private Long accountId;
    /**
     * 目标机器的执行账户名
     */
    private String account;
    /**
     * 上传文件限速，单位KB
     */
    private Integer fileUploadSpeedLimit;

    /**
     * 下载文件限速，单位KB
     */
    private Integer fileDownloadSpeedLimit;
    /**
     * 文件分发-重名文件处理，1-覆盖，2-追加源IP目录
     */
    private Integer fileDuplicateHandle;
    /**
     * 目标路径不存在：路径处理
     */
    private Integer notExistPathHandler;
}
