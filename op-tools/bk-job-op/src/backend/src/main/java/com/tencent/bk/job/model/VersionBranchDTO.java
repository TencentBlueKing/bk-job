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

package com.tencent.bk.job.model;

import lombok.Data;

/**
 * 版本分支信息
 */
@Data
public class VersionBranchDTO {

    /**
     * 版本分支，如 3.10.x / master
     */
    private String versionBranch;

    /**
     * 版本分支描述
     */
    private String description;

    /**
     * 对应开发分支，如 bk-dev_3.10.x
     */
    private String devBranch;

    /**
     * 开发分支部署流水线远程触发命令
     */
    private String devBranchDeployPipelineCmd;

    /**
     * 上述命令的使用说明与示例
     */
    private String devBranchDeployPipelineCmdDesc;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createTime;

    /**
     * 最后修改时间（毫秒时间戳）
     */
    private Long lastModifyTime;
}
