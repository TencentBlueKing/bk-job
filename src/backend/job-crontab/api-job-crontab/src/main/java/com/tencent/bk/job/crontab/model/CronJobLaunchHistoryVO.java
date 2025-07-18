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

package com.tencent.bk.job.crontab.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 定时任务执行历史
 *
 * @since 30/10/2020 14:50
 */
@Data
@ApiModel("定时任务执行历史")
public class CronJobLaunchHistoryVO {

    /**
     * 任务调度时间
     */
    @ApiModelProperty("调度时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long scheduledTime;

    /**
     * 任务执行时间
     */
    @ApiModelProperty("执行时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long executeTime;

    /**
     * 任务状态
     *
     */
    @ApiModelProperty("启动状态")
    private Integer status;

    /**
     * 执行人
     */
    @ApiModelProperty("执行人")
    private String executor;

    /**
     * 错误码
     */
    @ApiModelProperty("错误码")
    private Long errorCode;

    /**
     * 错误信息
     */
    @ApiModelProperty("错误信息")
    private String errorMsg;
}
