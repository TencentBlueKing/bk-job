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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 脚本日志查询请求
 */
@Data
@ApiModel("脚本日志查询请求")
public class ServiceScriptLogQueryRequest {
    /**
     * 步骤实例ID
     */
    @ApiModelProperty(value = "步骤实例ID", required = true)
    private Long stepInstanceId;

    /**
     * 执行次数
     */
    @ApiModelProperty(value = "执行次数", required = true)
    private Integer executeCount;

    /**
     * 滚动执行批次，可能为null
     */
    @ApiModelProperty(value = "滚动执行批次，可能为null")
    private Integer batch;

    @ApiModelProperty("主机IP列表;兼容参数,如果hostIds参数不为空，那么忽略ips参数")
    @CompatibleImplementation(name = "rolling_execute", explain = "兼容字段，后续用hostIds替换", version = "3.7.x")
    private List<String> ips;

    /**
     * 查询的主机ID列表
     */
    @ApiModelProperty("查询的主机ID列表")
    private List<Long> hostIds;
}
