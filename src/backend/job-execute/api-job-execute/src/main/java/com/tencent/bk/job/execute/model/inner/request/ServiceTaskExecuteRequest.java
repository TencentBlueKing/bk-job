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

package com.tencent.bk.job.execute.model.inner.request;

import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 作业执行请求
 */
@Data
@ApiModel("作业执行请求报文")
public class ServiceTaskExecuteRequest {
    @ApiModelProperty(value = "业务ID", required = true)
    private Long appId;

    @ApiModelProperty(value = "操作者", required = true)
    private String operator;

    /**
     * 执行方案ID
     */
    @ApiModelProperty(value = "执行方案ID", required = true)
    private Long planId;

    @ApiModelProperty(value = "定时任务ID,如果是定时执行需要填", required = false)
    private Long cronTaskId;

    @ApiModelProperty(value = "任务名称，如果不为空就使用该值作为执行任务的名称", required = false)
    private String taskName;

    /**
     * 全局变量
     */
    @ApiModelProperty(value = "全局变量", required = false)
    private List<ServiceTaskVariable> taskVariables;
    /**
     * 启动方式
     * 1:页面执行，2：API调用，3：定时执行
     */
    private Integer startupMode;

    /**
     * 是否跳过鉴权；默认不跳过
     */
    @ApiModelProperty(value = "是否跳过鉴权，默认不跳过", required = false)
    private boolean skipAuth;
}
