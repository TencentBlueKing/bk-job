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

package com.tencent.bk.job.file_gateway.model.resp.inner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 文件下载日志
 */
@ApiModel("单个文件的一条下载日志")
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FileLogPieceDTO {

    /**
     * 下载文件的file-worker的云区域ID:IP
     */
    @JsonProperty("srcIp")
    private String srcIp;

    /**
     * 下载文件的file-worker的云区域ID:IP - 显示
     */
    @JsonProperty("displaySrcIp")
    private String displaySrcIp;

    /**
     * 源文件路径 - 真实路径
     */
    @JsonProperty("srcFile")
    private String srcFile;

    /**
     * 源文件路径 - 用于显示
     */
    @JsonProperty("displaySrcFile")
    private String displaySrcFile;

    /**
     * 文件大小
     */
    @JsonProperty("size")
    private String size;

    /**
     * 文件任务状态
     */
    @JsonProperty("status")
    private Integer status;

    /**
     * 文件任务状态描述
     */
    @JsonProperty("statusDesc")
    private String statusDesc;

    /**
     * 速度
     */
    @JsonProperty("speed")
    private String speed;

    /**
     * 进度
     */
    @JsonProperty("process")
    private String process;

    /**
     * 日志内容
     */
    @JsonProperty("content")
    private String content;

    public FileLogPieceDTO(String srcIp,
                           String displaySrcIp,
                           String srcFile,
                           String displaySrcFile,
                           String size,
                           Integer status,
                           String statusDesc,
                           String speed,
                           String process,
                           String content) {
        this.srcIp = srcIp;
        this.displaySrcIp = displaySrcIp;
        this.srcFile = srcFile;
        this.displaySrcFile = displaySrcFile;
        this.size = size;
        this.status = status;
        this.statusDesc = statusDesc;
        this.speed = speed;
        this.process = process;
        this.content = content;
    }

    public String getDisplaySrcIp() {
        if (StringUtils.isNotEmpty(this.displaySrcIp)) {
            return this.displaySrcIp;
        } else {
            return this.srcIp;
        }
    }
}


