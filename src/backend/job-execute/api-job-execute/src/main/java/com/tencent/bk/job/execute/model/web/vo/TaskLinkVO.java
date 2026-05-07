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

package com.tencent.bk.job.execute.model.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "任务链接")
@Data
public class TaskLinkVO {
    /**
     * 资源范围类型
     */
    @Schema(description = "资源范围类型", allowableValues = "biz-业务,biz_set-业务集")
    private String scopeType;

    /**
     * 资源范围ID
     */
    @Schema(description = "资源范围ID")
    private String scopeId;

    /**
     * 业务ID
     */
    @Schema(description = "业务ID")
    private Long appId;

    /**
     * 任务ID
     */
    @Schema(description = "任务ID")
    private Long jobInstanceId;

    /**
     * 步骤ID
     */
    @Schema(description = "步骤ID")
    private Long stepInstanceId;

    /**
     * 执行次数
     */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /**
     * 批次
     */
    @Schema(description = "批次")
    private Integer batch;

    /**
     * gse任务ID
     */
    @Schema(description = "gse任务ID")
    private String gseTaskId;

    /**
     * web访问链接
     */
    @Schema(description = "web访问链接")
    private List<String> link;
}
