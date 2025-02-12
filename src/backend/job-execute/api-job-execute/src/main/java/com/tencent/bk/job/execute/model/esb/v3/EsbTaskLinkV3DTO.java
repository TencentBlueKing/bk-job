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

package com.tencent.bk.job.execute.model.esb.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class EsbTaskLinkV3DTO {
    /**
     * 资源范围类型
     */
    @JsonProperty("bk_scope_type")
    private String scopeType;

    /**
     * 资源范围ID
     */
    @JsonProperty("bk_scope_id")
    private String scopeId;

    /**
     * 业务ID
     */
    @JsonProperty("bk_app_id")
    private Long appId;

    /**
     * 任务ID
     */
    @JsonProperty("job_instance_id")
    private Long jobInstanceId;

    /**
     * 步骤ID
     */
    @JsonProperty("step_instance_id")
    private Long stepInstanceId;

    /**
     * 执行次数
     */
    @JsonProperty("retry_count")
    private Integer retryCount;

    /**
     * 批次
     */
    @JsonProperty("batch")
    private Integer batch;

    /**
     * gse任务ID
     */
    @JsonProperty("gse_task_id")
    private String gseTaskId;

    /**
     * web访问链接
     */
    @JsonProperty("link")
    private List<String> link;
}
