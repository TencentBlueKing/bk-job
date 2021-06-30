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
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.StringJoiner;

@ApiModel("执行日志保存请求")
@Getter
@Setter
public class SaveLogRequest {
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
     * ip
     */
    @ApiModelProperty(value = "ip", required = true)
    private String ip;
    /**
     * 执行次数
     */
    @ApiModelProperty(value = "执行次数", required = true)
    private Integer executeCount;

    /**
     * 脚本日志内容
     */
    @ApiModelProperty(value = "脚本日志内容,新协议")
    @JsonProperty("scriptLog")
    private ServiceScriptLogDTO scriptLog;

    /**
     * 文件日志
     */
    @ApiModelProperty
    private List<ServiceFileTaskLogDTO> fileTaskLogs;

    private Integer logType;

    @Override
    public String toString() {
        return new StringJoiner(", ", SaveLogRequest.class.getSimpleName() + "[", "]")
            .add("jobCreateDate='" + jobCreateDate + "'")
            .add("stepInstanceId=" + stepInstanceId)
            .add("ip='" + ip + "'")
            .add("executeCount=" + executeCount)
            .add("logType=" + logType)
            .toString();
    }
}
