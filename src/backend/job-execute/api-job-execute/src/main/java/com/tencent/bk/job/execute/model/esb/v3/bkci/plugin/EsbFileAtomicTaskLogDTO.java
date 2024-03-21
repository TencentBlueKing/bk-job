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

package com.tencent.bk.job.execute.model.esb.v3.bkci.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiExecuteObjectDTO;
import lombok.Data;

/**
 * 文件分发原子任务，指一个文件从一个源执行对象分发到一个目标执行对象的任务
 */
@Data
public class EsbFileAtomicTaskLogDTO {

    /**
     * 分发模式
     */
    @JsonProperty("mode")
    private Integer mode;

    /**
     * 源文件所在执行对象
     */
    @JsonProperty("src_execute_object")
    private OpenApiExecuteObjectDTO srcExecuteObject;

    /**
     * 源文件路径
     */
    @JsonProperty("src_path")
    private String srcPath;

    /**
     * 文件分发目标执行对象
     */
    @JsonProperty("dest_execute_object")
    private OpenApiExecuteObjectDTO destExecuteObject;

    /**
     * 目标路径
     */
    @JsonProperty("dest_path")
    private String destPath;

    /**
     * 任务状态
     */
    @JsonProperty("status")
    private Integer status;

    /**
     * 分发日志
     */
    @JsonProperty("log_content")
    private String logContent;

    /**
     * 文件大小
     */
    @JsonProperty("size")
    private String size;

    /**
     * 分发速度
     */
    @JsonProperty("speed")
    private String speed;

    /**
     * 分发进度
     */
    @JsonProperty("process")
    private String process;

}


