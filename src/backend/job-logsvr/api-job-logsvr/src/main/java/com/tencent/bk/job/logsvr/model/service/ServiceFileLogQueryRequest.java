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

package com.tencent.bk.job.logsvr.model.service;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 文件任务执行日志查询请求
 */
@Data
@ApiModel("文件任务执行日志查询请求")
public class ServiceFileLogQueryRequest {

    @ApiModelProperty(value = "作业实例创建时间，格式为yyyy_MM_dd", required = true)
    private String jobCreateDate;

    @ApiModelProperty(value = "步骤实例ID", required = true)
    private Long stepInstanceId;

    @ApiModelProperty(value = "执行次数", required = true)
    private Integer executeCount;

    @ApiModelProperty(value = "滚动执行批次")
    private Integer batch;

    /**
     * 查询的主机ID列表
     */
    @ApiModelProperty("查询的主机ID列表")
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
    private List<Long> hostIds;

    @ApiModelProperty("查询的执行对象ID列表")
    private List<String> executeObjectIds;

    /**
     * @see com.tencent.bk.job.logsvr.consts.FileTaskModeEnum
     */
    @ApiModelProperty("分发模式,0:upload,1:download")
    private Integer mode;
}
