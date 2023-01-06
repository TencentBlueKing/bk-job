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

package com.tencent.bk.job.logsvr.model.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel("执行日志保存请求")
@Data
public class ServiceSaveLogRequest {
    /**
     * 作业实例创建时间
     */
    @ApiModelProperty(value = "作业实例创建时间，格式为yyyy_MM_dd", required = true)
    private String jobCreateDate;

    /**
     * 作业步骤实例ID
     */
    @ApiModelProperty(value = "步骤实例ID", required = true)
    private Long stepInstanceId;

    /**
     * 主机云区域ID:ipv4
     */
    @ApiModelProperty(value = "主机云区域ID:ipv4")
    private String ip;

    /**
     * 主机云区域ID:ipv6
     */
    @ApiModelProperty(value = "主机云区域ID:ipv6")
    private String ipv6;

    /**
     * 主机ID
     */
    @ApiModelProperty(value = "主机ID")
    private Long hostId;

    /**
     * 执行次数
     */
    @ApiModelProperty(value = "执行次数", required = true)
    private Integer executeCount;

    /**
     * 滚动执行批次
     */
    @ApiModelProperty(value = "滚动批次")
    private Integer batch;

    /**
     * 脚本日志内容
     */
    @ApiModelProperty(value = "脚本日志内容")
    @JsonProperty("scriptLog")
    private ServiceScriptLogDTO scriptLog;

    /**
     * 文件日志
     */
    @ApiModelProperty(value = "文件任务日志")
    private List<ServiceFileTaskLogDTO> fileTaskLogs;

    /**
     * 日志类型
     *
     * @see com.tencent.bk.job.logsvr.consts.LogTypeEnum
     */
    @ApiModelProperty(value = "日志类型,1-script;2-file")
    private Integer logType;
}
