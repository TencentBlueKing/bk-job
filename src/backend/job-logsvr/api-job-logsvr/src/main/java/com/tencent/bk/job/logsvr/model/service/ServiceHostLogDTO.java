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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@ApiModel("主机执行日志")
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ServiceHostLogDTO {
    /**
     * 作业步骤实例ID
     */
    @ApiModelProperty("步骤实例ID")
    private Long stepInstanceId;

    /**
     * 执行次数
     */
    @ApiModelProperty("执行次数")
    private Integer executeCount;

    /**
     * 滚动执行批次
     */
    @ApiModelProperty("滚动执行批次")
    private Integer batch;

    /**
     * ip
     */
    @CompatibleImplementation(name = "rolling_execute", explain = "兼容字段，后续用hostId替换", version = "3.7.x")
    @ApiModelProperty(value = "ip, 兼容参数, 如果存在hostId那么忽略ip参数")
    private String ip;

    /**
     * 主机ID
     */
    @ApiModelProperty(value = "主机ID")
    private Long hostId;

    /**
     * 脚本日志内容
     */
    @ApiModelProperty(value = "脚本日志内容，保存日志的时候需要传入,查询日志的时候该字段无效")
    @JsonProperty("scriptLog")
    private ServiceScriptLogDTO scriptLog;

    /**
     * 文件任务执行日志
     */
    private List<ServiceFileTaskLogDTO> fileTaskLogs;

    public void addFileTaskLog(ServiceFileTaskLogDTO fileTaskDetailLog) {
        if (fileTaskLogs == null) {
            fileTaskLogs = new ArrayList<>();
        }
        fileTaskLogs.add(fileTaskDetailLog);
    }
}
