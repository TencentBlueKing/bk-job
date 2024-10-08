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

package com.tencent.bk.job.analysis.service.ai.context.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条下载文件错误信息，提交给AI让其进行归纳总结，字段名必须清晰
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DownloadFileErrorInfo {

    /**
     * 源执行对象类型
     */
    private String sourceExecuteObjectType;

    /**
     * 源主机描述信息
     */
    private HostDescription sourceHostDescription;

    /**
     * 源容器描述信息
     */
    private ContainerDescription sourceContainerDescription;

    /**
     * 目标执行对象类型
     */
    private String targetExecuteObjectType;

    /**
     * 目标主机描述信息
     */
    private HostDescription targetHostDescription;

    /**
     * 目标容器描述信息
     */
    private ContainerDescription targetContainerDescription;

    /**
     * 目标文件路径
     */
    private String targetFilePath;

    /**
     * 目标文件下载报错信息
     */
    private String errorLog;
}
