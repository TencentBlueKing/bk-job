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

package com.tencent.bk.job.execute.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.execute.model.esb.v4.req.ApiGwV4HostDTO;
import lombok.Data;

@Data
public class V4FileLogDTO {

    /**
     * 文件分发模式
     * @see com.tencent.bk.job.common.gse.constants.FileDistModeEnum
     */
    @JsonProperty("mode")
    private Integer mode;

    /**
     * 源IP
     */
    @JsonProperty("src_host")
    private ApiGwV4HostDTO srcHost;

    /**
     * 文件源路径
     */
    @JsonProperty("src_path")
    private String srcPath;

    /**
     * 目标IP
     */
    @JsonProperty("dest_host")
    private ApiGwV4HostDTO destHost;

    /**
     * 文件目标路径
     */
    @JsonProperty("dest_path")
    private String destPath;

    /**
     * 文件传输任务状态
     * @see com.tencent.bk.job.execute.common.constants.FileDistStatusEnum
     */
    @JsonProperty("status")
    private Integer status;

    /**
     * 文件传输日志内容
     */
    @JsonProperty("log_content")
    private String logContent;

    /**
     * 文件大小
     */
    @JsonProperty("size")
    private String size;

    /**
     * 上传/下载 速度
     */
    @JsonProperty("speed")
    private String speed;

    /**
     * 上传/下载 进度
     */
    @JsonProperty("process")
    private String process;

}
